"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import type { Polygon } from "@/lib/geometry";

export type ZoneMode = "inside" | "outside";

export interface SafeZone {
  polygon: Polygon;
  mode: ZoneMode;
  updated_at?: string;
}

export function useSafeZone() {
  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery<SafeZone | null>({
    queryKey: ["safe-zone"],
    queryFn: () =>
      api.get<SafeZone>("/api/safe-zone").catch((e: Error) => {
        if (e.message.includes("404")) return null;
        throw e;
      }),
    staleTime: 30_000,
  });

  const mutation = useMutation({
    mutationFn: (zone: SafeZone) => api.put<SafeZone>("/api/safe-zone", zone),
    onSuccess: (saved) => {
      queryClient.setQueryData(["safe-zone"], saved);
    },
  });

  return {
    safeZone: data ?? null,
    isLoading,
    save: mutation.mutate,
    isSaving: mutation.isPending,
    saveError: mutation.error,
  };
}
