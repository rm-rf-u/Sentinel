"""Verifies geometry.py against shared JSON fixtures — must agree with TypeScript."""

import json
from pathlib import Path

import pytest

from sentinel.inference.geometry import point_in_polygon

FIXTURES = json.loads(
    (Path(__file__).parents[3] / "shared/test-fixtures/geometry.json").read_text()
)

POLYGONS = {
    "square": [tuple(p) for p in FIXTURES["polygon_square"]],
    "triangle": [tuple(p) for p in FIXTURES["polygon_triangle"]],
}


@pytest.mark.parametrize("case", FIXTURES["cases"], ids=[c["label"] for c in FIXTURES["cases"]])
def test_point_in_polygon(case: dict) -> None:
    polygon = POLYGONS[case["polygon"]]
    result = point_in_polygon(tuple(case["point"]), polygon)  # type: ignore[arg-type]
    assert result == case["expected"], f"Failed: {case['label']}"
