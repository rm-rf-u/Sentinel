# Web — Claude guidance

This is the Next.js 15 front-end for Sentinel. Read the root `CLAUDE.md` first for project-wide constraints.

## Stack

- **Next.js 15** (App Router) + TypeScript strict mode
- **Tailwind CSS** — palette tokens in `tailwind.config.ts`, no inline hex values
- **TanStack Query** — all server state; no Redux/Zustand
- **next-intl** — `ko` default locale, `en` fallback; messages in `messages/`
- **Fonts:** Pretendard (KR), Inter (Latin)

## Key conventions

- Types in `src/types/` are **generated** from `shared/schemas/` via `pnpm gen:types`. Never edit them directly — change the schema and regenerate.
- API base URL comes from `NEXT_PUBLIC_API_BASE` env var (Tailscale hostname + `:8000`).
- FCM push is handled by `public/firebase-messaging-sw.js` (service worker). It fetches Firebase config from `/api/firebase-config` on install.
- The live video feed uses WebRTC: SDP offer sent to `POST /api/webrtc/offer`, answer applied, then ICE candidates exchanged.
- Polygon coordinates are always normalized `[0,1]` — never store or send pixel coords.

## Dev commands

```bash
pnpm dev          # :3000
pnpm gen:types    # regenerate TS types from shared/schemas
pnpm test         # Vitest
pnpm lint         # ESLint
pnpm build        # production build
```

## What NOT to do

- Don't add a global state library (TanStack Query is enough).
- Don't use `any` in TypeScript.
- Don't edit generated types in `src/types/` directly.
- Don't add Vercel/cloud deployment config — this runs locally over Tailscale only.
