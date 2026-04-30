#!/usr/bin/env python
"""One-time script: download yolov8n-pose.pt and export to CoreML (.mlpackage).

Run once before starting the backend:
    mamba activate sentinel
    cd backend
    python scripts/export_coreml.py
"""

import shutil
import sys
from pathlib import Path

MODEL_DIR = Path(__file__).parent.parent / "data" / "models"
MODEL_DIR.mkdir(parents=True, exist_ok=True)

OUT = MODEL_DIR / "yolov8n-pose.mlpackage"

if OUT.exists():
    print(f"Already exported: {OUT}")
    sys.exit(0)

print("Downloading yolov8n-pose.pt …")
from ultralytics import YOLO  # noqa: E402

model = YOLO("yolov8n-pose.pt")

print("Exporting to CoreML (this takes ~1 minute) …")
export_path = model.export(format="coreml", imgsz=640, nms=True, half=False)

# Ultralytics saves next to the .pt; move into data/models/
src = Path(export_path)
dst = MODEL_DIR / src.name
if src.resolve() != dst.resolve():
    shutil.move(str(src), str(dst))

print(f"Saved → {dst}")
print("You can now start the backend — it will use the CoreML model automatically.")
