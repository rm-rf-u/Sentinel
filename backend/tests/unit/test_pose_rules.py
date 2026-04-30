"""Unit tests for pose_rules: prone detection and zone violation."""

import numpy as np
import pytest

from sentinel.inference.pose_rules import check_prone, check_zone_violation

# ── check_prone ──────────────────────────────────────────────────────────────

def _kp(nose_y: float, ls_y: float, rs_y: float, conf: float = 0.9):
    """Build minimal keypoint arrays (17 joints, zeros for unused)."""
    xy = np.zeros((17, 2), dtype=np.float32)
    conf_arr = np.zeros(17, dtype=np.float32)

    xy[0] = [320, nose_y]    # nose
    xy[5] = [280, ls_y]      # left shoulder
    xy[6] = [360, rs_y]      # right shoulder

    conf_arr[0] = conf
    conf_arr[5] = conf
    conf_arr[6] = conf
    return xy, conf_arr


def test_prone_when_nose_below_shoulders():
    xy, conf = _kp(nose_y=400, ls_y=300, rs_y=300)
    assert check_prone(xy, conf) is True


def test_not_prone_when_nose_above_shoulders():
    xy, conf = _kp(nose_y=200, ls_y=300, rs_y=300)
    assert check_prone(xy, conf) is False


def test_not_prone_at_same_level():
    xy, conf = _kp(nose_y=300, ls_y=300, rs_y=300)
    assert check_prone(xy, conf) is False


def test_prone_rejected_if_low_confidence():
    xy, conf = _kp(nose_y=400, ls_y=300, rs_y=300, conf=0.2)
    assert check_prone(xy, conf) is False


def test_prone_rejected_with_none_input():
    assert check_prone(None, None) is False  # type: ignore[arg-type]


# ── check_zone_violation ─────────────────────────────────────────────────────

SQUARE = [(0.2, 0.2), (0.8, 0.2), (0.8, 0.8), (0.2, 0.8)]


def test_zone_inside_mode_violation_when_outside():
    # inside mode: alert when person is OUTSIDE the polygon
    assert check_zone_violation((0.1, 0.1), SQUARE, "inside") is True


def test_zone_inside_mode_ok_when_inside():
    assert check_zone_violation((0.5, 0.5), SQUARE, "inside") is False


def test_zone_outside_mode_violation_when_inside():
    # outside mode: alert when person is INSIDE the polygon
    assert check_zone_violation((0.5, 0.5), SQUARE, "outside") is True


def test_zone_outside_mode_ok_when_outside():
    assert check_zone_violation((0.1, 0.1), SQUARE, "outside") is False
