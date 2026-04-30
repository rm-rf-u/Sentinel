import { describe, it, expect } from "vitest";
import { pointInPolygon, getVideoRect, toPixel, toNorm } from "@/lib/geometry";
import fixtures from "../../../shared/test-fixtures/geometry.json";

const polygons = {
  square: fixtures.polygon_square as [number, number][],
  triangle: fixtures.polygon_triangle as [number, number][],
};

describe("pointInPolygon — shared fixtures", () => {
  for (const c of fixtures.cases) {
    it(c.label, () => {
      const polygon = polygons[c.polygon as keyof typeof polygons];
      expect(pointInPolygon(c.point as [number, number], polygon)).toBe(c.expected);
    });
  }
});

describe("getVideoRect", () => {
  it("no letterbox when aspects match", () => {
    const r = getVideoRect(1280, 720, 1280, 720);
    expect(r).toEqual({ x: 0, y: 0, w: 1280, h: 720 });
  });

  it("letterbox left/right when container is wider", () => {
    const r = getVideoRect(1000, 500, 4, 3);
    expect(r.x).toBeGreaterThan(0);
    expect(r.y).toBe(0);
  });

  it("letterbox top/bottom when container is taller", () => {
    const r = getVideoRect(400, 600, 16, 9);
    expect(r.x).toBe(0);
    expect(r.y).toBeGreaterThan(0);
  });
});

describe("toPixel / toNorm roundtrip", () => {
  it("roundtrip returns original point", () => {
    const rect = { x: 0, y: 0, w: 1280, h: 720 };
    const norm: [number, number] = [0.3, 0.7];
    const pixel = toPixel(norm, rect);
    const back = toNorm(pixel, rect);
    expect(back[0]).toBeCloseTo(norm[0]);
    expect(back[1]).toBeCloseTo(norm[1]);
  });
});
