"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";

export interface SensitivitySettings {
  zone_violation_seconds: number;
  prone_position_seconds: number;
  cry_score_threshold: number;
  cry_window_seconds: number;
  zone_violation_cooldown_seconds: number;
  prone_position_cooldown_seconds: number;
  cry_detected_cooldown_seconds: number;
}

export interface QuietHours {
  enabled: boolean;
  start: string;
  end: string;
}

export interface AppSettings {
  sensitivity: SensitivitySettings;
  quiet_hours: QuietHours;
  notifications: { fcm_enabled: boolean };
}

export const DEFAULT_SETTINGS: AppSettings = {
  sensitivity: {
    zone_violation_seconds: 2,
    prone_position_seconds: 5,
    cry_score_threshold: 0.35,
    cry_window_seconds: 3,
    zone_violation_cooldown_seconds: 0,
    prone_position_cooldown_seconds: 0,
    cry_detected_cooldown_seconds: 0,
  },
  quiet_hours: { enabled: false, start: "22:00", end: "07:00" },
  notifications: { fcm_enabled: true },
};

export function useSettings() {
  const queryClient = useQueryClient();

  const { data: settings, isLoading } = useQuery<AppSettings>({
    queryKey: ["settings"],
    queryFn: () => api.get<AppSettings>("/api/settings"),
    staleTime: 60_000,
  });

  const mutation = useMutation({
    mutationFn: (s: AppSettings) => api.put<AppSettings>("/api/settings", s),
    onSuccess: (saved) => queryClient.setQueryData(["settings"], saved),
  });

  return {
    settings: settings ?? DEFAULT_SETTINGS,
    isLoading,
    save: mutation.mutate,
    isSaving: mutation.isPending,
    isSuccess: mutation.isSuccess,
  };
}
