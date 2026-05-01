# CLAUDE.md

Guidance for Claude Code working in this repository. OpenAI Codex will review your output.

## What this project is

**Sentinel** is a baby/young-child safety monitoring system. Camera + microphone watch the child; the system fires events for three conditions:

1. **Zone violation** — child enters or leaves a user-defined polygon
2. **Prone position** — child face-down for >5s (SIDS *awareness*; framed as a supplementary tool, **never** as medical/SIDS prevention in user-facing copy)
3. **Cry detection** — sustained baby cry on the audio stream

Events stream live to a Korean-language web app and Android app, plus FCM push.

## Hard constraints — do not violate

- **Backend runs on M1 MacBook only** (dev setup). Jetson is a future target, not in scope. Use CoreML for ML, AVFoundation for camera, sounddevice for mic.
- **Tailscale is the auth boundary.** No app-level login, no JWT, no OAuth. Backend is reachable only via the user's tailnet.
- **Single user, single camera.** Don't add multi-tenant abstractions, account systems, or device fleets.
- **No video recording.** Event log only (SQLite, 30-day rolling). Footage never leaves the device.
- **All ML is on-device.** No cloud inference. Privacy + latency.
- **Frontend UI is Korean** (`ko` default in `next-intl`, `strings.xml` KR primary on Android). Backend code/comments are English. Push notification *body* text is KR.
- **Never use medical/SIDS-prevention language** in user-facing copy. "엎드린 자세 감지 — 아기를 확인해 주세요" not "SIDS prevention."
- **Polygon coordinates are normalized [0,1] everywhere.** Never store pixel coords.
- **All times are UTC** in storage and over the wire. Clients render in local TZ.

## Architecture (full detail in `shared/schemas/` and module docstrings)

```
sentinel/
├── shared/schemas/        # JSON Schema = single source of truth for Event, SafeZoneConfig, Settings
├── backend/               # Python, mamba env "sentinel"
│   └── sentinel/
│       ├── capture/       # camera + mic producer threads → ring buffers
│       ├── inference/     # YOLOv8-pose CoreML @ 10fps, PANNs CNN14 audio
│       ├── events/        # rule engine, debouncer, dispatcher
│       ├── storage/       # aiosqlite repos, migrations
│       ├── transport/     # aiortc WebRTC, WebSocket manager
│       ├── push/          # FCM Admin SDK
│       └── api/           # FastAPI routes
├── web/                   # Next.js 15 + TS + Tailwind, KR-default
└── android/               # Kotlin + Compose
```

**Concurrency model:** single asyncio loop hosts FastAPI + aiortc + WS. Capture runs in dedicated threads (blocking I/O), pushes into lossy ring buffers via `loop.call_soon_threadsafe`. Inference runs in a thread-pool executor. Don't block the event loop.

**Data flow:** `camera → ring buffer → [WebRTC encoder] + [vision @10fps]`, `mic → ring buffer → [audio @2Hz]`, both → `EventEngine (debounce + rules) → Dispatcher → [WS broadcast | SQLite | FCM]`.

## Schemas (authoritative)

Defined in `shared/schemas/*.schema.json`:

- `event.schema.json` — `{id, type: zone_violation|prone_position|cry_detected, severity, timestamp, confidence, details}`
- `safe_zone.schema.json` — `{polygon: [[x,y]...], mode: inside|outside, updated_at}` (normalized coords)
- `settings.schema.json` — `{sensitivity: {...thresholds_in_seconds_or_score}, quiet_hours, notifications}`

Backend pydantic models and web TS types are derived from these. **Edit the schema first**, then regenerate types — don't drift.

## API surface

Stable REST + WS endpoints — see `backend/sentinel/api/` for routes.

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/webrtc/offer` | SDP offer/answer for live feed |
| GET/PUT | `/api/safe-zone` | Zone config |
| GET/PUT | `/api/settings` | Sensitivity, quiet hours |
| GET | `/api/events` | Paginated log |
| DELETE | `/api/events` | Clear all events |
| POST/DELETE | `/api/devices` | FCM token registration |
| WS | `/ws/events` | Live event stream |

## Build / dev commands

```bash
# Backend only (quickest — use this for Android-only workflows)
./start.sh                        # activates mamba env + uvicorn --reload on :8000

# Backend (manual)
mamba activate sentinel
cd backend && uv pip install -e ".[dev]"
./scripts/dev.sh                  # uvicorn --reload on :8000

# Web
cd web && pnpm install
pnpm gen:types                    # regenerate TS from shared/schemas
pnpm dev                          # :3000

# Tests
cd backend && pytest
cd web && pnpm test
```

Android only needs the backend. Both servers are needed to use the web browser UI.

## Android APK
```bash
cd android
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
ANDROID_HOME="/Users/steven/Library/Android/sdk" \
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

## Firebase / FCM

Firebase project: **sentinel-be1eb** (console.firebase.google.com)

Credential files (all gitignored — do not commit):
- `android/app/google-services.json` — Android SDK config
- `web/.env.local` — all `NEXT_PUBLIC_FIREBASE_*` vars + VAPID key
- `backend/data/fcm-sa.json` — Admin SDK service account

To re-obtain on a new machine: Firebase console → Project sentinel-be1eb → Project settings.

## Conventions

- **Python:** ruff + mypy strict, async-first, `structlog` for logging, pydantic v2 models, `aiosqlite` (no ORM — repos hold raw SQL).
- **TypeScript:** strict mode, no `any`. TanStack Query for server state. No client-state library — component state is enough.
- **Styling:** Tailwind only, palette tokens in `tailwind.config.ts`. Pretendard for KR, Inter for Latin.
- **Tests:** `pytest` for backend (unit + integration), Vitest for web. Geometry helpers (`point-in-polygon`) must have **shared JSON fixtures** so backend and web behave identically.
- **Commits:** conventional-commits style (`feat:`, `fix:`, `chore:`).
- **Comments:** rare. Only when *why* is non-obvious.

## Color palette (warm/bright nursery)

| Token | Hex | Use |
|---|---|---|
| `bg` | `#FFF8F1` | App background |
| `surface` | `#FFFFFF` | Cards |
| `primary` | `#F4A261` | Buttons, primary actions |
| `primary-deep` | `#E76F51` | Hover/pressed |
| `accent` | `#8FB996` | Safe-zone polygon, "all good" |
| `warning` | `#FFB703` | Soft alerts |
| `danger` | `#D62828` | Critical alerts (deep coral, gentler at 3am than pure red) |
| `text-primary` | `#3D2C2A` | Warm near-black |
| `text-secondary` | `#7A6A66` | Warm gray |

Rounded corners 12-16px. Generous padding. Don't use harsh black/red.

## Build phases

All 9 phases complete (Phases 0–8). See `README.md` for the full list.

## Things to avoid

- ❌ Cloud inference, server-side recording, account systems
- ❌ Pixel-space polygon coords (must be normalized)
- ❌ Synchronous file/network I/O on the asyncio loop
- ❌ Pure red `#FF0000` for alerts — use `#D62828`
- ❌ Medical claims in copy ("prevents SIDS," "medical-grade")
- ❌ Adding a SQL ORM, Redux/Zustand, or cloud auth — reasoning above
- ❌ Editing generated TS types directly — change the schema and regenerate

## When in doubt

Re-read this file and the relevant `shared/schemas/*.json`. If those don't answer the question, ask the user — don't invent a convention.
