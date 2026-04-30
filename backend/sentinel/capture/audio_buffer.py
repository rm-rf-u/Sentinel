"""Circular audio ring buffer — single producer (mic callback), single consumer (inference).

Stores a rolling window of PCM samples. The producer overwrites the oldest
samples when the buffer is full (lossy). The consumer reads the most recent
N samples without removing them.
"""

from __future__ import annotations

import threading

import numpy as np


class AudioRingBuffer:
    def __init__(self, max_seconds: float = 5.0, sample_rate: int = 16_000) -> None:
        self._sr = sample_rate
        self._cap = int(max_seconds * sample_rate)
        self._buf = np.zeros(self._cap, dtype=np.float32)
        self._write_pos = 0
        self._total_written = 0
        self._lock = threading.Lock()

    @property
    def sample_rate(self) -> int:
        return self._sr

    def put(self, chunk: np.ndarray) -> None:
        """Push a chunk of float32 mono samples."""
        chunk = chunk.astype(np.float32).ravel()
        n = len(chunk)
        with self._lock:
            end = self._write_pos + n
            if end <= self._cap:
                self._buf[self._write_pos : end] = chunk
            else:
                first = self._cap - self._write_pos
                self._buf[self._write_pos :] = chunk[:first]
                self._buf[: n - first] = chunk[first:]
            self._write_pos = end % self._cap
            self._total_written += n

    def get_latest(self, n_samples: int) -> np.ndarray | None:
        """Return the most recent n_samples as a contiguous float32 array, or None if not enough data."""
        with self._lock:
            if self._total_written < n_samples or n_samples > self._cap:
                return None
            out = np.empty(n_samples, dtype=np.float32)
            end = self._write_pos
            start = (end - n_samples) % self._cap
            if start < end:
                out[:] = self._buf[start:end]
            else:
                first = self._cap - start
                out[:first] = self._buf[start:]
                out[first:] = self._buf[:end]
            return out

    @property
    def seconds_buffered(self) -> float:
        return min(self._total_written, self._cap) / self._sr
