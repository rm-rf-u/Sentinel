"""YOLOv8-pose inference runner — background thread, ~10 fps."""

from __future__ import annotations

import asyncio
import threading
import time
from pathlib import Path

import numpy as np
import structlog

from sentinel.capture.ring_buffer import FrameBuffer
from sentinel.events.models import VisionDetection
from sentinel.inference.pose_rules import check_prone, get_centroid_norm

logger = structlog.get_logger()

TARGET_FPS = 10
_INTERVAL = 1.0 / TARGET_FPS


class VisionRunner:
    def __init__(self, model_dir: Path, buffer: FrameBuffer) -> None:
        self._model_dir = model_dir
        self._buffer = buffer
        self._thread: threading.Thread | None = None
        self._stop = threading.Event()
        self._loop: asyncio.AbstractEventLoop | None = None
        self._queue: asyncio.Queue[VisionDetection] | None = None
        self._frame_count = 0

    def start(self, loop: asyncio.AbstractEventLoop, queue: asyncio.Queue[VisionDetection]) -> None:
        self._loop = loop
        self._queue = queue
        self._stop.clear()
        self._thread = threading.Thread(target=self._run, daemon=True, name="vision")
        self._thread.start()

    def stop(self) -> None:
        self._stop.set()
        if self._thread:
            self._thread.join(timeout=5.0)

    # ── Internal ─────────────────────────────────────────────────────────────

    def _run(self) -> None:
        try:
            model = self._load_model()
        except Exception:
            logger.exception("vision.model.load_failed")
            return

        logger.info("vision.ready", fps=TARGET_FPS)
        last_tick = 0.0
        fps_log_at = time.monotonic() + 30.0

        while not self._stop.is_set():
            now = time.monotonic()
            wait = _INTERVAL - (now - last_tick)
            if wait > 0:
                time.sleep(wait)
                continue

            frame = self._buffer.get_latest()
            if frame is None:
                time.sleep(0.02)
                continue

            last_tick = time.monotonic()
            self._frame_count += 1

            try:
                detection = self._infer(model, frame)
                self._push(detection)
            except Exception:
                logger.exception("vision.infer_error", frame=self._frame_count)

            if time.monotonic() >= fps_log_at:
                logger.debug("vision.fps_ok", frame=self._frame_count)
                fps_log_at = time.monotonic() + 30.0

    def _load_model(self):  # type: ignore[return]
        from ultralytics import YOLO  # imported here so the main process doesn't pay the import cost

        coreml = self._model_dir / "yolov8n-pose.mlpackage"
        pt = self._model_dir / "yolov8n-pose.pt"

        if coreml.exists():
            logger.info("vision.model.coreml", path=str(coreml))
            return YOLO(str(coreml))

        logger.warning("vision.model.pytorch", hint="run scripts/export_coreml.py for CoreML/ANE")
        if not pt.exists():
            logger.info("vision.model.download")
        return YOLO("yolov8n-pose.pt")  # auto-downloads on first run

    def _infer(self, model, frame: np.ndarray) -> VisionDetection:  # type: ignore[return]
        results = model.predict(frame, verbose=False, conf=0.35, imgsz=640)
        r = results[0]

        if r.boxes is None or len(r.boxes) == 0:
            return VisionDetection(frame_number=self._frame_count)

        boxes_xyxy = r.boxes.xyxy.cpu().numpy()
        confs = r.boxes.conf.cpu().numpy()
        best_idx = int(np.argmax(confs))
        best_conf = float(confs[best_idx])

        h, w = frame.shape[:2]
        centroid = get_centroid_norm(boxes_xyxy[best_idx : best_idx + 1], h, w)

        prone = False
        if r.keypoints is not None and len(r.keypoints) > best_idx:
            kp = r.keypoints[best_idx]
            kp_xy = kp.xy.cpu().numpy()[0]   # shape (17, 2)
            kp_conf = kp.conf.cpu().numpy()[0] if kp.conf is not None else np.zeros(17)
            prone = check_prone(kp_xy, kp_conf)

        return VisionDetection(
            person_detected=True,
            centroid_norm=centroid,
            is_prone_candidate=prone,
            confidence=best_conf,
            frame_number=self._frame_count,
        )

    def _push(self, detection: VisionDetection) -> None:
        if self._loop is None or self._queue is None:
            return
        try:
            self._loop.call_soon_threadsafe(self._queue.put_nowait, detection)
        except asyncio.QueueFull:
            pass  # drop — inference faster than event processing
