"""Evaluates vision and audio detections against rules and fires events."""

from __future__ import annotations

import time
from datetime import datetime, timezone as tz

import structlog

from sentinel.events.debouncer import Debouncer
from sentinel.events.dispatcher import Dispatcher
from sentinel.events.models import AudioDetection, VisionDetection
from sentinel.inference.pose_rules import check_zone_violation
from sentinel.types import (
    Event,
    EventDetails,
    EventType,
    SafeZone,
    Severity,
    Settings,
)

logger = structlog.get_logger()


class EventEngine:
    """Consumes VisionDetection and AudioDetection; evaluates rules; dispatches events."""

    def __init__(self, settings: Settings, dispatcher: Dispatcher) -> None:
        self._dispatcher = dispatcher
        self._safe_zone: SafeZone | None = None
        self._settings = settings
        self._zone_debouncer = Debouncer(settings.sensitivity.zone_violation_seconds)
        self._prone_debouncer = Debouncer(settings.sensitivity.prone_position_seconds)
        self._cry_debouncer = Debouncer(settings.sensitivity.cry_window_seconds)

    # ── Configuration hot-reload ──────────────────────────────────────────────

    def update_safe_zone(self, zone: SafeZone | None) -> None:
        self._safe_zone = zone
        self._zone_debouncer.reset()

    def update_settings(self, settings: Settings) -> None:
        self._settings = settings
        self._zone_debouncer = Debouncer(settings.sensitivity.zone_violation_seconds)
        self._prone_debouncer = Debouncer(settings.sensitivity.prone_position_seconds)
        self._cry_debouncer = Debouncer(settings.sensitivity.cry_window_seconds)

    # ── Vision path ───────────────────────────────────────────────────────────

    async def process_vision(self, detection: VisionDetection) -> None:
        now = time.monotonic()
        if not detection.person_detected:
            self._zone_debouncer.update(False, now)
            self._prone_debouncer.update(False, now)
            return
        await self._eval_zone(detection, now)
        await self._eval_prone(detection, now)

    def _should_push(self) -> bool:
        """Return False during configured quiet hours (suppresses FCM only)."""
        qh = self._settings.quiet_hours
        if not qh.enabled:
            return True
        now = datetime.now().strftime("%H:%M")
        start, end = qh.start, qh.end
        in_window = (now >= start or now <= end) if start > end else (start <= now <= end)
        return not in_window

    async def _eval_zone(self, d: VisionDetection, now: float) -> None:
        zone = self._safe_zone
        if zone is None or d.centroid_norm is None:
            self._zone_debouncer.update(False, now)
            return
        violation = check_zone_violation(d.centroid_norm, zone.polygon, zone.mode)
        if self._zone_debouncer.update(violation, now):
            await self._dispatcher.dispatch(
                Event(
                    type=EventType.ZONE_VIOLATION,
                    severity=Severity.WARNING,
                    timestamp=datetime.now(tz.utc),
                    confidence=d.confidence,
                    details=EventDetails(zone_mode=zone.mode),
                ),
                push=self._should_push(),
            )

    async def _eval_prone(self, d: VisionDetection, now: float) -> None:
        if self._prone_debouncer.update(d.is_prone_candidate, now):
            elapsed_ms = int(self._prone_debouncer.elapsed(now) * 1000)
            await self._dispatcher.dispatch(
                Event(
                    type=EventType.PRONE_POSITION,
                    severity=Severity.DANGER,
                    timestamp=datetime.now(tz.utc),
                    confidence=d.confidence,
                    details=EventDetails(duration_ms=elapsed_ms),
                ),
                push=self._should_push(),
            )

    # ── Audio path ────────────────────────────────────────────────────────────

    async def process_audio(self, detection: AudioDetection) -> None:
        now = time.monotonic()
        is_crying = detection.window_mean >= self._settings.sensitivity.cry_score_threshold
        if self._cry_debouncer.update(is_crying, now):
            await self._dispatcher.dispatch(
                Event(
                    type=EventType.CRY_DETECTED,
                    severity=Severity.WARNING,
                    timestamp=datetime.now(tz.utc),
                    confidence=detection.window_mean,
                ),
                push=self._should_push(),
            )
