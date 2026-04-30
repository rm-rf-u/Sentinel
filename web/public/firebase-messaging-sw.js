/**
 * Firebase Cloud Messaging service worker.
 *
 * Fetches Firebase config from /api/firebase-config (served by Next.js)
 * so we can use NEXT_PUBLIC_ env vars without hardcoding them here.
 *
 * Uses Firebase compat SDK (v10) via importScripts — compatible with FCM v12
 * project configs since the FCM wire protocol hasn't changed.
 */

importScripts("https://www.gstatic.com/firebasejs/10.11.1/firebase-app-compat.js");
importScripts("https://www.gstatic.com/firebasejs/10.11.1/firebase-messaging-compat.js");

let messaging = null;

async function initFirebase() {
  try {
    const res = await fetch("/api/firebase-config");
    const config = await res.json();
    if (!config.apiKey) return; // not configured
    if (!firebase.apps.length) firebase.initializeApp(config);
    messaging = firebase.messaging();
    messaging.onBackgroundMessage((payload) => {
      const notification = payload.notification ?? {};
      const title = notification.title ?? "Sentinel";
      const body = notification.body ?? "";
      const eventId = payload.data?.event_id ?? "sentinel";
      self.registration.showNotification(title, {
        body,
        icon: "/icon-192.png",
        badge: "/icon-192.png",
        tag: eventId,        // deduplicates same-event notifications
        renotify: false,
        data: { url: "/events" },
      });
    });
  } catch (err) {
    console.warn("[Sentinel SW] Firebase init failed:", err);
  }
}

self.addEventListener("install", (event) => {
  event.waitUntil(initFirebase());
});

self.addEventListener("activate", (event) => {
  event.waitUntil(clients.claim());
});

self.addEventListener("notificationclick", (event) => {
  event.notification.close();
  const url = event.notification.data?.url ?? "/events";
  event.waitUntil(
    clients
      .matchAll({ type: "window", includeUncontrolled: true })
      .then((clientList) => {
        const existing = clientList.find((c) => c.url.includes(url));
        if (existing) return existing.focus();
        return clients.openWindow(url);
      })
  );
});
