"""Geometry helpers shared with the web client.

The point_in_polygon implementation must produce identical results to
web/src/lib/geometry.ts. Verified by shared JSON fixtures in
shared/test-fixtures/geometry.json.
"""

from __future__ import annotations

Point = tuple[float, float]
Polygon = list[Point]


def point_in_polygon(point: Point, polygon: Polygon) -> bool:
    """Ray-casting algorithm. Matches TypeScript implementation."""
    px, py = point
    inside = False
    j = len(polygon) - 1
    for i in range(len(polygon)):
        xi, yi = polygon[i]
        xj, yj = polygon[j]
        if (yi > py) != (yj > py) and px < (xj - xi) * (py - yi) / (yj - yi) + xi:
            inside = not inside
        j = i
    return inside


def normalize(pixel: Point, frame_w: int, frame_h: int) -> Point:
    return (pixel[0] / frame_w, pixel[1] / frame_h)


def denormalize(norm: Point, frame_w: int, frame_h: int) -> Point:
    return (norm[0] * frame_w, norm[1] * frame_h)


def polygon_centroid(polygon: Polygon) -> Point:
    n = len(polygon)
    return (sum(p[0] for p in polygon) / n, sum(p[1] for p in polygon) / n)
