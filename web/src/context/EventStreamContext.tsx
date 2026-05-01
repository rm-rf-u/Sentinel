"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { type SentinelEvent } from "@/hooks/useEventStream";

export type { SentinelEvent };

type Handler = (event: SentinelEvent) => void;

interface EventStreamContextValue {
  connected: boolean;
  lastEvent: SentinelEvent | null;
  subscribe: (handler: Handler) => () => void;
}

const EventStreamContext = createContext<EventStreamContextValue | null>(null);

const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8000";

export function EventStreamProvider({ children }: { children: ReactNode }) {
  const [connected, setConnected] = useState(false);
  const [lastEvent, setLastEvent] = useState<SentinelEvent | null>(null);
  const handlersRef = useRef<Set<Handler>>(new Set());

  const subscribe = useCallback((handler: Handler) => {
    handlersRef.current.add(handler);
    return () => handlersRef.current.delete(handler);
  }, []);

  useEffect(() => {
    const wsBase = API_BASE.replace(/^http/, "ws");
    const ws = new WebSocket(`${wsBase}/ws/events`);

    ws.onopen = () => setConnected(true);
    ws.onclose = () => setConnected(false);
    ws.onerror = () => setConnected(false);

    ws.onmessage = (msg) => {
      try {
        const event = JSON.parse(msg.data) as SentinelEvent;
        setLastEvent(event);
        handlersRef.current.forEach((h) => h(event));
      } catch {
        // ignore malformed messages
      }
    };

    return () => ws.close();
  }, []);

  return (
    <EventStreamContext.Provider value={{ connected, lastEvent, subscribe }}>
      {children}
    </EventStreamContext.Provider>
  );
}

export function useEventStreamContext(): EventStreamContextValue {
  const ctx = useContext(EventStreamContext);
  if (!ctx) throw new Error("useEventStreamContext must be used within EventStreamProvider");
  return ctx;
}
