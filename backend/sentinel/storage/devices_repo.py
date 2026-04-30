from __future__ import annotations

import uuid
from datetime import datetime, timezone

from sentinel.storage.db import get_db


async def register_device(fcm_token: str) -> str:
    db = await get_db()
    device_id = str(uuid.uuid4())
    now = datetime.now(timezone.utc).isoformat()
    await db.execute(
        "INSERT INTO devices (id, fcm_token, registered_at) VALUES (?,?,?) "
        "ON CONFLICT(fcm_token) DO UPDATE SET id=excluded.id, registered_at=excluded.registered_at",
        (device_id, fcm_token, now),
    )
    await db.commit()
    return device_id


async def unregister_device(device_id: str) -> bool:
    db = await get_db()
    async with db.execute("DELETE FROM devices WHERE id = ?", (device_id,)) as cur:
        deleted = cur.rowcount > 0
    await db.commit()
    return deleted


async def list_fcm_tokens() -> list[str]:
    db = await get_db()
    async with db.execute("SELECT fcm_token FROM devices") as cur:
        rows = await cur.fetchall()
    return [row["fcm_token"] for row in rows]
