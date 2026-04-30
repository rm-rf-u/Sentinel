"""Microphone capture producer — sounddevice callback → AudioRingBuffer."""

from __future__ import annotations

import threading

import numpy as np
import sounddevice as sd
import structlog

from sentinel.capture.audio_buffer import AudioRingBuffer

logger = structlog.get_logger()

SAMPLE_RATE = 16_000
BLOCK_SIZE = 512  # ~32ms per callback


class MicCapture:
    def __init__(self, device_index: int = -1) -> None:
        self._device = device_index if device_index >= 0 else None
        self.buffer = AudioRingBuffer(max_seconds=6.0, sample_rate=SAMPLE_RATE)
        self._stream: sd.InputStream | None = None
        self._lock = threading.Lock()

    def start(self) -> None:
        with self._lock:
            self._stream = sd.InputStream(
                device=self._device,
                samplerate=SAMPLE_RATE,
                channels=1,
                dtype="float32",
                blocksize=BLOCK_SIZE,
                callback=self._callback,
            )
            self._stream.start()
        logger.info("mic.start", sr=SAMPLE_RATE, device=self._device)

    def stop(self) -> None:
        with self._lock:
            if self._stream:
                self._stream.stop()
                self._stream.close()
                self._stream = None
        logger.info("mic.stop")

    def _callback(
        self,
        indata: np.ndarray,
        frames: int,
        time_info: object,
        status: sd.CallbackFlags,
    ) -> None:
        if status:
            logger.warning("mic.callback_status", status=str(status))
        self.buffer.put(indata[:, 0])  # mono: take first channel
