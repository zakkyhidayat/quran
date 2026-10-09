#!/usr/bin/env python3
"""Build app/src/main/assets/collections.json from QuranApp's curated verse collections.

Source: https://github.com/AlfaazPlus/QuranApp (GPL-3.0), folder app/src/main/assets/verses/:
  type0       Solutions from the Quran (ayahs for a situation: anxiety, grief, ...)
  type1       Duas in the Quran (item 1, "prayers of the prophets", points to a separate dataset and is skipped)
  type2       Etiquette in the Quran
  major_sins  Major sins in the Quran
Each folder has map.json (item id -> "2:153,3:173,94:5-6") and <lang>/<folder>.json (item id -> title, or
{"title", "description"}). Only the references and titles are copied; ayah text comes from quran.db.

Usage:
  git clone --depth 1 https://github.com/AlfaazPlus/QuranApp.git data-src/QuranApp
  python tools/build_collections.py [data-src/QuranApp]
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "app/src/main/assets/collections.json"
COLLECTIONS = [("dua", "type1"), ("solution", "type0"), ("etiquette", "type2"), ("major_sins", "major_sins")]
# App language -> QuranApp folder. Malay has no folder; the app falls back to English.
LANGS = {"en": "en", "id": "id", "ar": "ar", "ur": "ur", "bn": "bn", "tr": "tr", "fa": "fa", "fr": "fr", "ru": "ru"}
REF = re.compile(r"^\d{1,3}:\d{1,3}(-\d{1,3})?$")


def main():
    src = Path(sys.argv[1] if len(sys.argv) > 1 else ROOT / "data-src/QuranApp") / "app/src/main/assets/verses"
    out = {}
    for key, folder in COLLECTIONS:
        refs = json.loads((src / folder / "map.json").read_text(encoding="utf-8"))
        titles = {lang: json.loads((src / folder / qa / f"{folder}.json").read_text(encoding="utf-8")) for lang, qa in LANGS.items()}
        items = []
        for item_id in sorted(refs, key=int):
            parts = [p.strip() for p in refs[item_id].split(",")]
            if not all(REF.match(p) for p in parts):
                print(f"skip {folder}/{item_id}: {refs[item_id]}")
                continue
            item = {"refs": ",".join(parts), "title": {}, "description": {}}
            for lang, table in titles.items():
                value = table.get(item_id)
                if isinstance(value, dict):
                    item["title"][lang] = value["title"].strip()
                    if value.get("description"):
                        item["description"][lang] = value["description"].strip()
                elif value:
                    item["title"][lang] = value.strip()
            if not item["description"]:
                del item["description"]
            items.append(item)
        out[key] = items
        print(f"{key}: {len(items)} items")
    OUT.write_text(json.dumps(out, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    print(f"wrote {OUT.relative_to(ROOT)} ({OUT.stat().st_size // 1024} KB)")


if __name__ == "__main__":
    main()
