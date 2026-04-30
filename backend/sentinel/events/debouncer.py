"""Generic sustain-then-fire debouncer.

Fires exactly once per continuous violation period — the condition must drop
to False before the next event can be armed.
"""

from __future__ import annotations


class Debouncer:
    def __init__(self, threshold_seconds: float) -> None:
        self.threshold = threshold_seconds
        self._start: float | None = None
        self._fired: bool = False

    def update(self, condition: bool, now: float) -> bool:
        """Call on every tick. Returns True exactly once per sustained trigger."""
        if condition:
            if self._start is None:
                self._start = now
                self._fired = False
            elif not self._fired and (now - self._start) >= self.threshold:
                self._fired = True
                return True
        else:
            self._start = None
            self._fired = False
        return False

    def elapsed(self, now: float) -> float:
        """Seconds the current condition has been active (0 if not active)."""
        if self._start is None:
            return 0.0
        return now - self._start

    def reset(self) -> None:
        self._start = None
        self._fired = False
