#!/usr/bin/env python3
"""Bangun paket tafsir (ar-tahrir.db) dari unduhan QUL "ar-tafseer-tahrir-al-tanwir" di data-src/tafsir/.

Tafsir QUL menyimpan satu teks per rentang ayat (kolom from_ayah..to_ayah); ayat lain di dalam rentang itu kosong.
Skrip ini hanya mengambil baris yang berteks, membersihkan markup (pembungkus div, nomor halaman, span), lalu menyimpan
rentang dengan kunci surah*1000+ayah (sama dengan paket yang sudah ada di rilis). Setiap ayat harus tercakup tepat
satu rentang, jika tidak skrip gagal.

Pakai: python tools/build_tafsir_pack.py [berkas-sumber.db]
Hasil: build/translation-packs/ar-tahrir.db dan ar-tahrir.entry.json (satu entri katalog untuk digabung ke catalog.json
di rilis "translations"; lihat docs/DATA_SOURCES.md).
"""
import hashlib
import html
import json
import re
import sqlite3
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DEFAULT_SRC = ROOT / "data-src/tafsir/ar-tafseer-tahrir-al-tanwir.db"
OUT = ROOT / "build/translation-packs"
AYAH_TOTAL = 6236
PACK_ID = "ar-tahrir"
VERSION = 2
INFO = {
    "id": PACK_ID,
    "lang": "ar",
    "name": "التحرير والتنوير",
    "author": "ابن عاشور",
    "kind": "tafsir",
    "version": str(VERSION),
    "source": "QUL ar-tafseer-tahrir-al-tanwir",
}
SCHEMA = """
CREATE TABLE info (key TEXT PRIMARY KEY, value TEXT NOT NULL) WITHOUT ROWID;
CREATE TABLE tafsir (start INTEGER PRIMARY KEY, "end" INTEGER NOT NULL, text TEXT NOT NULL);
"""


def clean(raw: str) -> str:
    """HTML tafsir QUL menjadi teks polos berparagraf (paragraf dipisah baris kosong)."""
    t = re.sub(r"</?div\b[^>]*>", "", raw)
    t = re.sub(r'<p\b[^>]*class="page-num"[^>]*>.*?</p>', "", t, flags=re.S)  # nomor halaman mushaf
    t = re.sub(r"<p>\s*</p>", "", t)
    t = re.sub(r"</p>\s*", "\n\n", t)
    t = re.sub(r"<br\s*/?>", "\n", t)
    t = re.sub(r"<p\b[^>]*>", "", t)
    t = re.sub(r"<[^>]+>", "", t)  # span pembungkus ayat (qpc-hafs, hlt) dan tag lain
    t = html.unescape(t)
    t = t.replace("&gt؛", "؛")  # satu galat sumber (26:69, "&amp;gt؛"): dipetakan ke titik koma Arab
    t = re.sub(r"[ \t ]+", " ", t)
    t = re.sub(r" *\n *", "\n", t)
    t = re.sub(r"\n{3,}", "\n\n", t)
    return t.strip()


def passages(src: Path):
    rows = sqlite3.connect(src).execute("SELECT from_ayah, to_ayah, text FROM tafsir WHERE text != ''").fetchall()
    out, covered = [], set()
    for f, t, raw in rows:
        fs, fa = (int(x) for x in f.split(":"))
        ts, ta = (int(x) for x in t.split(":"))
        assert fs == ts and fa <= ta, f"rentang lintas surah atau terbalik: {f}-{t}"
        for a in range(fa, ta + 1):
            assert (fs, a) not in covered, f"rentang tumpang tindih di {fs}:{a}"
            covered.add((fs, a))
        text = clean(raw)
        assert text, f"teks kosong setelah dibersihkan: {f}"
        assert "<" not in text and "&" not in text, f"markup tersisa di {f}"
        out.append((fs * 1000 + fa, fs * 1000 + ta, text))
    assert len(covered) == AYAH_TOTAL, f"{len(covered)} ayat tercakup (harus {AYAH_TOTAL})"
    return sorted(out)


def main() -> int:
    src = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_SRC
    rows = passages(src)
    OUT.mkdir(parents=True, exist_ok=True)
    out = OUT / f"{PACK_ID}.db"
    out.unlink(missing_ok=True)
    db = sqlite3.connect(out)
    db.executescript(SCHEMA)
    db.executemany("INSERT INTO info VALUES (?,?)", [(k, v) for k, v in INFO.items()])
    db.executemany("INSERT INTO tafsir VALUES (?,?,?)", rows)
    db.commit()
    db.execute("VACUUM")
    db.close()
    data = out.read_bytes()
    entry = {
        "id": PACK_ID,
        "lang": INFO["lang"],
        "name": INFO["name"],
        "source": INFO["author"],
        "kind": "tafsir",
        "file": out.name,
        "bytes": len(data),
        "sha256": hashlib.sha256(data).hexdigest(),
        "version": VERSION,
    }
    (OUT / f"{PACK_ID}.entry.json").write_text(json.dumps(entry, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"{len(rows)} rentang tafsir -> {out} ({len(data) / 1e6:.2f} MB)")
    print(json.dumps(entry, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    sys.exit(main())
