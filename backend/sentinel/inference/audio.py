"""Audio inference runner — PANNs CNN14 cry detection in a background thread.

Sliding window: 3-second analysis window, 1-second hop (~1 inference/sec).
The model needs 32 kHz input; we capture at 16 kHz and resample on the fly.
Baby cry = AudioSet class 23 ("Baby cry, infant cry").
"""

from __future__ import annotations

import asyncio
import threading
import time
from collections import deque
from dataclasses import dataclass
from pathlib import Path

import numpy as np
import structlog

from sentinel.capture.audio_buffer import AudioRingBuffer

logger = structlog.get_logger()

CAPTURE_SR = 16_000
MODEL_SR = 32_000
WINDOW_SECONDS = 3.0
HOP_SECONDS = 1.0

BABY_CRY_CLASS = 23  # AudioSet index for "Baby cry, infant cry"

WINDOW_SAMPLES_CAPTURE = int(WINDOW_SECONDS * CAPTURE_SR)
HOP_SAMPLES_CAPTURE = int(HOP_SECONDS * CAPTURE_SR)


@dataclass
class AudioDetection:
    cry_score: float       # 0..1, instantaneous PANNs score for baby cry
    window_mean: float     # rolling mean over recent windows
    frame_number: int


class AudioRunner:
    def __init__(self, buffer: AudioRingBuffer) -> None:
        self._buffer = buffer
        self._thread: threading.Thread | None = None
        self._stop = threading.Event()
        self._loop: asyncio.AbstractEventLoop | None = None
        self._queue: asyncio.Queue[AudioDetection] | None = None
        self._frame_count = 0
        # Rolling score history (last ~4 s at 1 s/hop = 4 windows)
        self._score_history: deque[float] = deque(maxlen=4)

    def start(self, loop: asyncio.AbstractEventLoop, queue: asyncio.Queue[AudioDetection]) -> None:
        self._loop = loop
        self._queue = queue
        self._stop.clear()
        self._thread = threading.Thread(target=self._run, daemon=True, name="audio")
        self._thread.start()

    def stop(self) -> None:
        self._stop.set()
        if self._thread:
            self._thread.join(timeout=5.0)

    # ── Internal ─────────────────────────────────────────────────────────────

    def _run(self) -> None:
        try:
            model, device = self._load_model()
        except Exception:
            logger.exception("audio.model.load_failed")
            return

        logger.info("audio.ready", hop_s=HOP_SECONDS, window_s=WINDOW_SECONDS)
        last_tick = 0.0

        while not self._stop.is_set():
            now = time.monotonic()
            wait = HOP_SECONDS - (now - last_tick)
            if wait > 0:
                time.sleep(wait)
                continue

            audio = self._buffer.get_latest(WINDOW_SAMPLES_CAPTURE)
            if audio is None:
                time.sleep(0.1)
                continue

            last_tick = time.monotonic()
            self._frame_count += 1

            try:
                score = self._infer(model, device, audio)
                self._score_history.append(score)
                mean = float(np.mean(self._score_history))
                detection = AudioDetection(
                    cry_score=score,
                    window_mean=mean,
                    frame_number=self._frame_count,
                )
                self._push(detection)
            except Exception:
                logger.exception("audio.infer_error", frame=self._frame_count)

    def _load_model(self):  # type: ignore[return]
        import torch
        from panns_inference import AudioTagging

        device = "mps" if torch.backends.mps.is_available() else "cpu"
        # PANNs will download CNN14 checkpoint (~80 MB) on first run
        model = AudioTagging(checkpoint_path=None, device=device)
        return model, device

    def _infer(self, model, device: str, audio_16k: np.ndarray) -> float:
        import torch
        from torchaudio.functional import resample

        # Peak-normalise to bring low-gain Mac mics into the [-1,1] range PANNs expects.
        peak = float(np.abs(audio_16k).max())
        if peak > 1e-4:
            audio_16k = audio_16k * (0.9 / peak)

        # Resample 16 kHz → 32 kHz (PANNs requirement)
        t = torch.from_numpy(audio_16k).unsqueeze(0)  # (1, samples)
        t = resample(t, CAPTURE_SR, MODEL_SR)
        audio_32k = t.numpy()

        # PANNs expects (batch, samples) normalised to [-1, 1]
        clipwise_output, _ = model.inference(audio_32k)
        score = float(clipwise_output[0, BABY_CRY_CLASS])
        return score

    def _push(self, detection: AudioDetection) -> None:
        if self._loop is None or self._queue is None:
            return
        try:
            self._loop.call_soon_threadsafe(self._queue.put_nowait, detection)
        except asyncio.QueueFull:
            pass
