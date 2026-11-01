#!/usr/bin/env python3
"""Regenerate the offline Unicode alias map from the pinned MIT gemoji input; no network."""
import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "app/src/main/assets/emoji"
OUTPUT = ROOT / "app/src/main/java/com/parka/mdive/data/document/EmojiAliases.kt"


def generated():
    revision = next(line.removeprefix("Revision: ") for line in (SOURCE / "SOURCE.txt").read_text().splitlines() if line.startswith("Revision: "))
    entries = {alias: item["emoji"] for item in json.loads((SOURCE / "gemoji.json").read_text()) if "emoji" in item for alias in item["aliases"]}
    def quote(value):
        return json.dumps(value, ensure_ascii=False).replace("$", "\\$")
    parts = [list(entries.items())[i:i + 200] for i in range(0, len(entries), 200)]
    return (f"// Generated from GitHub gemoji {revision}; MIT license in assets/emoji/LICENSE.\n"
            "package com.parka.mdive.data.document\n\ninternal object EmojiAliases {\n"
            "    val values: Map<String, String> = buildMap {\n"
            + "".join(f"        part{i}(this)\n" for i in range(len(parts))) + "    }\n"
            + "".join(f"    private fun part{i}(target: MutableMap<String, String>) {{\n"
                      + "".join(f"        target[{quote(k)}] = {quote(v)}\n" for k, v in part) + "    }\n"
                      for i, part in enumerate(parts)) + "}\n")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    text = generated()
    if args.check:
        if OUTPUT.read_text() != text:
            raise SystemExit("Emoji alias map is stale")
        print("Emoji alias map is current")
    else:
        OUTPUT.write_text(text)
