"""Firebase Admin SDK wrapper for FCM push notifications.

Initialised lazily so the backend starts fine even without a service account.
All public functions are safe to call when FCM is not configured — they no-op.
"""

from __future__ import annotations

import asyncio
from pathlib import Path

import structlog

logger = structlog.get_logger()

_initialised = False


def init(service_account_path: Path) -> bool:
    """Initialise the Firebase Admin SDK. Returns True on success."""
    global _initialised
    if _initialised:
        return True
    if not service_account_path.exists():
        logger.warning(
            "fcm.not_configured",
            hint=f"Place service account JSON at {service_account_path} to enable push",
        )
        return False
    try:
        import firebase_admin
        from firebase_admin import credentials

        cred = credentials.Certificate(str(service_account_path))
        firebase_admin.initialize_app(cred)
        _initialised = True
        logger.info("fcm.ready")
        return True
    except Exception:
        logger.exception("fcm.init_failed")
        return False


def is_ready() -> bool:
    return _initialised


async def send_push(
    tokens: list[str],
    title: str,
    body: str,
    data: dict[str, str] | None = None,
) -> None:
    """Send a multicast FCM push. Silently no-ops if FCM is not configured."""
    if not _initialised or not tokens:
        return
    try:
        from firebase_admin import messaging

        msg = messaging.MulticastMessage(
            tokens=tokens,
            notification=messaging.Notification(title=title, body=body),
            data=data or {},
            android=messaging.AndroidConfig(priority="high"),
            apns=messaging.APNSConfig(
                payload=messaging.APNSPayload(aps=messaging.Aps(sound="default"))
            ),
        )
        loop = asyncio.get_event_loop()
        response = await loop.run_in_executor(
            None, messaging.send_each_for_multicast, msg
        )
        failed = [
            tokens[i][:20]
            for i, r in enumerate(response.responses)
            if not r.success
        ]
        if failed:
            logger.warning("fcm.send_partial_failure", failed_count=len(failed))
        else:
            logger.debug("fcm.sent", count=len(tokens))
    except Exception:
        logger.exception("fcm.send_error")
