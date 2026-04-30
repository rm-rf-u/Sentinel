"use client";

import { useState } from "react";
import { useEvents, type EventTypeFilter } from "@/hooks/useEvents";
import EventCard from "@/components/EventCard";

const FILTERS: { value: EventTypeFilter; label: string }[] = [
  { value: "all", label: "전체" },
  { value: "zone_violation", label: "구역 이탈" },
  { value: "prone_position", label: "엎드린 자세" },
  { value: "cry_detected", label: "울음" },
];

export default function EventsPage() {
  const [filter, setFilter] = useState<EventTypeFilter>("all");
  const { events, isLoading, loadMore, hasMore } = useEvents(filter);

  return (
    <div className="max-w-5xl mx-auto p-4 flex flex-col gap-4">
      {/* Header */}
      <div className="flex items-center justify-between flex-wrap gap-2">
        <h1 className="text-xl font-semibold" style={{ color: "var(--color-text-primary)" }}>
          이벤트 로그
        </h1>
        <div
          className="flex items-center gap-0.5 rounded-lg p-0.5"
          style={{ backgroundColor: "var(--color-border)" }}
        >
          {FILTERS.map(({ value, label }) => (
            <button
              key={value}
              onClick={() => setFilter(value)}
              className="px-3 py-1 text-sm rounded-md font-medium transition-colors"
              style={{
                backgroundColor: filter === value ? "var(--color-primary)" : "transparent",
                color: filter === value ? "white" : "var(--color-text-secondary)",
              }}
            >
              {label}
            </button>
          ))}
        </div>
      </div>

      {/* Event list */}
      {isLoading ? (
        <div className="flex justify-center py-12">
          <Spinner />
        </div>
      ) : events.length === 0 ? (
        <EmptyState filter={filter} />
      ) : (
        <div className="flex flex-col gap-2">
          {events.map((event) => (
            <EventCard key={event.id} event={event} />
          ))}

          {hasMore && (
            <button
              onClick={loadMore}
              className="mt-2 py-2 text-sm rounded-xl font-medium transition-opacity hover:opacity-75"
              style={{
                backgroundColor: "var(--color-surface)",
                border: "1px solid var(--color-border)",
                color: "var(--color-text-secondary)",
              }}
            >
              더 보기
            </button>
          )}
        </div>
      )}
    </div>
  );
}

function EmptyState({ filter }: { filter: EventTypeFilter }) {
  const messages: Record<EventTypeFilter, string> = {
    all: "아직 이벤트가 없습니다",
    zone_violation: "구역 이탈 이벤트가 없습니다",
    prone_position: "엎드린 자세 이벤트가 없습니다",
    cry_detected: "울음 이벤트가 없습니다",
  };
  return (
    <div
      className="flex flex-col items-center justify-center py-16 rounded-xl gap-2"
      style={{ backgroundColor: "var(--color-surface)", border: "1px solid var(--color-border)" }}
    >
      <span className="text-3xl">🔍</span>
      <p className="text-sm" style={{ color: "var(--color-text-secondary)" }}>
        {messages[filter]}
      </p>
    </div>
  );
}

function Spinner() {
  return (
    <div
      className="w-8 h-8 rounded-full border-2 animate-spin"
      style={{
        borderColor: "var(--color-border)",
        borderTopColor: "var(--color-primary)",
      }}
    />
  );
}
