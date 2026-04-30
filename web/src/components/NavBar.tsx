"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useTranslations } from "next-intl";

export default function NavBar() {
  const t = useTranslations("nav");
  const pathname = usePathname();

  const links = [
    { href: "/", label: t("liveView") },
    { href: "/events", label: t("events") },
    { href: "/settings", label: t("settings") },
  ];

  return (
    <header
      style={{ backgroundColor: "var(--color-surface)", borderBottom: "1px solid var(--color-border)" }}
      className="sticky top-0 z-10 shadow-sm"
    >
      <div className="max-w-5xl mx-auto px-4 flex items-center gap-1 h-14">
        <span className="font-semibold text-lg mr-4" style={{ color: "var(--color-primary-deep)" }}>
          Sentinel
        </span>
        {links.map(({ href, label }) => {
          const active = pathname === href;
          return (
            <Link
              key={href}
              href={href}
              className="px-3 py-1.5 rounded-md text-sm font-medium transition-colors"
              style={{
                backgroundColor: active ? "var(--color-primary)" : "transparent",
                color: active ? "white" : "var(--color-text-secondary)",
              }}
            >
              {label}
            </Link>
          );
        })}
      </div>
    </header>
  );
}
