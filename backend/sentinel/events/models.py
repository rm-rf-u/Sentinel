"""Internal data models for the event pipeline (not persisted directly)."""

from __future__ import annotations

from dataclasses import dataclass

from sentinel.inference.geometry import Point


@dataclass
class VisionDetection:
    """Parsed output from one YOLOv8-pose inference frame."""

    person_detected: bool = False
    centroid_norm: Point | None = None      # normalised (x,y) centre of bbox
    is_prone_candidate: bool = False
    confidence: float = 0.0                 # highest-confidence detection score
    frame_number: int = 0


@dataclass
class AudioDetection:
    """Parsed output from one PANNs inference window."""

    cry_score: float = 0.0      # instantaneous class probability [0, 1]
    window_mean: float = 0.0    # rolling mean over recent windows
    frame_number: int = 0
