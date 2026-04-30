"use client";

import { useEffect, useRef } from "react";
import { isFirebaseConfigured, getFirebaseApp } from "@/lib/firebase";
import { api } from "@/lib/api";

const VAPID_KEY = process.env.NEXT_PUBLIC_FIREBASE_VAPID_KEY;

export function useFcmRegistration() {
  const attempted = useRef(false);

  useEffect(() => {
    if (attempted.current) return;
    attempted.current = true;
    if (typeof window === "undefined") return;
    if (!isFirebaseConfigured()) return;
    if (!("serviceWorker" in navigator) || !("Notification" in window)) return;
    if (Notification.permission === "denied") return;

    registerFcm();
  }, []);
}

async function registerFcm(): Promise<void> {
  try {
    // Register the service worker
    const registration = await navigator.serviceWorker.register(
      "/firebase-messaging-sw.js",
      { scope: "/" }
    );

    // Request notification permission
    const permission = await Notification.requestPermission();
    if (permission !== "granted") return;

    // Get FCM token using the modular SDK
    const { getMessaging, getToken } = await import("firebase/messaging");
    const messaging = getMessaging(getFirebaseApp());

    const token = await getToken(messaging, {
      vapidKey: VAPID_KEY,
      serviceWorkerRegistration: registration,
    });

    if (!token) return;

    await api.post<{ device_id: string }>("/api/devices/register", {
      fcm_token: token,
    });

    console.info("[Sentinel] FCM push registered");
  } catch (err) {
    // Non-fatal — app works fine without push
    console.warn("[Sentinel] FCM registration skipped:", err);
  }
}
