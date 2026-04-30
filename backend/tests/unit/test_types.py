"""Smoke tests for pydantic models — ensures schema and types stay in sync."""

from sentinel.types import Event, EventType, SafeZone, Settings, Severity, ZoneMode


def test_event_defaults():
    event = Event(
        type=EventType.CRY_DETECTED,
        severity=Severity.WARNING,
        timestamp="2024-01-01T00:00:00Z",
        confidence=0.75,
    )
    assert event.id is not None
    assert event.details.zone_mode is None


def test_safe_zone_normalized():
    zone = SafeZone(polygon=[[0.1, 0.2], [0.5, 0.8], [0.9, 0.2]], mode=ZoneMode.INSIDE)
    assert len(zone.polygon) == 3


def test_settings_defaults():
    s = Settings()
    assert s.sensitivity.zone_violation_seconds == 2.0
    assert s.sensitivity.prone_position_seconds == 5.0
    assert not s.quiet_hours.enabled
