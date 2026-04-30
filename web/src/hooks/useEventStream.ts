"use client";

import { useEffect, useRef, useState } from "react";

const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8000";

export interface SentinelEvent {
  id: string;
  type: "zone_violation" | "prone_position" | "cry_detected";
  severity: "info" | "warning" | "danger";
  timestamp: string;
  confidence: number;
  details?: {
    zone_mode?: "inside" | "outside";
    duration_ms?: number;
  };
}

export function useEventStream(onEvent?: (event: SentinelEvent) => void) {
  const [connected, setConnected] = useState(false);
  const wsRef = useRef<WebSocket | null>(null);
  const onEventRef = useRef(onEvent);
  onEventRef.current = onEvent;

  useEffect(() => {
    const wsBase = API_BASE.replace(/^http/, "ws");
    const ws = new WebSocket(`${wsBase}/ws/events`);
    wsRef.current = ws;

    ws.onopen = () => setConnected(true);
    ws.onclose = () => setConnected(false);
    ws.onerror = () => setConnected(false);

    ws.onmessage = (msg) => {
      try {
        const event = JSON.parse(msg.data) as SentinelEvent;
        onEventRef.current?.(event);
      } catch {
        // ignore malformed messages
      }
    };

    return () => {
      ws.close();
    };
  }, []);

  return { connected };
}
