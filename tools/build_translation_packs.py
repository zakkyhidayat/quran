"""Bangun paket terjemahan unduhan (build/translation-packs/) dari hasil pemecahan berkas QUL di data-src/.

Alur: tools/qul_catch.py (tangkap unduhan) -> tools/qul_translation_meta.py (pecah per terjemahan; tambah --footnotes
untuk varian catatan kaki) -> skrip ini. Daftar paket dan metadata (bahasa, nama, pengarang, nomor QUL, varian) ada di
tools/translation_catalog.json; entri yang punya kunci "excluded" (tidak lengkap, ambigu) atau "skip" tidak dibangun.

Pakai: python tools/build_translation_packs.py [id-paket ...]
Tanpa argumen, semua paket di katalog dibangun. Hasil: satu <id>.db per paket dan catalog.json (format yang dibaca aplikasi).
Unggah dengan `gh release upload translations ... --clobber` (lihat docs/DATA_SOURCES.md, bagian "Paket terjemahan unduhan").
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
CATALOG = Path(__file__).resolve().parent / "translation_catalog.json"
SPLIT = {"footnote": SRC / "translation-footnote-split", "simple": SRC / "translation-split"}
PACK_VERSION = 2
AYAH_TOTAL = 6236
MAX_EMPTY = 10  # sumber QUL kadang kosong di beberapa ayat; lebih dari ini paket dianggap tidak layak


def load_catalog():
    """Entri yang dibangun (tanpa excluded/skip), urut seperti di berkas katalog."""
    entries = json.loads(CATALOG.read_text(encoding="utf-8"))
    return entries, [e for e in entries if "excluded" not in e and not e.get("skip")]


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


def find_source(entry):
    """Berkas <qul>-*.db dari folder pecahan sesuai varian, atau None bila belum dipecah."""
    d = SPLIT[entry["variant"]]
    return next(iter(sorted(d.glob(f"{entry['qul']}-*.db"))), None) if d.is_dir() else None


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
    # Kolom sura/ayah rusak di beberapa berkas QUL (mis. 114 ayat unik saja); ayah_key selalu benar.
    query = "SELECT ayah_key, ayah_key, text{} FROM translation".format(", footnotes" if with_notes else "")
    stats = {"overrides": 0, "unreferenced": 0}
    rows = []
    for row in con.execute(query):
        surah, ayah = (int(x) for x in row[0].split(":"))
        raw_text = row[2]
        if with_notes and (raw_text or "").lstrip().startswith('"'):
            text, notes = build_db.parse_translation_row(pack_id, surah, ayah, raw_text, row[3], stats)
            text = re.sub(r"</?a\b[^>]*>", "", text).strip()  # pembungkus <a class="f"> penanda catatan (mis. Piccardo)
        else:
            text, notes = simple_text(raw_text or ""), []
        rows.append((surah, ayah, text, notes))
    con.close()
    rows.sort(key=lambda r: (r[0], r[1]))
    return rows, ("with-footnote-tags" if with_notes else "simple"), stats


def build_pack(pack, src):
    pack_id, lang, name = pack["pack_id"], pack["lang"], pack["name"]
    rows, variant, stats = read_rows(pack_id, src)
    keys = {(r[0], r[1]) for r in rows}
    assert len(rows) == AYAH_TOTAL and len(keys) == AYAH_TOTAL, f"{pack_id}: {len(rows)} baris, {len(keys)} ayat unik (harus {AYAH_TOTAL})"
    empty = sum(1 for r in rows if not r[2])
    assert empty <= MAX_EMPTY, f"{pack_id}: {empty} ayat tanpa teks (batas {MAX_EMPTY})"

    OUT.mkdir(parents=True, exist_ok=True)
    out = OUT / f"{pack_id}.db"
    out.unlink(missing_ok=True)
    db = sqlite3.connect(out)
    db.executescript(SCHEMA)
    info = [("id", pack_id), ("lang", lang), ("name", name), ("version", str(PACK_VERSION))]
    if pack.get("lang_name"):
        info.append(("lang_name", pack["lang_name"]))
    db.executemany("INSERT INTO info VALUES (?,?)", info)
    for surah, ayah, text, notes in rows:
        db.execute("INSERT INTO translation_texts VALUES (?,?,?)", (surah, ayah, text))
        db.executemany("INSERT INTO footnotes VALUES (?,?,?,?,?)", [(surah, ayah, i, label, note) for i, label, note in notes])
    count = db.execute("SELECT COUNT(*) FROM translation_texts").fetchone()[0]
    assert count == AYAH_TOTAL
    notes_total = db.execute("SELECT COUNT(*) FROM footnotes").fetchone()[0]
    db.commit()
    db.execute("VACUUM")
    db.close()
    print(f"  {variant}: {count} ayat, {notes_total} catatan kaki, {out.stat().st_size / 1e6:.2f} MB" + (f", {empty} ayat kosong di sumber" if empty else ""))
    return out, empty


def main():
    wanted = set(sys.argv[1:])
    everything, packs = load_catalog()
    known = {e["pack_id"] for e in packs}
    unknown = wanted - known
    if unknown:
        print("Id paket tidak dikenal (atau dikecualikan di katalog):", *sorted(unknown))
        return 2
    entries, skipped, failed = [], [], []
    for pack in packs:
        pack_id = pack["pack_id"]
        if wanted and pack_id not in wanted:
            continue
        src = find_source(pack)
        if src is None:
            print(f"LEWATI {pack_id}: belum dipecah. Jalankan tools/qul_translation_meta.py (dan --footnotes) setelah mengunduh QUL {pack['qul']}.")
            skipped.append(pack_id)
            continue
        print(f"{pack_id} <- {src.relative_to(ROOT)}")
        try:
            out, empty = build_pack(pack, src)
        except (AssertionError, sqlite3.Error, ValueError) as e:
            print(f"  GAGAL {pack_id}: {e}")
            (OUT / f"{pack_id}.db").unlink(missing_ok=True)
            failed.append(pack_id)
            continue
        data = out.read_bytes()
        entry = {
            "id": pack_id, "lang": pack["lang"], "name": pack["name"], "source": pack["author"], "qul": pack["qul"], "file": out.name,
            "bytes": len(data), "sha256": hashlib.sha256(data).hexdigest(), "version": PACK_VERSION,
        }
        if empty:
            entry["empty_ayah"] = empty
        if pack.get("lang_name"):
            entry["lang_name"] = pack["lang_name"]
        entries.append(entry)
    if entries:
        # Bangun sebagian tidak boleh menghapus entri paket lain yang sudah ada di katalog.
        catalog = OUT / "catalog.json"
        old = json.loads(catalog.read_text(encoding="utf-8"))["packs"] if wanted and catalog.exists() else []
        merged = {e["id"]: e for e in old if (OUT / e["file"]).exists()}
        merged.update({e["id"]: e for e in entries})
        order = [p["pack_id"] for p in packs]
        result = sorted(merged.values(), key=lambda e: order.index(e["id"]) if e["id"] in order else len(order))
        catalog.write_text(json.dumps({"version": 1, "packs": result}, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        total = sum(e["bytes"] for e in result)
        langs = len({e["lang"] for e in result})
        print(f"OK -> {catalog} ({len(result)} paket, {langs} bahasa, {total / 1e6:.1f} MB)")
    else:
        print("Tidak ada paket yang dibangun.")
    if skipped:
        print(f"{len(skipped)} paket dilewati karena sumber belum ada.")
    if failed:
        print(f"{len(failed)} paket GAGAL: {', '.join(failed)}")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
