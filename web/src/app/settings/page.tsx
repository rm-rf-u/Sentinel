"use client";

import { useEffect, useState } from "react";
import { useSettings, type AppSettings } from "@/hooks/useSettings";

export default function SettingsPage() {
  const { settings, isLoading, save, isSaving, isSuccess } = useSettings();
  const [local, setLocal] = useState<AppSettings>(settings);
  const [showSaved, setShowSaved] = useState(false);

  // Sync when server data loads
  useEffect(() => {
    setLocal(settings);
  }, [settings]);

  useEffect(() => {
    if (isSuccess) {
      setShowSaved(true);
      const t = setTimeout(() => setShowSaved(false), 2000);
      return () => clearTimeout(t);
    }
  }, [isSuccess]);

  if (isLoading) {
    return (
      <div className="max-w-2xl mx-auto p-4 flex justify-center py-12">
        <Spinner />
      </div>
    );
  }

  function patch<K extends keyof AppSettings>(key: K, value: AppSettings[K]) {
    setLocal((prev) => ({ ...prev, [key]: value }));
  }

  function patchSensitivity(key: keyof AppSettings["sensitivity"], value: number) {
    setLocal((prev) => ({
      ...prev,
      sensitivity: { ...prev.sensitivity, [key]: value },
    }));
  }

  function patchQuiet(key: keyof AppSettings["quiet_hours"], value: string | boolean) {
    setLocal((prev) => ({
      ...prev,
      quiet_hours: { ...prev.quiet_hours, [key]: value },
    }));
  }

  return (
    <div className="max-w-2xl mx-auto p-4 flex flex-col gap-4">
      <h1 className="text-xl font-semibold" style={{ color: "var(--color-text-primary)" }}>
        설정
      </h1>

      {/* Sensitivity */}
      <Section title="감도 설정">
        <SliderRow
          label="구역 이탈 판정 시간"
          unit="초"
          value={local.sensitivity.zone_violation_seconds}
          min={0.5} max={30} step={0.5}
          onChange={(v) => patchSensitivity("zone_violation_seconds", v)}
        />
        <SliderRow
          label="엎드린 자세 판정 시간"
          unit="초"
          value={local.sensitivity.prone_position_seconds}
          min={1} max={60} step={1}
          onChange={(v) => patchSensitivity("prone_position_seconds", v)}
        />
        <SliderRow
          label="울음 감지 임계값"
          value={local.sensitivity.cry_score_threshold}
          min={0.1} max={1.0} step={0.05}
          display={(v) => `${Math.round(v * 100)}%`}
          onChange={(v) => patchSensitivity("cry_score_threshold", v)}
        />
        <SliderRow
          label="울음 감지 윈도우"
          unit="초"
          value={local.sensitivity.cry_window_seconds}
          min={1} max={10} step={0.5}
          onChange={(v) => patchSensitivity("cry_window_seconds", v)}
        />
      </Section>

      {/* Quiet hours */}
      <Section title="방해 금지 시간">
        <ToggleRow
          label="방해 금지 시간 사용"
          value={local.quiet_hours.enabled}
          onChange={(v) => patchQuiet("enabled", v)}
        />
        {local.quiet_hours.enabled && (
          <div className="flex items-center gap-4 pt-1">
            <TimeField
              label="시작"
              value={local.quiet_hours.start}
              onChange={(v) => patchQuiet("start", v)}
            />
            <span style={{ color: "var(--color-text-secondary)" }}>→</span>
            <TimeField
              label="종료"
              value={local.quiet_hours.end}
              onChange={(v) => patchQuiet("end", v)}
            />
          </div>
        )}
      </Section>

      {/* Notifications */}
      <Section title="알림">
        <ToggleRow
          label="푸시 알림 사용"
          value={local.notifications.fcm_enabled}
          onChange={(v) => patch("notifications", { fcm_enabled: v })}
        />
      </Section>

      {/* Save */}
      <div className="flex items-center gap-3 pt-1">
        <button
          onClick={() => save(local)}
          disabled={isSaving}
          className="px-6 py-2 rounded-xl text-sm font-medium text-white transition-opacity disabled:opacity-50 hover:opacity-85"
          style={{ backgroundColor: "var(--color-primary)" }}
        >
          {isSaving ? "저장 중…" : "저장"}
        </button>
        {showSaved && (
          <span className="text-sm font-medium" style={{ color: "var(--color-accent)" }}>
            저장되었습니다 ✓
          </span>
        )}
        <button
          onClick={() => setLocal(settings)}
          className="ml-auto text-sm"
          style={{ color: "var(--color-text-secondary)" }}
        >
          초기화
        </button>
      </div>
    </div>
  );
}

// ── Sub-components ─────────────────────────────────────────────────────────

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div
      className="rounded-xl p-4 flex flex-col gap-4"
      style={{
        backgroundColor: "var(--color-surface)",
        border: "1px solid var(--color-border)",
      }}
    >
      <h2 className="text-sm font-semibold" style={{ color: "var(--color-text-secondary)" }}>
        {title}
      </h2>
      {children}
    </div>
  );
}

function SliderRow({
  label,
  unit,
  value,
  min,
  max,
  step,
  display,
  onChange,
}: {
  label: string;
  unit?: string;
  value: number;
  min: number;
  max: number;
  step: number;
  display?: (v: number) => string;
  onChange: (v: number) => void;
}) {
  const formatted = display ? display(value) : unit ? `${value}${unit}` : `${value}`;
  return (
    <div className="flex flex-col gap-1.5">
      <div className="flex justify-between items-center">
        <label className="text-sm" style={{ color: "var(--color-text-primary)" }}>
          {label}
        </label>
        <span
          className="text-sm font-medium tabular-nums"
          style={{ color: "var(--color-primary-deep)" }}
        >
          {formatted}
        </span>
      </div>
      <input
        type="range"
        min={min}
        max={max}
        step={step}
        value={value}
        onChange={(e) => onChange(parseFloat(e.target.value))}
        className="w-full h-1.5 rounded-full appearance-none cursor-pointer"
        style={{
          accentColor: "var(--color-primary)",
          background: `linear-gradient(to right, var(--color-primary) ${((value - min) / (max - min)) * 100}%, var(--color-border) 0%)`,
        }}
      />
    </div>
  );
}

function ToggleRow({
  label,
  value,
  onChange,
}: {
  label: string;
  value: boolean;
  onChange: (v: boolean) => void;
}) {
  return (
    <div className="flex items-center justify-between">
      <span className="text-sm" style={{ color: "var(--color-text-primary)" }}>
        {label}
      </span>
      <button
        role="switch"
        aria-checked={value}
        onClick={() => onChange(!value)}
        className="relative w-11 h-6 rounded-full transition-colors"
        style={{
          backgroundColor: value ? "var(--color-primary)" : "var(--color-border)",
        }}
      >
        <span
          className="absolute top-0.5 left-0.5 w-5 h-5 rounded-full bg-white transition-transform shadow-sm"
          style={{ transform: value ? "translateX(20px)" : "translateX(0)" }}
        />
      </button>
    </div>
  );
}

function TimeField({
  label,
  value,
  onChange,
}: {
  label: string;
  value: string;
  onChange: (v: string) => void;
}) {
  return (
    <div className="flex flex-col gap-1">
      <label className="text-xs" style={{ color: "var(--color-text-secondary)" }}>
        {label}
      </label>
      <input
        type="time"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="px-2 py-1.5 rounded-lg text-sm border outline-none"
        style={{
          borderColor: "var(--color-border)",
          color: "var(--color-text-primary)",
          backgroundColor: "var(--color-bg)",
        }}
      />
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
