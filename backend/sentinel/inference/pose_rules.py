"""Rules evaluated on YOLOv8-pose keypoints.

COCO keypoint indices used here:
  0 nose  1 left_eye  2 right_eye  5 left_shoulder  6 right_shoulder
"""

from __future__ import annotations

import numpy as np

from sentinel.inference.geometry import Point, point_in_polygon, normalize

# COCO keypoint indices
NOSE = 0
LEFT_EYE = 1
RIGHT_EYE = 2
LEFT_SHOULDER = 5
RIGHT_SHOULDER = 6

FACE_INDICES = (NOSE, LEFT_EYE, RIGHT_EYE)
SHOULDER_INDICES = (LEFT_SHOULDER, RIGHT_SHOULDER)


def get_centroid_norm(boxes_xyxy: np.ndarray, frame_h: int, frame_w: int) -> Point | None:
    """Return the centre of the highest-confidence bounding box in normalised coords."""
    if boxes_xyxy is None or len(boxes_xyxy) == 0:
        return None
    # boxes_xyxy shape: (N, 4) — take first (highest-conf) detection
    x1, y1, x2, y2 = boxes_xyxy[0]
    cx = float((x1 + x2) / 2)
    cy = float((y1 + y2) / 2)
    return normalize((cx, cy), frame_w, frame_h)


def check_prone(
    keypoints_xy: np.ndarray,
    keypoints_conf: np.ndarray,
    conf_threshold: float = 0.35,
) -> bool:
    """Return True if the detected person is likely face-down.

    Heuristic: nose y-coordinate is below (greater than) the average shoulder
    y-coordinate by at least a small margin. This works for side-angle and
    moderately angled cameras typical of a nursery setup.

    All involved keypoints must exceed conf_threshold.
    """
    if keypoints_xy is None or keypoints_conf is None:
        return False
    if keypoints_xy.shape[0] < max(FACE_INDICES + SHOULDER_INDICES) + 1:
        return False

    nose_conf = float(keypoints_conf[NOSE])
    ls_conf = float(keypoints_conf[LEFT_SHOULDER])
    rs_conf = float(keypoints_conf[RIGHT_SHOULDER])

    if nose_conf < conf_threshold or ls_conf < conf_threshold or rs_conf < conf_threshold:
        return False

    nose_y = float(keypoints_xy[NOSE][1])
    shoulder_mid_y = (float(keypoints_xy[LEFT_SHOULDER][1]) + float(keypoints_xy[RIGHT_SHOULDER][1])) / 2

    # In image coords y increases downward — nose below shoulders means face-down
    return nose_y > shoulder_mid_y


def check_zone_violation(
    centroid: Point,
    polygon: list[Point],
    mode: str,  # "inside" | "outside"
) -> bool:
    """Return True when the centroid violates the zone rule."""
    inside = point_in_polygon(centroid, polygon)
    return (mode == "inside" and not inside) or (mode == "outside" and inside)
