#!/usr/bin/env bash
set -e
cd "$(dirname "$0")/web"
exec pnpm dev
