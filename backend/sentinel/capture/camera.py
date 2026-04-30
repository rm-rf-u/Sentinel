"""Camera capture producer — runs in a dedicated daemon thread."""

from __future__ import annotations

import threading
import time

import cv2
import numpy as np
import structlog

from sentinel.capture.ring_buffer import FrameBuffer

logger = structlog.get_logger()

WIDTH = 1280
HEIGHT = 720
FPS = 30


class CameraCapture:
    def __init__(self, device_index: int = 0) -> None:
        self._device_index = device_index
        self.buffer = FrameBuffer()
        self._thread: threading.Thread | None = None
        self._stop_event = threading.Event()

    def start(self) -> None:
        self._stop_event.clear()
        self._thread = threading.Thread(target=self._run, daemon=True, name="camera")
        self._thread.start()
        logger.info("camera.start", device=self._device_index)

    def stop(self) -> None:
        self._stop_event.set()
        if self._thread:
            self._thread.join(timeout=3.0)
        logger.info("camera.stop")

    def _run(self) -> None:
        cap: cv2.VideoCapture | None = None
        while not self._stop_event.is_set():
            cap = self._open_capture()
            if cap is None:
                time.sleep(2.0)
                continue
            self._capture_loop(cap)
            cap.release()

    def _open_capture(self) -> cv2.VideoCapture | None:
        cap = cv2.VideoCapture(self._device_index, cv2.CAP_AVFOUNDATION)
        if not cap.isOpened():
            logger.warning("camera.open.failed", device=self._device_index)
            return None
        cap.set(cv2.CAP_PROP_FRAME_WIDTH, WIDTH)
        cap.set(cv2.CAP_PROP_FRAME_HEIGHT, HEIGHT)
        cap.set(cv2.CAP_PROP_FPS, FPS)
        actual_w = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
        actual_h = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
        actual_fps = cap.get(cv2.CAP_PROP_FPS)
        logger.info("camera.opened", w=actual_w, h=actual_h, fps=actual_fps)
        return cap

    def _capture_loop(self, cap: cv2.VideoCapture) -> None:
        consecutive_failures = 0
        while not self._stop_event.is_set():
            ok, frame = cap.read()
            if not ok:
                consecutive_failures += 1
                if consecutive_failures >= 10:
                    logger.warning("camera.read.failed", consecutive=consecutive_failures)
                    break
                time.sleep(0.01)
                continue
            consecutive_failures = 0
            self.buffer.put(frame)
