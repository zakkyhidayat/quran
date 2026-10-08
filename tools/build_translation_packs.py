#!/usr/bin/env python3
"""Bangun paket terjemahan unduhan (build/translation-packs/) dari berkas QUL di data-src/.

Pakai: python tools/build_translation_packs.py [id-paket ...]
Tanpa argumen, semua paket yang sumbernya sudah diunduh dibangun; yang belum diunduh dilewati dengan pesan.
Hasil: satu <id>.db per paket dan catalog.json. Unggah dengan `gh release upload translations ... --clobber`
(lihat docs/DATA_SOURCES.md, bagian "Paket terjemahan unduhan").
"""
import hashlib
import html
import json
import re
import sqlite3
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
# build_db membaca sys.argv saat diimpor; kosongkan agar argumen skrip ini tidak dianggap folder sumber.
_argv, sys.argv = sys.argv, sys.argv[:1]
sys.path.insert(0, str(Path(__file__).resolve().parent))
import build_db  # noqa: E402  (memakai ulang parser catatan kaki)
sys.argv = _argv

SRC = ROOT / "data-src"
OUT = ROOT / "build/translation-packs"
PACK_VERSION = 1
AYAH_TOTAL = 6236

PACKS = [
    # id, bahasa (ISO 639-1), nama tampil, pengarang/sumber, nomor QUL, folder di data-src/
    # Empat yang pertama dulu dibundel di quran.db; id-nya dipertahankan agar pilihan pengguna lama tetap berlaku.
    ("id-kemenag", "id", "Kemenag RI", "Kementerian Agama RI", 224, "quran-id-with-footnote-tags"),
    ("id-sabiq", "id", "The Sabiq Company", "The Sabiq Company", 194, "the-sabiq-company-with-footnote-tags"),
    ("id-kfqpc", "id", "King Fahad Quran Complex", "King Fahad Quran Complex", 173, "king-fahad-quran-complex-with-footnote-tags"),
    ("en-sahih", "en", "Saheeh International", "Saheeh International", 193, "en-sahih-international-with-footnote-tags"),
    ("en-khattab", "en", "The Clear Quran (Khattab)", "Dr. Mustafa Khattab", 426, "en-khattab"),
    ("en-yusufali", "en", "Yusuf Ali", "Abdullah Yusuf Ali", 124, "en-yusufali"),
    ("ur-jalandhari", "ur", "Jalandhari", "Fatah Muhammad Jalandhari", 218, "ur-jalandhari"),
    ("bn-mujibur", "bn", "Mujibur Rahman", "Sheikh Mujibur Rahman", 186, "bn-mujibur"),
    ("tr-diyanet", "tr", "Diyanet", "Diyanet", 148, "tr-diyanet"),
    ("fa-islamhouse", "fa", "IslamHouse.com", "IslamHouse.com", 169, "fa-islamhouse"),
    ("ms-basmeih", "ms", "Basmeih", "Abdul Hameed and Kunhi", 130, "ms-basmeih"),
    ("fr-hamidullah", "fr", "Hamidullah", "Muhammad Hamidullah", 227, "fr-hamidullah"),
    ("ru-kuliev", "ru", "Kuliev", "Elmir Kuliev", 136, "ru-kuliev"),
]

SCHEMA = """
CREATE TABLE info (key TEXT PRIMARY KEY, value TEXT NOT NULL) WITHOUT ROWID;
CREATE TABLE translation_texts (
    surah INTEGER NOT NULL, ayah INTEGER NOT NULL, text TEXT NOT NULL, PRIMARY KEY (surah, ayah)
) WITHOUT ROWID;
CREATE TABLE footnotes (
    surah INTEGER NOT NULL, ayah INTEGER NOT NULL, idx INTEGER NOT NULL, label INTEGER, text TEXT NOT NULL,
    PRIMARY KEY (surah, ayah, idx)
) WITHOUT ROWID;
"""


def find_source(folder):
    """Berkas .db QUL pertama di folder, atau None bila belum diunduh."""
    d = SRC / folder
    return next(iter(sorted(d.glob("*.db"))), None) if d.is_dir() else None


def simple_text(raw):
    """Varian simple: teks polos, kadang terbungkus JSON; buang tag HTML sisa dan rapikan entitas."""
    text = raw
    if text.startswith('"') and text.endswith('"'):
        try:
            text = json.loads(text)
        except ValueError:
            pass
    return html.unescape(re.sub(r"<[^>]+>", "", text)).strip()


