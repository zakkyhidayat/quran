#!/usr/bin/env python3
"""Integrity checks for the Qur'an content in app/src/main/assets/quran.db.

A single wrong letter or diacritic is a mistake in the text of the Qur'an, so these checks must pass before any release.

  python tools/verify_quran.py              # structure + content fingerprints (runs in CI; no data-src needed)
  python tools/verify_quran.py --source     # also compare every ayah against the original QUL file in data-src/
  python tools/verify_quran.py --write      # record new fingerprints after an intentional data update
  python tools/verify_quran.py --db FILE    # check another copy of the database

What is checked:
  1. 114 surahs and 6,236 ayahs; the ayah count of every surah matches the Kufi count listed below (written here,
     not read from the database being checked).
  2. Every ayah has words on the mushaf pages, exactly one end-of-ayah marker, and 604 pages exist.
  3. Division counts: 30 juz, 60 hizb, 240 rub', 7 manzil, 15 sajdah.
  4. SHA-256 fingerprints of all Arabic text and all page glyph codes equal docs/quran-content.json. Changing one
     character anywhere fails the check.
  5. With --source: the Unicode text of every ayah equals the text in data-src/qpc-hafs/qpc-hafs.db exactly.
"""
import hashlib
import json
import sqlite3
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DB = ROOT / "app/src/main/assets/quran.db"
GOLDEN = ROOT / "docs/quran-content.json"
SOURCE = ROOT / "data-src/qpc-hafs/qpc-hafs.db"

# Ayahs per surah, Kufi count (Hafs 'an 'Asim), surah 1..114.
KUFI = [
    7, 286, 200, 176, 120, 165, 206, 75, 129, 109, 123, 111, 43, 52, 99, 128, 111, 110, 98, 135,
    112, 78, 118, 64, 77, 227, 93, 88, 69, 60, 34, 30, 73, 54, 45, 83, 182, 88, 75, 85,
    54, 53, 89, 59, 37, 35, 38, 29, 18, 45, 60, 49, 62, 55, 78, 96, 29, 22, 24, 13,
    14, 11, 11, 18, 12, 12, 30, 52, 52, 44, 28, 28, 20, 56, 40, 31, 50, 40, 46, 42,
    29, 19, 36, 25, 22, 17, 19, 26, 30, 20, 15, 21, 11, 8, 8, 19, 5, 8, 8, 11,
    11, 8, 3, 9, 5, 4, 7, 3, 6, 3, 5, 4, 5, 6,
]
DIVISIONS = {"juz": 30, "hizb": 60, "rub": 240, "manzil": 7, "sajda": 15}

failures = []


def check(ok, message):
    if not ok:
        failures.append(message)


def fingerprints(db):
    text = hashlib.sha256()
    for surah, ayah, t in db.execute("SELECT surah, ayah, text_ar FROM ayahs ORDER BY surah, ayah"):
        text.update(f"{surah}:{ayah}\t{t}\n".encode("utf-8"))
    glyphs = hashlib.sha256()
    for surah, ayah, pos, t, page, line in db.execute("SELECT surah, ayah, pos, text, page, line FROM words ORDER BY id"):
        glyphs.update(f"{surah}:{ayah}:{pos}\t{t}\t{page}:{line}\n".encode("utf-8"))
    return {"arabic_text_sha256": text.hexdigest(), "page_glyphs_sha256": glyphs.hexdigest()}


def main():
    global DB
    if "--db" in sys.argv:  # periksa salinan lain (dipakai untuk menguji bahwa pemeriksaan ini memang bisa gagal)
        DB = Path(sys.argv[sys.argv.index("--db") + 1])
    assert sum(KUFI) == 6236 and len(KUFI) == 114, "the reference table in this script is wrong"
    db = sqlite3.connect(f"file:{DB}?mode=ro", uri=True)

    # 1. Counts.
    surahs = dict(db.execute("SELECT id, ayah_count FROM surahs ORDER BY id"))
    check(len(surahs) == 114, f"{len(surahs)} surahs instead of 114")
    for n, expected in enumerate(KUFI, 1):
        check(surahs.get(n) == expected, f"surah {n}: ayah_count {surahs.get(n)} instead of {expected}")
    per_surah = dict(db.execute("SELECT surah, COUNT(*) FROM ayahs GROUP BY surah"))
    for n, expected in enumerate(KUFI, 1):
        check(per_surah.get(n) == expected, f"surah {n}: {per_surah.get(n)} ayahs in the ayahs table instead of {expected}")
    total = db.execute("SELECT COUNT(*) FROM ayahs").fetchone()[0]
    check(total == 6236, f"{total} ayahs instead of 6236")

    # 2. Pages and words.
    pages = {r[0] for r in db.execute("SELECT DISTINCT page FROM page_lines")}
    check(pages == set(range(1, 605)), f"pages {min(pages, default=0)}..{max(pages, default=0)} ({len(pages)}) instead of 1..604")
    missing = db.execute("""SELECT COUNT(*) FROM ayahs a WHERE NOT EXISTS
        (SELECT 1 FROM words w WHERE w.surah = a.surah AND w.ayah = a.ayah)""").fetchone()[0]
    check(missing == 0, f"{missing} ayahs have no words on the mushaf pages")
    bad_end = db.execute("""SELECT COUNT(*) FROM (SELECT surah, ayah, SUM(is_end) e FROM words GROUP BY surah, ayah)
        WHERE e != 1""").fetchone()[0]
    check(bad_end == 0, f"{bad_end} ayahs do not have exactly one end-of-ayah marker")

    # 3. Divisions.
    for table, expected in DIVISIONS.items():
        n = db.execute(f"SELECT COUNT(*) FROM {table}").fetchone()[0]
        check(n == expected, f"{table}: {n} rows instead of {expected}")

    # 4. Fingerprints.
    now = fingerprints(db)
    if "--write" in sys.argv:
        if failures:
            print("Not writing fingerprints, structure checks failed:", *failures, sep="\n  ")
            return 1
        GOLDEN.write_text(json.dumps(now, indent=2) + "\n", encoding="utf-8")
        print(f"Fingerprints written to {GOLDEN.relative_to(ROOT)}")
    else:
        golden = json.loads(GOLDEN.read_text(encoding="utf-8")) if GOLDEN.exists() else {}
        for key, value in now.items():
            check(golden.get(key) == value, f"{key} differs from docs/quran-content.json (content changed)")

    # 5. Source comparison.
    if "--source" in sys.argv:
        if not SOURCE.exists():
            failures.append(f"--source: {SOURCE.relative_to(ROOT)} not found")
        else:
            src = dict(((s, a), t) for s, a, t in sqlite3.connect(SOURCE).execute("SELECT surah, ayah, text FROM verses"))
            ours = dict(((s, a), t) for s, a, t in db.execute("SELECT surah, ayah, text_ar FROM ayahs"))
            check(set(src) == set(ours), "ayah keys differ from the source")
            diff = [k for k in ours if src.get(k) != ours[k]]
            check(not diff, f"{len(diff)} ayahs differ from the source text, first: {diff[:5]}")
            if not diff:
                print(f"source: all {len(ours)} ayahs identical to {SOURCE.relative_to(ROOT)}")

    if failures:
        print("FAILED:", *failures, sep="\n  ")
        return 1
    print("OK: 114 surahs, 6236 ayahs (Kufi), 604 pages, divisions and content fingerprints verified")
    return 0


if __name__ == "__main__":
    sys.exit(main())
