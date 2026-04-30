#!/usr/bin/env tsx
/**
 * Generates TypeScript types from shared/schemas/*.schema.json
 * Run: pnpm gen:types
 */
import { compile } from "json-schema-to-typescript";
import { readFileSync, writeFileSync, readdirSync } from "fs";
import { join, resolve } from "path";

const schemasDir = resolve(__dirname, "../../shared/schemas");
const outFile = resolve(__dirname, "../src/types/generated.ts");

const files = readdirSync(schemasDir).filter((f) => f.endsWith(".schema.json"));

let combined = "/* AUTO-GENERATED — do not edit by hand. Run `pnpm gen:types` to regenerate. */\n\n";

for (const file of files) {
  const schema = JSON.parse(readFileSync(join(schemasDir, file), "utf-8"));
  const ts = await compile(schema, schema.title ?? file, {
    bannerComment: "",
    additionalProperties: false,
  });
  combined += ts + "\n";
}

writeFileSync(outFile, combined);
console.log(`Generated types → ${outFile}`);
