CREATE TABLE IF NOT EXISTS events (
    id          TEXT PRIMARY KEY,
    type        TEXT NOT NULL,
    severity    TEXT NOT NULL,
    timestamp   TEXT NOT NULL,  -- UTC ISO-8601
    confidence  REAL NOT NULL,
    details     TEXT NOT NULL DEFAULT '{}'  -- JSON
);

CREATE INDEX IF NOT EXISTS idx_events_timestamp ON events(timestamp DESC);

CREATE TABLE IF NOT EXISTS safe_zone (
    id          INTEGER PRIMARY KEY CHECK (id = 1),  -- single-row table
    polygon     TEXT NOT NULL DEFAULT '[]',           -- JSON [[x,y], ...]
    mode        TEXT NOT NULL DEFAULT 'inside',
    updated_at  TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS settings (
    id       INTEGER PRIMARY KEY CHECK (id = 1),      -- single-row table
    data     TEXT NOT NULL DEFAULT '{}'               -- JSON
);

CREATE TABLE IF NOT EXISTS devices (
    id          TEXT PRIMARY KEY,   -- UUID
    fcm_token   TEXT NOT NULL UNIQUE,
    registered_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS schema_migrations (
    version   INTEGER PRIMARY KEY,
    applied_at TEXT NOT NULL
);
