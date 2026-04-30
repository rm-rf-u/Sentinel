from __future__ import annotations

import importlib.resources
from pathlib import Path

import aiosqlite
import structlog

logger = structlog.get_logger()

_conn: aiosqlite.Connection | None = None


async def get_db() -> aiosqlite.Connection:
    if _conn is None:
        raise RuntimeError("Database not initialised — call init_db() first")
    return _conn


async def init_db(db_path: Path) -> None:
    global _conn
    db_path.parent.mkdir(parents=True, exist_ok=True)
    _conn = await aiosqlite.connect(db_path)
    _conn.row_factory = aiosqlite.Row
    await _conn.execute("PRAGMA journal_mode=WAL")
    await _conn.execute("PRAGMA foreign_keys=ON")
    await _run_migrations(_conn)
    logger.info("db.ready", path=str(db_path))


async def close_db() -> None:
    global _conn
    if _conn:
        await _conn.close()
        _conn = None


async def _run_migrations(conn: aiosqlite.Connection) -> None:
    migrations_dir = Path(__file__).parent / "migrations"
    sql_files = sorted(migrations_dir.glob("*.sql"))

    await conn.execute(
        "CREATE TABLE IF NOT EXISTS schema_migrations "
        "(version INTEGER PRIMARY KEY, applied_at TEXT NOT NULL)"
    )

    async with conn.execute("SELECT version FROM schema_migrations") as cur:
        applied = {row[0] for row in await cur.fetchall()}

    for sql_file in sql_files:
        version = int(sql_file.stem.split("_")[0])
        if version in applied:
            continue
        sql = sql_file.read_text()
        await conn.executescript(sql)
        await conn.execute(
            "INSERT INTO schema_migrations (version, applied_at) VALUES (?, datetime('now'))",
            (version,),
        )
        await conn.commit()
        logger.info("db.migration.applied", version=version)
