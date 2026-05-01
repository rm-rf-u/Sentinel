"""Pydantic models matching shared/schemas/*.schema.json — edit schemas first, then update here."""

from __future__ import annotations

import uuid
from datetime import datetime
from enum import StrEnum
from typing import Any

from pydantic import BaseModel, Field


class EventType(StrEnum):
    ZONE_VIOLATION = "zone_violation"
    PRONE_POSITION = "prone_position"
    CRY_DETECTED = "cry_detected"


class Severity(StrEnum):
    INFO = "info"
    WARNING = "warning"
    DANGER = "danger"


class ZoneMode(StrEnum):
    INSIDE = "inside"
    OUTSIDE = "outside"


class EventDetails(BaseModel):
    zone_mode: ZoneMode | None = None
    duration_ms: int | None = None


class Event(BaseModel):
    id: uuid.UUID = Field(default_factory=uuid.uuid4)
    type: EventType
    severity: Severity
    timestamp: datetime
    confidence: float = Field(ge=0.0, le=1.0)
    details: EventDetails = Field(default_factory=EventDetails)


class SafeZone(BaseModel):
    polygon: list[tuple[float, float]] = Field(min_length=3)
    mode: ZoneMode
    updated_at: datetime | None = None


class SensitivitySettings(BaseModel):
    zone_violation_seconds: float = Field(default=2.0, ge=0.5, le=30.0)
    prone_position_seconds: float = Field(default=5.0, ge=1.0, le=60.0)
    cry_score_threshold: float = Field(default=0.35, ge=0.1, le=1.0)
    cry_window_seconds: float = Field(default=3.0, ge=1.0, le=10.0)
    zone_violation_cooldown_seconds: float = Field(default=0.0, ge=0.0, le=600.0)
    prone_position_cooldown_seconds: float = Field(default=0.0, ge=0.0, le=600.0)
    cry_detected_cooldown_seconds: float = Field(default=0.0, ge=0.0, le=600.0)


class QuietHours(BaseModel):
    enabled: bool = False
    start: str = "22:00"
    end: str = "07:00"


class NotificationSettings(BaseModel):
    fcm_enabled: bool = True


class Settings(BaseModel):
    sensitivity: SensitivitySettings = Field(default_factory=SensitivitySettings)
    quiet_hours: QuietHours = Field(default_factory=QuietHours)
    notifications: NotificationSettings = Field(default_factory=NotificationSettings)


class WebRTCOffer(BaseModel):
    sdp: str
    type: str = "offer"


class WebRTCAnswer(BaseModel):
    sdp: str
    type: str = "answer"
