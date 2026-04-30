export type Point = [number, number]; // normalized [0,1]
export type Polygon = Point[];

/** Ray-casting point-in-polygon. Matches the Python implementation in backend/sentinel/inference/geometry.py. */
export function pointInPolygon(point: Point, polygon: Polygon): boolean {
  const [px, py] = point;
  let inside = false;
  let j = polygon.length - 1;
  for (let i = 0; i < polygon.length; i++) {
    const [xi, yi] = polygon[i];
    const [xj, yj] = polygon[j];
    if ((yi > py) !== (yj > py) && px < ((xj - xi) * (py - yi)) / (yj - yi) + xi) {
      inside = !inside;
    }
    j = i;
  }
  return inside;
}

export interface VideoRect {
  x: number; // px from canvas left
  y: number; // px from canvas top
  w: number;
  h: number;
}

/**
 * Compute the actual displayed video rectangle inside the container,
 * accounting for object-contain letterboxing.
 */
export function getVideoRect(
  containerW: number,
  containerH: number,
  videoW: number,
  videoH: number
): VideoRect {
  if (videoW === 0 || videoH === 0) {
    return { x: 0, y: 0, w: containerW, h: containerH };
  }
  const containerAspect = containerW / containerH;
  const videoAspect = videoW / videoH;
  if (videoAspect > containerAspect) {
    const h = containerW / videoAspect;
    return { x: 0, y: (containerH - h) / 2, w: containerW, h };
  } else {
    const w = containerH * videoAspect;
    return { x: (containerW - w) / 2, y: 0, w, h: containerH };
  }
}

export function toPixel(norm: Point, rect: VideoRect): Point {
  return [rect.x + norm[0] * rect.w, rect.y + norm[1] * rect.h];
}

export function toNorm(pixel: Point, rect: VideoRect): Point {
  return [
    Math.max(0, Math.min(1, (pixel[0] - rect.x) / rect.w)),
    Math.max(0, Math.min(1, (pixel[1] - rect.y) / rect.h)),
  ];
}

export function distance(a: Point, b: Point): number {
  return Math.hypot(a[0] - b[0], a[1] - b[1]);
}

export function polygonCentroid(polygon: Polygon): Point {
  const x = polygon.reduce((s, p) => s + p[0], 0) / polygon.length;
  const y = polygon.reduce((s, p) => s + p[1], 0) / polygon.length;
  return [x, y];
}
