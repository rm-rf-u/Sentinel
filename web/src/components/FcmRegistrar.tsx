"use client";

import { useFcmRegistration } from "@/hooks/useFcmRegistration";

/** Mounts in the layout to trigger FCM registration silently. Renders nothing. */
export default function FcmRegistrar() {
  useFcmRegistration();
  return null;
}
