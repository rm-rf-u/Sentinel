"""Fans out a fired Event to WebSocket clients, SQLite, and FCM push."""

from __future__ import annotations

import structlog

from sentinel.push import fcm, templates
from sentinel.storage import devices_repo, events_repo
from sentinel.transport.ws import ConnectionManager
from sentinel.types import Event

logger = structlog.get_logger()


class Dispatcher:
    def __init__(self, ws_manager: ConnectionManager) -> None:
        self._ws = ws_manager

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

        # 3. FCM push
        if push and fcm.is_ready():
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
