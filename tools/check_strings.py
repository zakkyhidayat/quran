#!/usr/bin/env python3
"""Check the interface translations in app/src/main/res/values-*/strings.xml.

English (values/strings.xml) is the reference. For every other language this reports:
  - missing keys (Android falls back to English for those)
  - keys that no longer exist in English (safe to delete)
  - placeholder mismatches (%1$s, %2$d ...), which crash or garble the text at runtime
  - unescaped apostrophes, which break the build
It also checks that every language folder is listed in AppLanguage.supported and res/xml/locales_config.xml.

Usage:
  python tools/check_strings.py           # report; exit 1 when something must be fixed
  python tools/check_strings.py --missing  # also list every missing key per language

See docs/TRANSLATING.md.
"""
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res"
APP_LANGUAGE = ROOT / "app/src/main/java/io/zakkyhidayat/quran/AppLanguage.kt"
LOCALES_CONFIG = RES / "xml/locales_config.xml"

# Android resource folders use the legacy code "in" for Indonesian; everywhere else it is "id".
FOLDER_TO_CODE = {"in": "id"}


def load(path):
    """name -> raw text of each translatable <string> (raw, so escapes stay visible)."""
    text = path.read_text(encoding="utf-8")
    out = {}
    for m in re.finditer(r'<string name="([^"]+)"([^>]*)>(.*?)</string>', text, re.S):
        if 'translatable="false"' not in m.group(2):
            out[m.group(1)] = m.group(3)
    ET.fromstring(text)  # fails loudly on malformed XML
    return out


def placeholders(text):
    return sorted(re.findall(r"%\d+\$[sd]", text))


def bad_apostrophes(text):
    return [m.start() for m in re.finditer(r"(?<!\\)'", text)]


def main():
    show_missing = "--missing" in sys.argv
    base = load(RES / "values/strings.xml")
    folders = sorted(p for p in RES.glob("values-*/strings.xml"))
    problems = 0

    for name, text in base.items():
        if bad_apostrophes(text):
            print(f"[en] {name}: unescaped apostrophe (write \\' or ’)")
            problems += 1

    codes = []
    for path in folders:
        folder = path.parent.name.removeprefix("values-")
        code = FOLDER_TO_CODE.get(folder, folder)
        codes.append(code)
        strings = load(path)
        missing = [k for k in base if k not in strings]
        extra = [k for k in strings if k not in base]
        mismatch = [k for k in strings if k in base and placeholders(strings[k]) != placeholders(base[k])]
        quotes = [k for k, v in strings.items() if bad_apostrophes(v)]
        status = "ok" if not (missing or extra or mismatch or quotes) else ""
        print(f"[{code}] {len(strings)}/{len(base)} translated {status}".rstrip())
        if missing:
            print(f"    {len(missing)} missing (English shown instead)" + (":" if show_missing else ""))
            if show_missing:
                print("      " + "\n      ".join(missing))
        for k in extra:
            print(f"    no longer used: {k}")
        for k in mismatch:
            print(f"    placeholder mismatch: {k}: {placeholders(strings[k])} vs English {placeholders(base[k])}")
            problems += 1
        for k in quotes:
            print(f"    unescaped apostrophe: {k} (write \\' or ’)")
            problems += 1

    supported = re.search(r"val supported = listOf\(([^)]*)\)", APP_LANGUAGE.read_text(encoding="utf-8"))
    supported = set(re.findall(r'"([a-z]+)"', supported.group(1))) if supported else set()
    configured = set(re.findall(r'android:name="([a-zA-Z-]+)"', LOCALES_CONFIG.read_text(encoding="utf-8")))
    expected = set(codes) | {"en"}
    for label, found in (("AppLanguage.supported", supported), ("locales_config.xml", configured)):
        for code in sorted(expected - found):
            print(f"{label}: missing \"{code}\" (it has a strings.xml)")
            problems += 1
        for code in sorted(found - expected):
            print(f"{label}: lists \"{code}\" but there is no strings.xml for it")
            problems += 1

    if problems:
        print(f"\n{problems} problem(s) to fix.")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
