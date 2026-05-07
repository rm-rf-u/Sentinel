<!-- 한국어 -->
# Sentinel

로컬 우선 방식의 영·유아 안전 모니터링 시스템입니다. Sentinel은 M1 Mac에 연결된 단일 카메라와 마이크를 감시하며, 모든 ML 추론을 기기 내에서 처리하고 저지연 라이브 피드와 실시간 안전 이벤트를 한국어 웹 앱 및 Android 앱으로 스트리밍합니다.

> **중요:** Sentinel은 **보조적 인식 도구**이며, 의료 기기가 아닙니다. SIDS(영아 돌연사 증후군) 또는 어떠한 의학적 상태도 예방하지 않습니다. 안전한 수면 지침은 반드시 자격을 갖춘 의료 제공자의 지시를 따르세요.

---

## 주요 기능

- 🎥 **저지연 라이브 피드** — WebRTC, Tailscale 네트워크 내 글래스-투-글래스 200ms 미만
- 🟢 **안전 구역 모니터링** — 라이브 피드 위에 폴리곤을 그려 아이가 구역에 들어오거나 나갈 때 알림
- 🛌 **엎드린 자세 감지** — 키포인트 기반 포즈 분석으로 아이가 일정 시간 이상 엎드려 있을 때 경고
- 😢 **울음 감지** — 기기 내 오디오 분류
- 🔕 **방해 금지 시간대, 민감도 및 쿨다운 설정** — 이벤트 유형별 알림 쿨다운, 방해 금지 시간대, 임계값 조정으로 알림 과부하 방지
- 📲 **푸시 알림** — 웹 및 Android용 FCM
- 🇰🇷 **한국어 UI** — Pretendard 서체, 따뜻한 '아침 아기방' 색상 팔레트
- 🔒 **네트워크 내 유지** — Tailscale 전용 접근, 클라우드 없음, 녹화 없음

## 아키텍처 개요

```
        ┌─────────────────────────────────────────────────────┐
        │                  M1 MacBook                          │
        │  카메라 ─┐                                            │
        │          ├─► 캡처 ─► 추론 ─► 이벤트 엔진              │
        │  마이크 ──┘  (링버퍼) (YOLOv8 +     │                  │
        │                        PANNs)        │                  │
        │                                      ▼                  │
        │             FastAPI ◄─── WebRTC ◄─── 디스패처           │
        │                │             │           │              │
        └────────────────┼─────────────┼───────────┼──────────────┘
                         │  Tailnet    │           │
                         ▼             ▼           ▼
                    REST + WS    WebRTC 영상    FCM 푸시
                         │             │           │
                         ▼             ▼           ▼
                      ┌────────────────────────────────┐
                      │   웹(Next.js)  +  Android       │
                      │    한국어 UI, FCM               │
                      └────────────────────────────────┘
```

- **백엔드:** Python (FastAPI + aiortc), M1의 Mamba 환경 `sentinel`에서 실행
- **웹:** Next.js 15 + TypeScript + Tailwind, `next-intl`로 한국어 지원
- **Android:** Kotlin + Jetpack Compose
- **저장소:** SQLite, 이벤트 로그만 (30일 롤링 보존). 영상은 저장하지 않음

전체 기술 계약은 [`CLAUDE.md`](./CLAUDE.md)를 참조하세요.

## 저장소 구조

```
sentinel/
├── shared/schemas/     # JSON 스키마 (Event, SafeZoneConfig, Settings) — 단일 진실 공급원
├── backend/            # Python: 캡처, 추론, 이벤트, 전송, API
├── web/                # Next.js 웹 앱
├── android/            # Android 앱
├── CLAUDE.md           # AI 지원 개발을 위한 기술 계약
└── README.md
```

## 요구 사항