def read_rows(pack_id, src):
    """Baca (surah, ayah, teks, [(idx, label, catatan)]) dari varian with-footnote-tags atau simple."""
    con = sqlite3.connect(src)
    columns = {r[1] for r in con.execute("PRAGMA table_info(translation)")}
    with_notes = "footnotes" in columns
    query = "SELECT sura, ayah, text{} FROM translation ORDER BY sura, ayah".format(", footnotes" if with_notes else "")
    stats = {"overrides": 0, "unreferenced": 0}
    rows = []
    for row in con.execute(query):
        surah, ayah, raw_text = row[0], row[1], row[2]
        if with_notes and (raw_text or "").lstrip().startswith('"'):
            text, notes = build_db.parse_translation_row(pack_id, surah, ayah, raw_text, row[3], stats)
        else:
            text, notes = simple_text(raw_text or ""), []
        rows.append((surah, ayah, text, notes))
    con.close()
    return rows, ("with-footnote-tags" if with_notes else "simple"), stats


def build_pack(pack, src):
    pack_id, lang, name, _author, _qul, _folder = pack
    rows, variant, stats = read_rows(pack_id, src)
    keys = {(r[0], r[1]) for r in rows}
    assert len(rows) == AYAH_TOTAL and len(keys) == AYAH_TOTAL, f"{pack_id}: {len(rows)} baris, {len(keys)} ayat unik (harus {AYAH_TOTAL})"
    assert all(r[2] for r in rows), f"{pack_id}: ada ayat tanpa teks"

    OUT.mkdir(parents=True, exist_ok=True)
    out = OUT / f"{pack_id}.db"
    out.unlink(missing_ok=True)
    db = sqlite3.connect(out)
    db.executescript(SCHEMA)
    db.executemany("INSERT INTO info VALUES (?,?)", [("id", pack_id), ("lang", lang), ("name", name), ("version", str(PACK_VERSION))])
    for surah, ayah, text, notes in rows:
        db.execute("INSERT INTO translation_texts VALUES (?,?,?)", (surah, ayah, text))
        db.executemany("INSERT INTO footnotes VALUES (?,?,?,?,?)", [(surah, ayah, i, label, note) for i, label, note in notes])
    count = db.execute("SELECT COUNT(*) FROM translation_texts").fetchone()[0]
    assert count == AYAH_TOTAL
    notes_total = db.execute("SELECT COUNT(*) FROM footnotes").fetchone()[0]
    db.commit()
    db.execute("VACUUM")
    db.close()
    print(f"  {variant}: {count} ayat, {notes_total} catatan kaki, {out.stat().st_size / 1e6:.2f} MB")
    return out


def main():
    wanted = set(sys.argv[1:])
    unknown = wanted - {p[0] for p in PACKS}
    if unknown:
        print("Id paket tidak dikenal:", *sorted(unknown))
        return 2
    entries, skipped = [], []
    for pack in PACKS:
        pack_id, lang, name, author, qul, folder = pack
        if wanted and pack_id not in wanted:
            continue
        src = find_source(folder)
        if src is None:
            print(f"LEWATI {pack_id}: belum diunduh. Simpan berkas .db dari https://qul.tarteel.ai/resources/translation/{qul} ke data-src/{folder}/")
            skipped.append(pack_id)
            continue
        print(f"{pack_id} <- {src.relative_to(ROOT)}")
        out = build_pack(pack, src)
        data = out.read_bytes()
        entries.append({
            "id": pack_id, "lang": lang, "name": name, "source": author, "qul": qul, "file": out.name,
            "bytes": len(data), "sha256": hashlib.sha256(data).hexdigest(), "version": PACK_VERSION,
        })
    if entries:
        # Bangun sebagian tidak boleh menghapus entri paket lain yang sudah ada di katalog.
        catalog = OUT / "catalog.json"
        old = json.loads(catalog.read_text(encoding="utf-8"))["packs"] if wanted and catalog.exists() else []
        merged = {e["id"]: e for e in old if (OUT / e["file"]).exists()}
        merged.update({e["id"]: e for e in entries})
        order = [p[0] for p in PACKS]
        packs = sorted(merged.values(), key=lambda e: order.index(e["id"]) if e["id"] in order else len(order))
        catalog.write_text(json.dumps({"version": 1, "packs": packs}, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        print(f"OK -> {catalog} ({len(packs)} paket)")
    else:
        print("Tidak ada paket yang dibangun.")
    if skipped:
        print(f"{len(skipped)} paket dilewati karena sumber belum ada.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
