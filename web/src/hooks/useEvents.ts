"use client";

import { useCallback, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { useEventStream, type SentinelEvent } from "@/hooks/useEventStream";

const PAGE_SIZE = 50;

export type { SentinelEvent };

export type EventTypeFilter = "all" | "zone_violation" | "prone_position" | "cry_detected";

export function useEvents(filter: EventTypeFilter = "all") {
  const [extraEvents, setExtraEvents] = useState<SentinelEvent[]>([]);
  const [before, setBefore] = useState<string | undefined>(undefined);
  const [hasMore, setHasMore] = useState(true);

  const { data: initialEvents = [], isLoading } = useQuery<SentinelEvent[]>({
    queryKey: ["events"],
    queryFn: async () => {
      const data = await api.get<SentinelEvent[]>(`/api/events?limit=${PAGE_SIZE}`);
      setHasMore(data.length === PAGE_SIZE);
      if (data.length > 0) setBefore(data[data.length - 1].timestamp);
      return data;
    },
    staleTime: 0,
  });

  // Prepend real-time events from WebSocket
  useEventStream(
    useCallback((event: SentinelEvent) => {
      setExtraEvents((prev) => [event, ...prev]);
    }, [])
  );

  const loadMore = useCallback(async () => {
    if (!before || !hasMore) return;
    const more = await api.get<SentinelEvent[]>(
      `/api/events?limit=${PAGE_SIZE}&before=${encodeURIComponent(before)}`
    );
    setExtraEvents((prev) => [...prev, ...more]);
    setHasMore(more.length === PAGE_SIZE);
    if (more.length > 0) setBefore(more[more.length - 1].timestamp);
  }, [before, hasMore]);

  const allEvents = [...extraEvents, ...initialEvents];
  const filtered =
    filter === "all" ? allEvents : allEvents.filter((e) => e.type === filter);

  return { events: filtered, isLoading, loadMore, hasMore };
}
