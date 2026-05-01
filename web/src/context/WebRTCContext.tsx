"use client";

import { createContext, useContext, type ReactNode } from "react";
import { useWebRTC, type ConnectionStatus } from "@/hooks/useWebRTC";

export type { ConnectionStatus };

interface WebRTCContextValue {
  stream: MediaStream | null;
  status: ConnectionStatus;
  connect: () => Promise<void>;
  disconnect: () => void;
}

const WebRTCContext = createContext<WebRTCContextValue | null>(null);

export function WebRTCProvider({ children }: { children: ReactNode }) {
  const webrtc = useWebRTC();
  return <WebRTCContext.Provider value={webrtc}>{children}</WebRTCContext.Provider>;
}

export function useWebRTCContext(): WebRTCContextValue {
  const ctx = useContext(WebRTCContext);
  if (!ctx) throw new Error("useWebRTCContext must be used within WebRTCProvider");
  return ctx;
}
