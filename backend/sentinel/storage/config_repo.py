from __future__ import annotations

import json
from datetime import datetime, timezone

from sentinel.storage.db import get_db
from sentinel.types import SafeZone, Settings


async def get_safe_zone() -> SafeZone | None:
    db = await get_db()
    async with db.execute("SELECT polygon, mode, updated_at FROM safe_zone WHERE id = 1") as cur:
        row = await cur.fetchone()
    if not row:
        return None
    return SafeZone(polygon=json.loads(row["polygon"]), mode=row["mode"], updated_at=datetime.fromisoformat(row["updated_at"]) if row["updated_at"] else None)


async def upsert_safe_zone(zone: SafeZone) -> SafeZone:
    db = await get_db()
    now = datetime.now(timezone.utc)
    polygon_json = json.dumps([[p[0], p[1]] for p in zone.polygon])
    await db.execute(
        "INSERT INTO safe_zone (id, polygon, mode, updated_at) VALUES (1,?,?,?) "
        "ON CONFLICT(id) DO UPDATE SET polygon=excluded.polygon, mode=excluded.mode, updated_at=excluded.updated_at",
        (polygon_json, zone.mode, now.isoformat()),
    )
    await db.commit()
    return zone.model_copy(update={"updated_at": now})


async def get_settings() -> Settings:
    db = await get_db()
    async with db.execute("SELECT data FROM settings WHERE id = 1") as cur:
        row = await cur.fetchone()
    if not row:
        return Settings()
    return Settings.model_validate_json(row["data"])


async def upsert_settings(settings: Settings) -> Settings:
    db = await get_db()
    await db.execute(
        "INSERT INTO settings (id, data) VALUES (1,?) "
        "ON CONFLICT(id) DO UPDATE SET data=excluded.data",
        (settings.model_dump_json(),),
    )
    await db.commit()
    return settings
