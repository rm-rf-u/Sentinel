import type { Metadata } from "next";
import { NextIntlClientProvider } from "next-intl";
import { getLocale, getMessages } from "next-intl/server";
import "./globals.css";
import NavBar from "@/components/NavBar";
import QueryProvider from "@/components/QueryProvider";
import FcmRegistrar from "@/components/FcmRegistrar";
import { WebRTCProvider } from "@/context/WebRTCContext";
import { EventStreamProvider } from "@/context/EventStreamContext";

export const metadata: Metadata = {
  title: "Sentinel — 아기 안전 모니터",
  description: "실시간 아기 안전 모니터링 시스템",
};

export default async function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const locale = await getLocale();
  const messages = await getMessages();

  return (
    <html lang={locale} className="h-full">
      <body className="h-full flex flex-col">
        <NextIntlClientProvider messages={messages}>
          <QueryProvider>
            <EventStreamProvider>
              <WebRTCProvider>
                <NavBar />
                <FcmRegistrar />
                <main className="flex-1 overflow-auto">{children}</main>
              </WebRTCProvider>
            </EventStreamProvider>
          </QueryProvider>
        </NextIntlClientProvider>
      </body>
    </html>
  );
}
