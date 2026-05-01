"use client";

import type { SentinelEvent } from "@/hooks/useEvents";

const TYPE_LABELS: Record<string, string> = {
  zone_violation: "안전 구역 이탈",
  prone_position: "엎드린 자세 감지",
  cry_detected: "울음 감지",
};

const TYPE_ICONS: Record<string, string> = {
  zone_violation: "⚠",
  prone_position: "🛌",
  cry_detected: "🔔",
};

const SEVERITY_COLORS: Record<string, string> = {
  info: "var(--color-accent)",
  warning: "var(--color-warning)",
  danger: "var(--color-danger)",
};

const SEVERITY_BG: Record<string, string> = {
  info: "rgba(143,185,150,0.12)",
  warning: "rgba(255,183,3,0.12)",
  danger: "rgba(214,40,40,0.10)",
};

export default function EventCard({ event }: { event: SentinelEvent }) {
  const color = SEVERITY_COLORS[event.severity] ?? "var(--color-text-secondary)";
  const bg = SEVERITY_BG[event.severity] ?? "transparent";
  const label = TYPE_LABELS[event.type] ?? event.type;
  const icon = TYPE_ICONS[event.type] ?? "•";
  const confidence = Math.round(event.confidence * 100);

  return (
    <div
      className="flex items-start gap-3 rounded-xl px-4 py-3"
      style={{
        backgroundColor: "var(--color-surface)",
        border: "1px solid var(--color-border)",
        borderLeft: `4px solid ${color}`,
      }}
    >
      {/* Icon */}
      <span className="text-lg mt-0.5 select-none">{icon}</span>

      {/* Body */}
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2 flex-wrap">
          <span className="font-medium text-sm" style={{ color: "var(--color-text-primary)" }}>
            {label}
          </span>
          <span
            className="text-xs px-1.5 py-0.5 rounded-full font-medium"
            style={{ backgroundColor: bg, color }}
          >
            신뢰도 {confidence}%
          </span>
          {event.details?.zone_mode && (
            <span className="text-xs" style={{ color: "var(--color-text-secondary)" }}>
              {event.details.zone_mode === "inside" ? "안쪽 유지" : "바깥쪽 유지"}
            </span>
          )}
          {event.details?.duration_ms != null && (
            <span className="text-xs" style={{ color: "var(--color-text-secondary)" }}>
              {(event.details.duration_ms / 1000).toFixed(1)}s
            </span>
          )}
        </div>
        <p className="text-xs mt-0.5" style={{ color: "var(--color-text-secondary)" }}>
          {formatTime(event.timestamp)}
        </p>
      </div>
    </div>
  );
}

function formatTime(iso: string): string {
  const d = new Date(iso);
  const date = d.toLocaleDateString("sv-SE"); // YYYY-MM-DD
  const time = d.toLocaleTimeString("ko-KR", {
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false,
  });
  return `${date} ${time}`;
}
