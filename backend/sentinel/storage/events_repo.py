from __future__ import annotations

import json
from datetime import datetime, timezone, timedelta

from sentinel.storage.db import get_db
from sentinel.types import Event


async def insert_event(event: Event) -> None:
    db = await get_db()
    await db.execute(
        "INSERT INTO events (id, type, severity, timestamp, confidence, details) VALUES (?,?,?,?,?,?)",
        (
            str(event.id),
            event.type,
            event.severity,
            event.timestamp.isoformat(),
            event.confidence,
            event.details.model_dump_json(),
        ),
    )
    await db.commit()


async def list_events(limit: int = 50, before: str | None = None) -> list[dict]:
    db = await get_db()
    if before:
        async with db.execute(
            "SELECT * FROM events WHERE timestamp < ? ORDER BY timestamp DESC LIMIT ?",
            (before, limit),
        ) as cur:
            rows = await cur.fetchall()
    else:
        async with db.execute(
            "SELECT * FROM events ORDER BY timestamp DESC LIMIT ?", (limit,)
        ) as cur:
            rows = await cur.fetchall()
    return [_parse_row(row) for row in rows]


def _parse_row(row: object) -> dict:
    d = dict(row)  # type: ignore[call-overload]
    try:
        d["details"] = json.loads(d.get("details") or "{}")
    except (json.JSONDecodeError, TypeError):
        d["details"] = {}
    return d


async def clear_all_events() -> int:
    db = await get_db()
    async with db.execute("DELETE FROM events") as cur:
        deleted = cur.rowcount
    await db.commit()
    return deleted


async def purge_old_events(days: int = 30) -> int:
    db = await get_db()
    cutoff = (datetime.now(timezone.utc) - timedelta(days=days)).isoformat()
    async with db.execute("DELETE FROM events WHERE timestamp < ?", (cutoff,)) as cur:
        deleted = cur.rowcount
    await db.commit()
    return deleted
