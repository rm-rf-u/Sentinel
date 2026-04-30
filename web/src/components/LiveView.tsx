"use client";

import { useEffect, useRef, useState } from "react";
import { useTranslations } from "next-intl";
import { useWebRTC, type ConnectionStatus } from "@/hooks/useWebRTC";
import { useEventStream } from "@/hooks/useEventStream";
import { useSafeZone, type ZoneMode } from "@/hooks/useSafeZone";
import PolygonCanvas from "@/components/PolygonCanvas";
import PolygonToolbar from "@/components/PolygonToolbar";
import type { Polygon } from "@/lib/geometry";

export default function LiveView() {
  const t = useTranslations("liveView");
  const videoRef = useRef<HTMLVideoElement>(null);
  const { stream, status, connect } = useWebRTC();
  const { safeZone, isLoading: zoneLoading, save, isSaving } = useSafeZone();
  useEventStream();

  // Polygon editor state lifted here so toolbar (below video) and canvas (inside video) share it
  const [editorState, setEditorState] = useState<"idle" | "drawing" | "editing">(
    () => (safeZone?.polygon ? "editing" : "idle")
  );
  const [vertices, setVertices] = useState<Polygon>(safeZone?.polygon ?? []);
  const [mode, setMode] = useState<ZoneMode>(safeZone?.mode ?? "inside");

  // Sync when zone loads from server
  useEffect(() => {
    if (safeZone?.polygon) {
      setVertices(safeZone.polygon);
      setMode(safeZone.mode);
      setEditorState("editing");
    }
  }, [safeZone]);

  useEffect(() => {
    if (videoRef.current && stream) {
      videoRef.current.srcObject = stream;
    }
  }, [stream]);

  function handleSave() {
    if (vertices.length < 3) return;
    save({ polygon: vertices, mode });
  }

  return (
    <div className="flex flex-col gap-4 p-4 max-w-5xl mx-auto">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold" style={{ color: "var(--color-text-primary)" }}>
          {t("title")}
        </h1>
        <div className="flex items-center gap-3">
          <StatusBadge status={status} t={t} />
          {status === "disconnected" && (
            <button
              onClick={connect}
              className="px-3 py-1 text-sm rounded-md font-medium text-white transition-opacity hover:opacity-80"
              style={{ backgroundColor: "var(--color-primary)" }}
            >
              재연결
            </button>
          )}
        </div>
      </div>

      {/* Video + canvas overlay */}
      <div
        className="relative w-full rounded-xl overflow-hidden shadow-md"
        style={{ backgroundColor: "#1a1a1a", aspectRatio: "16/9" }}
      >
        <video
          ref={videoRef}
          autoPlay
          playsInline
          muted
          className="w-full h-full object-contain"
        />

        {status !== "connected" && (
          <div className="absolute inset-0 flex flex-col items-center justify-center gap-2 pointer-events-none">
            {status === "connecting" && (
              <>
                <Spinner />
                <p className="text-white/70 text-sm">{t("connecting")}</p>
              </>
            )}
            {(status === "disconnected" || status === "idle") && (
              <p className="text-white/50 text-sm">{t("disconnected")}</p>
            )}
          </div>
        )}

        <PolygonCanvas
          videoRef={videoRef}
          editorState={editorState}
          vertices={vertices}
          onVerticesChange={setVertices}
          onStateChange={setEditorState}
        />
      </div>

      {/* Toolbar below the video */}
      <PolygonToolbar
        editorState={editorState}
        mode={mode}
        isSaving={isSaving}
        vertexCount={vertices.length}
        onStartDrawing={() => {
          setVertices([]);
          setEditorState("drawing");
        }}
        onCancel={() => setEditorState(vertices.length >= 3 ? "editing" : "idle")}
        onClear={() => {
          setVertices([]);
          setEditorState("idle");
        }}
        onModeChange={setMode}
        onSave={handleSave}
      />
    </div>
  );
}

function StatusBadge({
  status,
  t,
}: {
  status: ConnectionStatus;
  t: ReturnType<typeof useTranslations>;
}) {
  const map: Record<ConnectionStatus, { dot: string; label: string }> = {
    idle: { dot: "var(--color-text-secondary)", label: t("disconnected") },
    connecting: { dot: "var(--color-warning)", label: t("connecting") },
    connected: { dot: "var(--color-accent)", label: t("connected") },
    disconnected: { dot: "var(--color-danger)", label: t("disconnected") },
  };
  const { dot, label } = map[status];
  return (
    <div className="flex items-center gap-1.5">
      <span
        className="w-2 h-2 rounded-full"
        style={{
          backgroundColor: dot,
          boxShadow: status === "connected" ? `0 0 6px ${dot}` : "none",
        }}
      />
      <span className="text-sm" style={{ color: "var(--color-text-secondary)" }}>
        {label}
      </span>
    </div>
  );
}

function Spinner() {
  return (
    <div
      className="w-8 h-8 rounded-full border-2 animate-spin"
      style={{ borderColor: "rgba(255,255,255,0.3)", borderTopColor: "var(--color-primary)" }}
    />
  );
}
