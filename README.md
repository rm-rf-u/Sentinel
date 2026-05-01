# Sentinel

A local-first baby & young-child safety monitor. Sentinel watches a single camera + microphone on your M1 Mac, runs all ML on-device, and streams a low-latency live feed plus real-time safety events to a Korean-language web app and Android app.

> **Important:** Sentinel is a **supplementary awareness tool**, not a medical device. It does not prevent SIDS or any medical condition. Always follow safe-sleep guidelines from a qualified healthcare provider.

---

## Features

- 🎥 **Low-latency live feed** — WebRTC, <200ms glass-to-glass over your tailnet
- 🟢 **Safe-zone monitoring** — draw a polygon on the live feed; alerts when the child enters or exits (configurable per zone)
- 🛌 **Prone-position detection** — keypoint-based pose analysis warns when the child has been face-down for too long
- 😢 **Cry detection** — on-device audio classification
- 🔕 **Quiet hours, sensitivity & cooldowns** — per-event-type notification cooldowns, quiet hours, and threshold tuning to avoid alert spam
- 📲 **Push notifications** — FCM to web and Android
- 🇰🇷 **Korean UI** — Pretendard typography, warm "morning nursery" palette
- 🔒 **Stays on your network** — Tailscale-only access, no cloud, no recording

## Architecture at a glance

```
        ┌─────────────────────────────────────────────────────┐
        │                  M1 MacBook                          │
        │  Camera ─┐                                           │
        │          ├─► Capture ─► Inference ─► Event Engine    │
        │  Mic   ──┘   (rings)   (YOLOv8 +     │               │
        │                         PANNs)        │               │
        │                                       ▼               │
        │              FastAPI ◄─── WebRTC ◄─── Dispatcher      │
        │                 │             │           │           │
        └─────────────────┼─────────────┼───────────┼───────────┘
                          │  Tailnet    │           │
                          ▼             ▼           ▼
                     REST + WS    WebRTC video    FCM push
                          │             │           │
                          ▼             ▼           ▼
                       ┌────────────────────────────────┐
                       │   Web (Next.js)  +  Android    │
                       │       Korean UI, FCM           │
                       └────────────────────────────────┘
```

- **Backend:** Python (FastAPI + aiortc), runs in a Mamba env named `sentinel` on M1.
- **Web:** Next.js 15 + TypeScript + Tailwind, KR via `next-intl`.
- **Android:** Kotlin + Jetpack Compose.
- **Storage:** SQLite, event log only (30-day rolling). No video recorded.

See [`CLAUDE.md`](./CLAUDE.md) for the full technical contract.

## Repository layout

```
sentinel/
├── shared/schemas/     # JSON Schema (Event, SafeZoneConfig, Settings) — source of truth
├── backend/            # Python: capture, inference, events, transport, API
├── web/                # Next.js web app
├── android/            # Android app
├── CLAUDE.md           # Technical contract for AI-assisted development
└── README.md
```

## Requirements

