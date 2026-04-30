"use client";

import type { ZoneMode } from "@/hooks/useSafeZone";

type EditorState = "idle" | "drawing" | "editing";

interface Props {
  editorState: EditorState;
  mode: ZoneMode;
  isSaving: boolean;
  vertexCount: number;
  onStartDrawing: () => void;
  onCancel: () => void;
  onClear: () => void;
  onModeChange: (m: ZoneMode) => void;
  onSave: () => void;
}

export default function PolygonToolbar({
  editorState,
  mode,
  isSaving,
  vertexCount,
  onStartDrawing,
  onCancel,
  onClear,
  onModeChange,
  onSave,
}: Props) {
  return (
    <div
      className="rounded-xl px-4 py-3 flex flex-wrap items-center gap-2 min-h-[52px]"
      style={{
        backgroundColor: "var(--color-surface)",
        border: "1px solid var(--color-border)",
      }}
    >
      {editorState === "idle" && (
        <Btn onClick={onStartDrawing} variant="primary">
          + 안전 구역 그리기
        </Btn>
      )}

      {editorState === "drawing" && (
        <>
          <span className="text-sm" style={{ color: "var(--color-text-secondary)" }}>
            {vertexCount === 0
              ? "화면을 클릭하여 구역 꼭짓점을 추가하세요"
              : vertexCount < 3
              ? `꼭짓점 ${vertexCount}개 — 최소 3개 필요`
              : "첫 점 클릭 또는 더블클릭으로 완료"}
          </span>
          <div className="ml-auto flex gap-2">
            <Btn onClick={onCancel} variant="ghost">취소</Btn>
          </div>
        </>
      )}

      {editorState === "editing" && (
        <>
          <ZoneModeToggle value={mode} onChange={onModeChange} />
          <div className="ml-auto flex gap-2">
            <Btn onClick={onStartDrawing} variant="ghost">다시 그리기</Btn>
            <Btn onClick={onClear} variant="ghost" danger>삭제</Btn>
            <Btn onClick={onSave} variant="primary" disabled={isSaving}>
              {isSaving ? "저장 중…" : "저장"}
            </Btn>
          </div>
        </>
      )}
    </div>
  );
}

function ZoneModeToggle({
  value,
  onChange,
}: {
  value: ZoneMode;
  onChange: (m: ZoneMode) => void;
}) {
  return (
    <div
      className="flex items-center gap-0.5 rounded-lg p-0.5"
      style={{ backgroundColor: "var(--color-border)" }}
    >
      {(["inside", "outside"] as ZoneMode[]).map((m) => (
        <button
          key={m}
          onClick={() => onChange(m)}
          className="px-3 py-1 text-sm rounded-md font-medium transition-colors"
          style={{
            backgroundColor: value === m ? "var(--color-primary)" : "transparent",
            color: value === m ? "white" : "var(--color-text-secondary)",
          }}
        >
          {m === "inside" ? "안쪽 유지" : "바깥쪽 유지"}
        </button>
      ))}
    </div>
  );
}

function Btn({
  children,
  onClick,
  variant,
  danger,
  disabled,
}: {
  children: React.ReactNode;
  onClick: () => void;
  variant: "primary" | "ghost";
  danger?: boolean;
  disabled?: boolean;
}) {
  return (
    <button
      className="px-3 py-1.5 text-sm rounded-lg font-medium transition-opacity disabled:opacity-50 hover:opacity-85"
      style={
        variant === "primary"
          ? { backgroundColor: "var(--color-primary)", color: "white" }
          : danger
          ? { color: "var(--color-danger)" }
          : { backgroundColor: "var(--color-border)", color: "var(--color-text-secondary)" }
      }
      onClick={onClick}
      disabled={disabled}
    >
      {children}
    </button>
  );
}
