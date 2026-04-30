"use client";

import { useCallback, useEffect, useRef, useState } from "react";

const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8000";

export type ConnectionStatus = "idle" | "connecting" | "connected" | "disconnected";

export function useWebRTC() {
  const [stream, setStream] = useState<MediaStream | null>(null);
  const [status, setStatus] = useState<ConnectionStatus>("idle");
  const pcRef = useRef<RTCPeerConnection | null>(null);

  const connect = useCallback(async () => {
    if (pcRef.current) {
      pcRef.current.close();
    }

    setStatus("connecting");
    setStream(null);

    const pc = new RTCPeerConnection({ iceServers: [] });
    pcRef.current = pc;

    pc.addTransceiver("video", { direction: "recvonly" });

    pc.ontrack = (event) => {
      setStream(event.streams[0] ?? null);
      setStatus("connected");
    };

    pc.oniceconnectionstatechange = () => {
      if (pc.iceConnectionState === "disconnected" || pc.iceConnectionState === "failed") {
        setStatus("disconnected");
      }
    };

    try {
      const offer = await pc.createOffer();
      await pc.setLocalDescription(offer);

      // Wait for ICE gathering to complete before sending (all candidates in SDP)
      await waitForIceGathering(pc);

      const res = await fetch(`${API_BASE}/api/webrtc/offer`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ sdp: pc.localDescription!.sdp, type: "offer" }),
      });

      if (!res.ok) throw new Error(`Signaling failed: ${res.status}`);

      const answer = await res.json();
      await pc.setRemoteDescription(answer);
    } catch (err) {
      console.error("WebRTC connect error:", err);
      setStatus("disconnected");
    }
  }, []);

  const disconnect = useCallback(() => {
    pcRef.current?.close();
    pcRef.current = null;
    setStream(null);
    setStatus("idle");
  }, []);

  useEffect(() => {
    connect();
    return () => {
      pcRef.current?.close();
    };
  }, [connect]);

  return { stream, status, connect, disconnect };
}

function waitForIceGathering(pc: RTCPeerConnection): Promise<void> {
  return new Promise((resolve) => {
    if (pc.iceGatheringState === "complete") {
      resolve();
      return;
    }
    const check = () => {
      if (pc.iceGatheringState === "complete") {
        pc.removeEventListener("icegatheringstatechange", check);
        resolve();
      }
    };
    pc.addEventListener("icegatheringstatechange", check);
    // Fallback: resolve after 5s regardless
    setTimeout(resolve, 5000);
  });
}