- macOS on Apple Silicon (M1+)
- [Mamba](https://mamba.readthedocs.io/) or Conda
- Python 3.11+
- Node.js 20+ and `pnpm`
- [Tailscale](https://tailscale.com/) installed on the Mac and on every viewing device
- A Firebase project (for FCM) — **project: sentinel-be1eb**
- Android Studio (for building the Android APK)

## Firebase credential files

These are gitignored. Obtain them from [console.firebase.google.com](https://console.firebase.google.com) → project **sentinel-be1eb**:

| File | Source |
|---|---|
| `android/app/google-services.json` | Project settings → Android app |
| `web/.env.local` | Project settings → Web app config + Cloud Messaging VAPID key |
| `backend/data/fcm-sa.json` | Project settings → Service accounts → Generate new private key |

## Setup

### 1. Clone and create the env

```bash
cd sentinel
mamba create -n sentinel python=3.11
mamba activate sentinel
```

### 2. Backend

```bash
cd backend
uv pip install -e ".[dev]"
cp .env.example .env
# edit .env: set TAILSCALE_HOSTNAME
# place backend/data/fcm-sa.json (from Firebase console)
python scripts/download_models.py  # one-time: YOLOv8-pose + PANNs CNN14
python scripts/export_coreml.py    # one-time: convert YOLOv8-pose to CoreML/ANE
./scripts/dev.sh                   # uvicorn --reload on :8000
```

### 3. Web

```bash
cd web
pnpm install
cp .env.local.example .env.local
# edit .env.local: set NEXT_PUBLIC_API_BASE (Tailscale hostname)
# fill in all NEXT_PUBLIC_FIREBASE_* vars and VAPID key (from Firebase console)
pnpm gen:types                    # regenerate TS types from shared/schemas
pnpm dev                          # :3000
```

### 4. Android APK

```bash
cd android
# place android/app/google-services.json (from Firebase console)
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
ANDROID_HOME="$HOME/Library/Android/sdk" \
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

### 5. View

On any device on your tailnet, browse to the Mac's MagicDNS hostname on port 3000:

```
http://your-mac.<tail-net>.ts.net:3000
```

## Development

| Task | Command |
|---|---|
| Run backend (quickest) | `./start.sh` |
| Run backend in watch mode | `cd backend && ./scripts/dev.sh` |
| Run web in watch mode | `cd web && pnpm dev` |
| Backend tests | `cd backend && pytest` |
| Web tests | `cd web && pnpm test` |
| Lint backend | `cd backend && ruff check && mypy .` |
| Lint web | `cd web && pnpm lint` |
| Regenerate TS types | `cd web && pnpm gen:types` |

When you change a schema in `shared/schemas/`, regenerate web types and update the matching pydantic model in `backend/sentinel/types.py`.

## Project status

| Phase | Status |
|---|---|
| 0 — Scaffold | ✅ |
| 1 — Live feed end-to-end | ✅ |
| 2 — Safe-zone editor | ✅ |
| 3 — Vision ML | ✅ |
| 4 — Audio ML | ✅ |
| 5 — Event log + settings UI | ✅ |
| 6 — FCM push | ✅ |
| 7 — Android app | ✅ |
| 8 — Polish (onboarding, calibration) | ✅ |

## Design principles

- **Local first.** Footage never leaves the Mac. Inference is on-device (CoreML).
- **Latency over throughput.** WebRTC, lossy ring buffers, decoupled inference loop.
- **Single source of truth.** JSON schemas in `shared/` drive both backend and web types.
- **Warm, calm UI.** Deep coral instead of pure red, cream instead of white — quieter at 3am.
- **No false-positive spam.** Every rule is debounced; quiet hours, sensitivity thresholds, and per-type notification cooldowns are all first-class settings tunable from the web and Android apps.

## Color palette

A "morning nursery" palette. Tokens are wired into Tailwind and Material 3.

| Role | Hex |
|---|---|
| Background | `#FFF8F1` |
| Primary (apricot) | `#F4A261` |
| Accent (sage) | `#8FB996` |
| Warning | `#FFB703` |
| Danger (deep coral) | `#D62828` |
| Text primary | `#3D2C2A` |

## Privacy

- All ML runs on the Mac. No frames, audio, or keypoints are uploaded anywhere.
- The only outbound network traffic is to Google FCM (notification metadata only — never frames).
- The event log lives in a local SQLite file with 30-day rolling retention.
- Access to the live feed is gated by Tailscale; only your tailnet members can connect.

## Disclaimer

Sentinel is a personal project provided as-is, with no warranty of any kind. It is **not** a medical device, **not** certified for safety-critical use, and **must not** be used as a replacement for direct supervision or for any medical purpose, including the prevention of Sudden Infant Death Syndrome (SIDS). Always follow safe-sleep guidelines from your pediatrician.

## License

TBD by the project owner.
