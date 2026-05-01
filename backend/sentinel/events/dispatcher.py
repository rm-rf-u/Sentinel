"""Fans out a fired Event to WebSocket clients, SQLite, and FCM push."""

from __future__ import annotations

from datetime import datetime, timezone as tz

import structlog

from sentinel.push import fcm, templates
from sentinel.storage import devices_repo, events_repo
from sentinel.transport.ws import ConnectionManager
from sentinel.types import Event, EventType, SensitivitySettings

logger = structlog.get_logger()

_EPOCH = datetime(1970, 1, 1, tzinfo=tz.utc)


class Dispatcher:
    def __init__(self, ws_manager: ConnectionManager) -> None:
        self._ws = ws_manager
        self._cooldowns: dict[EventType, float] = {
            EventType.ZONE_VIOLATION: 0.0,
            EventType.PRONE_POSITION: 0.0,
            EventType.CRY_DETECTED: 0.0,
        }
        self._last_push_at: dict[EventType, datetime] = {}

    def update_settings(self, sensitivity: SensitivitySettings) -> None:
        self._cooldowns = {
            EventType.ZONE_VIOLATION: sensitivity.zone_violation_cooldown_seconds,
            EventType.PRONE_POSITION: sensitivity.prone_position_cooldown_seconds,
            EventType.CRY_DETECTED: sensitivity.cry_detected_cooldown_seconds,
        }
        # Reset timestamps so new cooldown values take effect immediately
        self._last_push_at.clear()

    async def dispatch(self, event: Event, push: bool = True) -> None:
        logger.info(
            "event.dispatch",
            type=event.type,
            severity=event.severity,
            confidence=round(event.confidence, 2),
            push=push,
        )

        # 1. Persist
        await events_repo.insert_event(event)

        # 2. Live WebSocket broadcast (always — quiet hours only suppress push)
        await self._ws.broadcast(event.model_dump_json())

        # 3. FCM push — gated by quiet hours AND per-type cooldown
        if push and fcm.is_ready():
            now = datetime.now(tz.utc)
            last = self._last_push_at.get(event.type, _EPOCH)
            cooldown = self._cooldowns.get(event.type, 0.0)
            if (now - last).total_seconds() >= cooldown:
                tokens = await devices_repo.list_fcm_tokens()
                tmpl = templates.NOTIFICATION.get(event.type)
                if tokens and tmpl:
                    title, body = tmpl
                    await fcm.send_push(
                        tokens,
                        title,
                        body,
                        data={"event_id": str(event.id), "type": event.type},
                    )
                    self._last_push_at[event.type] = now
            else:
                logger.debug(
                    "event.push.cooldown",
                    type=event.type,
                    seconds_until=round(cooldown - (now - last).total_seconds(), 1),
                )
