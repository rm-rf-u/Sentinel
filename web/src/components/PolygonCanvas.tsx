"use client";

import { useCallback, useLayoutEffect, useRef } from "react";
import {
  distance,
  getVideoRect,
  pointInPolygon,
  toNorm,
  toPixel,
  type Point,
  type Polygon,
  type VideoRect,
} from "@/lib/geometry";

const HANDLE_RADIUS = 7;
const HIT_RADIUS = 18;
const CLOSE_RADIUS = 22;
const ACCENT = "#8FB996";
const PRIMARY = "#F4A261";

type EditorState = "idle" | "drawing" | "editing";

interface Props {
  videoRef: React.RefObject<HTMLVideoElement | null>;
  editorState: EditorState;
  vertices: Polygon;
  onVerticesChange: (v: Polygon) => void;
  onStateChange: (s: EditorState) => void;
}

export default function PolygonCanvas({
  videoRef,
  editorState,
  vertices,
  onVerticesChange,
  onStateChange,
}: Props) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const cursorRef = useRef<Point | null>(null);
  const dragRef = useRef<
    | { type: "vertex"; index: number }
    | { type: "polygon"; origin: Point; snapshot: Polygon }
    | null
  >(null);
  const videoRectRef = useRef<VideoRect>({ x: 0, y: 0, w: 0, h: 0 });

  // ── Canvas sizing ────────────────────────────────────────────────────────

  function updateVideoRect() {
    const canvas = canvasRef.current;
    const video = videoRef.current;
    if (!canvas || !video) return;
    videoRectRef.current = getVideoRect(
      canvas.clientWidth,
      canvas.clientHeight,
      video.videoWidth || canvas.clientWidth,
      video.videoHeight || canvas.clientHeight
    );
  }

  function redraw(verts = vertices, state = editorState) {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;
    const dpr = window.devicePixelRatio ?? 1;
    const { clientWidth: w, clientHeight: h } = canvas;
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.clearRect(0, 0, w, h);

    if (verts.length === 0) return;
    const rect = videoRectRef.current;
    const px = verts.map((v) => toPixel(v, rect));

    if (state === "editing" && px.length >= 3) {
      ctx.beginPath();
      ctx.moveTo(px[0][0], px[0][1]);
      for (let i = 1; i < px.length; i++) ctx.lineTo(px[i][0], px[i][1]);
      ctx.closePath();
      ctx.fillStyle = "rgba(143,185,150,0.2)";
      ctx.fill();
      ctx.strokeStyle = ACCENT;
      ctx.lineWidth = 2;
      ctx.setLineDash([]);
      ctx.stroke();
    }

    if (state === "drawing") {
      if (px.length >= 2) {
        ctx.beginPath();
        ctx.moveTo(px[0][0], px[0][1]);
        for (let i = 1; i < px.length; i++) ctx.lineTo(px[i][0], px[i][1]);
        ctx.strokeStyle = ACCENT;
        ctx.lineWidth = 2;
        ctx.setLineDash([6, 4]);
        ctx.stroke();
        ctx.setLineDash([]);
      }
      if (cursorRef.current && px.length >= 1) {
        const last = px[px.length - 1];
        ctx.beginPath();
        ctx.moveTo(last[0], last[1]);
        ctx.lineTo(cursorRef.current[0], cursorRef.current[1]);
        ctx.strokeStyle = "rgba(143,185,150,0.5)";
        ctx.lineWidth = 1.5;
        ctx.setLineDash([4, 4]);
        ctx.stroke();
        ctx.setLineDash([]);
      }
    }

    px.forEach((p, i) => {
      const closeable = state === "drawing" && verts.length >= 3 && i === 0;
      ctx.beginPath();
      ctx.arc(p[0], p[1], HANDLE_RADIUS, 0, Math.PI * 2);
      ctx.fillStyle = "white";
      ctx.fill();
      ctx.strokeStyle = closeable ? PRIMARY : ACCENT;
      ctx.lineWidth = closeable ? 3 : 2;
      ctx.stroke();
      if (closeable) {
        ctx.beginPath();
        ctx.arc(p[0], p[1], 3, 0, Math.PI * 2);
        ctx.fillStyle = PRIMARY;
        ctx.fill();
      }
    });
  }

  const resize = useCallback(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const dpr = window.devicePixelRatio ?? 1;
    const { clientWidth: w, clientHeight: h } = canvas;
    canvas.width = w * dpr;
    canvas.height = h * dpr;
    updateVideoRect();
    redraw();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [vertices, editorState]);

  useLayoutEffect(() => {
    resize();
    const ro = new ResizeObserver(resize);
    if (canvasRef.current) ro.observe(canvasRef.current);
    return () => ro.disconnect();
  }, [resize]);

  // Redraw whenever vertices or state change from outside
  useLayoutEffect(() => {
    updateVideoRect();
    redraw(vertices, editorState);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [vertices, editorState]);

  // ── Pointer helpers ──────────────────────────────────────────────────────

  function canvasPoint(e: React.PointerEvent<HTMLCanvasElement>): Point {
    const r = canvasRef.current!.getBoundingClientRect();
    return [e.clientX - r.left, e.clientY - r.top];
  }

  function hitVertex(pt: Point): number {
    const rect = videoRectRef.current;
    return vertices.findIndex((v) => distance(toPixel(v, rect), pt) <= HIT_RADIUS);
  }

  // ── Event handlers ────────────────────────────────────────────────────────

  function onPointerDown(e: React.PointerEvent<HTMLCanvasElement>) {
    if (e.button !== 0) return;
    const pt = canvasPoint(e);
    const rect = videoRectRef.current;

    if (editorState === "drawing") {
      if (vertices.length >= 3 && distance(toPixel(vertices[0], rect), pt) <= CLOSE_RADIUS) {
        onStateChange("editing");
        return;
      }
      onVerticesChange([...vertices, toNorm(pt, rect)]);
      return;
    }

    if (editorState === "editing") {
      const vi = hitVertex(pt);
      if (vi !== -1) {
        dragRef.current = { type: "vertex", index: vi };
        canvasRef.current!.setPointerCapture(e.pointerId);
        return;
      }
      const norm = toNorm(pt, rect);
      if (pointInPolygon(norm, vertices)) {
        dragRef.current = { type: "polygon", origin: pt, snapshot: [...vertices] };
        canvasRef.current!.setPointerCapture(e.pointerId);
      }
    }
  }

  function onPointerMove(e: React.PointerEvent<HTMLCanvasElement>) {
    const pt = canvasPoint(e);
    const rect = videoRectRef.current;

    if (editorState === "drawing") {
      cursorRef.current = pt;
      redraw(vertices, editorState);
      return;
    }

    if (editorState === "editing" && dragRef.current) {
      const drag = dragRef.current;
      if (drag.type === "vertex") {
        const next = [...vertices];
        next[drag.index] = toNorm(pt, rect);
        onVerticesChange(next);
      } else {
        const dx = pt[0] - drag.origin[0];
        const dy = pt[1] - drag.origin[1];
        onVerticesChange(
          drag.snapshot.map(([x, y]) => {
            const [px2, py2] = toPixel([x, y], rect);
            return toNorm([px2 + dx, py2 + dy], rect);
          })
        );
      }
    }
  }

  function onPointerUp() {
    dragRef.current = null;
  }

  function onDoubleClick() {
    if (editorState !== "drawing" || vertices.length < 3) return;
    // The second click of the dblclick already added a duplicate vertex — remove it
    onVerticesChange(vertices.slice(0, -1));
    onStateChange("editing");
  }

  const interactive = editorState !== "idle";

  return (
    <canvas
      ref={canvasRef}
      className="absolute inset-0 w-full h-full"
      style={{
        cursor: editorState === "drawing" ? "crosshair" : "default",
        pointerEvents: interactive ? "auto" : "none",
        borderRadius: "inherit",
      }}
      onPointerDown={onPointerDown}
      onPointerMove={onPointerMove}
      onPointerUp={onPointerUp}
      onDoubleClick={onDoubleClick}
    />
  );
}
