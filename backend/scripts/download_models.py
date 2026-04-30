#!/usr/bin/env python
"""Download all model checkpoints that require an internet connection.

Run once before starting the backend for the first time:
    mamba activate sentinel
    cd backend
    python scripts/download_models.py

Models downloaded:
  - YOLOv8n-pose.pt       (via ultralytics, auto-placed in data/models/)
  - PANNs CNN14 checkpoint (via curl/urllib, placed in ~/panns_data/)
"""

import os
import sys
import urllib.request
from pathlib import Path

MODEL_DIR = Path(__file__).parent.parent / "data" / "models"
MODEL_DIR.mkdir(parents=True, exist_ok=True)
PANNS_DIR = Path.home() / "panns_data"
PANNS_DIR.mkdir(parents=True, exist_ok=True)

# ── YOLOv8-pose ─────────────────────────────────────────────────────────────

yolo_pt = MODEL_DIR / "yolov8n-pose.pt"
if not yolo_pt.exists():
    print("Downloading yolov8n-pose.pt via ultralytics …")
    from ultralytics import YOLO
    m = YOLO("yolov8n-pose.pt")
    import shutil
    # ultralytics saves to ~/.config/Ultralytics/ or CWD — copy to data/models/
    src = Path("yolov8n-pose.pt")
    if src.exists():
        shutil.copy(src, yolo_pt)
        src.unlink()
    print(f"  → {yolo_pt}")
else:
    print(f"YOLOv8-pose already present: {yolo_pt}")

# ── PANNs CNN14 ──────────────────────────────────────────────────────────────

panns_ckpt = PANNS_DIR / "Cnn14_mAP=0.431.pth"
panns_csv = PANNS_DIR / "class_labels_indices.csv"

if not panns_csv.exists():
    print("Downloading AudioSet class labels …")
    urllib.request.urlretrieve(
        "http://storage.googleapis.com/us_audioset/youtube_corpus/v1/csv/class_labels_indices.csv",
        panns_csv,
    )
    print(f"  → {panns_csv}")

MIN_SIZE = 300_000_000  # ~300 MB
if not panns_ckpt.exists() or panns_ckpt.stat().st_size < MIN_SIZE:
    print("Downloading PANNs CNN14 checkpoint (~330 MB) …")
    url = "https://zenodo.org/record/3987831/files/Cnn14_mAP%3D0.431.pth?download=1"

    def _progress(count: int, block: int, total: int) -> None:
        if total > 0:
            pct = min(count * block / total * 100, 100)
            print(f"\r  {pct:.0f}%", end="", flush=True)

    urllib.request.urlretrieve(url, panns_ckpt, reporthook=_progress)
    print(f"\n  → {panns_ckpt}")
else:
    print(f"PANNs checkpoint already present: {panns_ckpt}")

print("\nAll models ready. You can now start the backend.")
