#!/usr/bin/env bash
set -e
source "$(conda info --base)/etc/profile.d/conda.sh"
conda activate sentinel
exec uvicorn sentinel.main:app --host 0.0.0.0 --port 8000 --reload --app-dir backend
