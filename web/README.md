# Sentinel — Web

Next.js 15 front-end for the Sentinel baby monitor. Korean UI (`ko` default via `next-intl`).

## Dev

```bash
pnpm install
cp .env.local.example .env.local   # fill in API base URL + Firebase vars
pnpm gen:types                      # regenerate TS types from shared/schemas
pnpm dev                            # http://localhost:3000
```

## Build

```bash
pnpm build
pnpm start
```

## Key conventions

- All server state via TanStack Query — no global state library.
- Tailwind only for styling; palette tokens in `tailwind.config.ts`.
- Types generated from `shared/schemas/` — edit the schema, then `pnpm gen:types`. Never edit generated types directly.
- `next-intl` messages in `messages/ko.json` (primary) and `messages/en.json` (fallback).
- FCM push via `public/firebase-messaging-sw.js` service worker; config fetched from `/api/firebase-config`.

See [`../CLAUDE.md`](../CLAUDE.md) for the full project technical contract.