- Apple Silicon(M1+) 기반 macOS
- [Mamba](https://mamba.readthedocs.io/) 또는 Conda
- Python 3.11+
- Node.js 20+ 및 `pnpm`
- Mac 및 모든 시청 기기에 [Tailscale](https://tailscale.com/) 설치
- Firebase 프로젝트 (FCM용) — **프로젝트: sentinel-be1eb**
- Android Studio (Android APK 빌드용)

## Firebase 자격 증명 파일

이 파일들은 `.gitignore`에 포함됩니다. [console.firebase.google.com](https://console.firebase.google.com) → 프로젝트 **sentinel-be1eb**에서 획득하세요:

| 파일 | 출처 |
|---|---|
| `android/app/google-services.json` | 프로젝트 설정 → Android 앱 |
| `web/.env.local` | 프로젝트 설정 → 웹 앱 설정 + Cloud Messaging VAPID 키 |
| `backend/data/fcm-sa.json` | 프로젝트 설정 → 서비스 계정 → 새 개인 키 생성 |

## 설치

### 1. 클론 및 환경 생성

```bash
cd sentinel
mamba create -n sentinel python=3.11
mamba activate sentinel
```

### 2. 백엔드

```bash
cd backend
uv pip install -e ".[dev]"
cp .env.example .env
# .env 편집: TAILSCALE_HOSTNAME 설정
# backend/data/fcm-sa.json 배치 (Firebase 콘솔에서 획득)
python scripts/download_models.py  # 최초 1회: YOLOv8-pose + PANNs CNN14
python scripts/export_coreml.py    # 최초 1회: YOLOv8-pose를 CoreML/ANE로 변환
./scripts/dev.sh                   # :8000에서 uvicorn --reload 실행
```

### 3. 웹

```bash
cd web
pnpm install
cp .env.local.example .env.local
# .env.local 편집: NEXT_PUBLIC_API_BASE 설정 (Tailscale 호스트명)
# 모든 NEXT_PUBLIC_FIREBASE_* 변수 및 VAPID 키 입력 (Firebase 콘솔에서 획득)
pnpm gen:types                    # shared/schemas로부터 TS 타입 재생성
pnpm dev                          # :3000
```

### 4. Android APK

```bash
cd android
# android/app/google-services.json 배치 (Firebase 콘솔에서 획득)
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
ANDROID_HOME="$HOME/Library/Android/sdk" \
./gradlew assembleRelease
# 출력: app/build/outputs/apk/release/app-release.apk
```

### 5. 접속

Tailscale 네트워크의 모든 기기에서 Mac의 MagicDNS 호스트명 포트 3000으로 접속:

```
http://your-mac.<tail-net>.ts.net:3000
```

## 개발

| 작업 | 명령어 |
|---|---|
| 백엔드 실행 (빠름) | `./start.sh` |
| 백엔드 감시 모드 실행 | `cd backend && ./scripts/dev.sh` |
| 웹 감시 모드 실행 | `cd web && pnpm dev` |
| 백엔드 테스트 | `cd backend && pytest` |
| 웹 테스트 | `cd web && pnpm test` |
| 백엔드 린트 | `cd backend && ruff check && mypy .` |
| 웹 린트 | `cd web && pnpm lint` |
| TS 타입 재생성 | `cd web && pnpm gen:types` |

`shared/schemas/`의 스키마를 변경할 경우, 웹 타입을 재생성하고 `backend/sentinel/types.py`의 해당 pydantic 모델을 업데이트하세요.

## 프로젝트 현황

| 단계 | 상태 |
|---|---|
| 0 — 스캐폴드 | ✅ |
| 1 — 라이브 피드 엔드-투-엔드 | ✅ |
| 2 — 안전 구역 편집기 | ✅ |
| 3 — 비전 ML | ✅ |
| 4 — 오디오 ML | ✅ |
| 5 — 이벤트 로그 + 설정 UI | ✅ |
| 6 — FCM 푸시 | ✅ |
| 7 — Android 앱 | ✅ |
| 8 — 완성도 향상 (온보딩, 보정) | ✅ |

## 설계 원칙

- **로컬 우선.** 영상은 Mac을 벗어나지 않습니다. 추론은 기기 내에서 처리(CoreML).
- **처리량보다 지연 시간 우선.** WebRTC, 손실 허용 링 버퍼, 독립 추론 루프.
- **단일 진실 공급원.** `shared/`의 JSON 스키마가 백엔드와 웹 타입을 모두 구동.
- **따뜻하고 차분한 UI.** 순수 빨간색 대신 딥 코랄, 흰색 대신 크림 — 새벽 3시에도 부드럽게.
- **오탐 알림 방지.** 모든 규칙은 디바운스되며, 방해 금지 시간대·민감도 임계값·이벤트 유형별 알림 쿨다운이 웹 및 Android 앱에서 모두 조정 가능한 1등급 설정입니다.

## 색상 팔레트

'아침 아기방' 팔레트. 토큰은 Tailwind 및 Material 3에 연결되어 있습니다.

| 역할 | Hex |
|---|---|
| 배경 | `#FFF8F1` |
| 주요 색상 (살구) | `#F4A261` |
| 강조 색상 (세이지) | `#8FB996` |
| 경고 | `#FFB703` |
| 위험 (딥 코랄) | `#D62828` |
| 기본 텍스트 | `#3D2C2A` |

## 개인정보 보호

- 모든 ML은 Mac에서 실행됩니다. 프레임, 오디오, 키포인트는 어디에도 업로드되지 않습니다.
- 외부로 나가는 유일한 네트워크 트래픽은 Google FCM(알림 메타데이터만 — 프레임 없음)입니다.
- 이벤트 로그는 30일 롤링 보존 정책의 로컬 SQLite 파일에 저장됩니다.
- 라이브 피드 접근은 Tailscale로 제한되며, 사용자의 tailnet 구성원만 연결할 수 있습니다.

## 면책 조항

Sentinel은 어떠한 종류의 보증도 없이 있는 그대로 제공되는 개인 프로젝트입니다. 이 소프트웨어는 **의료 기기가 아니며**, **안전 필수 용도에 대한 인증을 받지 않았고**, 직접 감독을 대체하거나 영아 돌연사 증후군(SIDS) 예방을 포함한 어떠한 의료 목적으로도 **사용해서는 안 됩니다**. 항상 소아과 의사의 안전한 수면 지침을 따르세요.

## 라이선스

프로젝트 소유자가 결정 예정.

---

<!-- English below / 영어 내용은 아래에 -->

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
