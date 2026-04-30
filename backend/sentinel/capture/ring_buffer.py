"""Thread-safe latest-frame holder.

Single producer (camera thread), multiple consumers (WebRTC track, inference).
Lossy by design: if the consumer is slower than the producer, old frames are
silently overwritten rather than blocking capture.
"""

from __future__ import annotations

import threading

import numpy as np


class FrameBuffer:
    def __init__(self) -> None:
        self._frame: np.ndarray | None = None
        self._lock = threading.Lock()
        self._ready = threading.Event()

    def put(self, frame: np.ndarray) -> None:
        with self._lock:
            self._frame = frame
        self._ready.set()

    def get_latest(self) -> np.ndarray | None:
        with self._lock:
            return self._frame

    def wait(self, timeout: float = 1.0) -> np.ndarray | None:
        """Block until a new frame is available (or timeout). Returns None on timeout."""
        self._ready.wait(timeout)
        self._ready.clear()
        return self.get_latest()

    @property
    def has_frame(self) -> bool:
        return self._frame is not None
