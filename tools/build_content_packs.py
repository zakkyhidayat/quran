"""Bangun paket unduhan info surah dan tafsir (build/translation-packs/) dari data-src/surah-info/ dan data-src/tafsir/.

Paket diunggah ke rilis yang sama dengan paket terjemahan (tag `translations`) dan dicatat di catalog.json yang sama, tetapi
di larik terpisah (`surah_info`, `tafsir`) agar aplikasi lama yang hanya membaca `packs` tidak menganggapnya terjemahan.
Isi paket dikompresi gzip (`<id>.db.gz`); sha256 dan bytes di katalog untuk berkas .gz, `size` untuk database setelah
dibuka.

Metadata dikurasi di tools/content_catalog.json: bahasa dari nama berkas dan tag HTML QUL sering keliru (mis. tafsir
As-Sa'di Prancis bertanda `fa`), jadi tidak ditebak. Entri dengan kunci "excluded" tidak dibangun.

Pakai: python tools/build_content_packs.py [surah_info|tafsir] [id-paket ...]
Tanpa argumen, semua paket dibangun. Info surah id dan en tidak dibuat paket karena sudah ada di quran.db.
"""
import gzip
import hashlib
import json
import re
import sqlite3
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "data-src"
OUT = ROOT / "build/translation-packs"
CATALOG = Path(__file__).resolve().parent / "content_catalog.json"
PACK_VERSION = 1
KINDS = ("surah_info", "tafsir")

SCHEMA = {
    "surah_info": """
        CREATE TABLE info (key TEXT PRIMARY KEY, value TEXT NOT NULL) WITHOUT ROWID;
        CREATE TABLE surah_info (
            surah INTEGER PRIMARY KEY, name TEXT NOT NULL, short_text TEXT NOT NULL, text TEXT NOT NULL
        );
    """,
    # start/end = surah * 1000 + ayah, jadi kelompok yang melintasi batas surah tetap bisa dicari dengan satu rentang.
    "tafsir": """
        CREATE TABLE info (key TEXT PRIMARY KEY, value TEXT NOT NULL) WITHOUT ROWID;
        CREATE TABLE tafsir (start INTEGER PRIMARY KEY, "end" INTEGER NOT NULL, text TEXT NOT NULL);
    """,
}


def clean_html(text: str) -> str:
    """Sisakan tag p, h1-h3, strong, em, b, i, br, ol, ul, li, a; buang atribut dan tag lain (span, div, font)."""
    text = re.sub(r"<(/?)(p|h[1-6]|strong|em|b|i|ol|ul|li|br|blockquote|sup)\b[^>]*>", r"<\1\2>", text, flags=re.I)
    text = re.sub(r"<a\b[^>]*href=\"([^\"]*)\"[^>]*>", lambda m: f'<a href="{m.group(1)}">', text)
    text = re.sub(r"<(?!/?(?:p|h[1-6]|strong|em|b|i|ol|ul|li|br|blockquote|sup|a)\b)[^>]+>", "", text, flags=re.I)
    return re.sub(r"[ \t]+", " ", text.replace("&nbsp;", " ")).strip()


def key(ayah_key: str) -> int:
    surah, ayah = (int(x) for x in ayah_key.split(":"))
    assert 1 <= surah <= 114 and ayah >= 1, ayah_key
    return surah * 1000 + ayah


def read_surah_info(src: Path):
    rows = sqlite3.connect(src).execute(
        "SELECT surah_number, surah_name, short_text, text FROM surah_infos ORDER BY surah_number").fetchall()
    assert 100 <= len(rows) <= 114, f"{src.name}: {len(rows)} surah"
    # Sumber QUL pernah mengirim satu teks yang sama untuk hampir semua surah (Tamil); catatan pendek yang berulang
    # (Italia: "Vedi Appendice 1") masih wajar, jadi batasnya longgar.
    distinct = len({r[3] for r in rows})
    assert distinct >= 0.8 * len(rows), f"{src.name}: hanya {distinct} teks berbeda dari {len(rows)} surah (sumber rusak?)"
    return [(n, name, (short or "").strip(), clean_html(text or "")) for n, name, short, text in rows], len(rows)


def read_tafsir(src: Path):
    """Satu baris per kelompok ayat: teks QUL hanya ada di ayat pertama kelompok, ayat lain kosong."""
    con = sqlite3.connect(src)
    rows = []
    for group, first, last, keys, text in con.execute(
            "SELECT group_ayah_key, from_ayah, to_ayah, ayah_keys, text FROM tafsir WHERE text <> '' AND text IS NOT NULL"):
        start = key(first or group)
        members = [key(k) for k in (keys or "").split(",") if k.strip()]
        end = max([start] + members + ([key(last)] if last else []))
        cleaned = clean_html(text)
        # Beberapa sumber (Al-Nashr, Ibn al-Qayyim, Ad-Durr al-Masun, ...) mengisi ayat tanpa tafsir dengan penampung kosong
        # seperti <div class=ar lang=ar></div>; baris tanpa huruf atau angka tidak berguna dan dibuang.
        if not re.search(r"\w", re.sub(r"<[^>]+>|&\w+;", "", cleaned)):
            continue
        rows.append((start, end, cleaned))
    rows.sort()
    # Kelompok tidak boleh tumpang tindih; sumber yang punya dua teks untuk ayat awal yang sama digabung.
    merged = []
    for start, end, text in rows:
        if merged and merged[-1][0] == start:
            merged[-1] = (start, max(end, merged[-1][1]), merged[-1][2] + "\n" + text)
        else:
            merged.append((start, end, text))
    assert len(merged) >= 100, f"{src.name}: hanya {len(merged)} kelompok"
    return merged, len(merged)


def build(kind: str, entry: dict):
    pack_id = entry["id"]
    src = SRC / ("surah-info" if kind == "surah_info" else "tafsir") / entry["file"]
    rows, count = (read_surah_info if kind == "surah_info" else read_tafsir)(src)
    OUT.mkdir(parents=True, exist_ok=True)
    db_path = OUT / f"{pack_id}.db"
    db_path.unlink(missing_ok=True)
    db = sqlite3.connect(db_path)
    db.executescript(SCHEMA[kind])
    info = [("id", pack_id), ("kind", kind), ("lang", entry["lang"]), ("name", entry["name"]), ("version", str(PACK_VERSION))]
    if entry.get("author"):
        info.append(("author", entry["author"]))
    db.executemany("INSERT INTO info VALUES (?,?)", info)
    table = "surah_info" if kind == "surah_info" else "tafsir"
    db.executemany(f"INSERT INTO {table} VALUES (?,?,?{',?' if kind == 'surah_info' else ''})", rows)
    db.commit()
    db.execute("VACUUM")
    db.close()
    raw = db_path.read_bytes()
    gz = OUT / f"{pack_id}.db.gz"
    gz.write_bytes(gzip.compress(raw, compresslevel=9, mtime=0))
    db_path.unlink()
    data = gz.read_bytes()
    print(f"  {pack_id}: {count} baris, {len(raw) / 1e6:.1f} MB -> {len(data) / 1e6:.1f} MB gzip")
    result = {
        "id": pack_id, "lang": entry["lang"], "name": entry["name"], "source": entry.get("author", ""), "file": gz.name,
        "bytes": len(data), "size": len(raw), "sha256": hashlib.sha256(data).hexdigest(), "version": PACK_VERSION,
        "compression": "gzip",
    }
    if entry.get("qul"):
        result["qul"] = entry["qul"]
    return result


def main():
    args = sys.argv[1:]
    kinds = [a for a in args if a in KINDS] or list(KINDS)
    wanted = {a for a in args if a not in KINDS}
    catalog_src = json.loads(CATALOG.read_text(encoding="utf-8"))
    catalog_path = OUT / "catalog.json"
    catalog = json.loads(catalog_path.read_text(encoding="utf-8")) if catalog_path.exists() else {"version": 1, "packs": []}
    failed = []
    for kind in kinds:
        entries = [e for e in catalog_src[kind] if "excluded" not in e and (not wanted or e["id"] in wanted)]
        old = {e["id"]: e for e in catalog.get(kind, []) if (OUT / e["file"]).exists()}
        for entry in entries:
            print(f"{kind} {entry['id']} <- {entry['file']}")
            try:
                old[entry["id"]] = build(kind, entry)
            except (AssertionError, sqlite3.Error, OSError, ValueError) as e:
                print(f"  GAGAL {entry['id']}: {e}")
                failed.append(entry["id"])
        order = [e["id"] for e in catalog_src[kind]]
        catalog[kind] = sorted(old.values(), key=lambda e: order.index(e["id"]) if e["id"] in order else len(order))
        total = sum(e["bytes"] for e in catalog[kind])
        print(f"{kind}: {len(catalog[kind])} paket, {total / 1e6:.1f} MB")
    catalog_path.write_text(json.dumps(catalog, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"OK -> {catalog_path}")
    if failed:
        print(f"{len(failed)} paket GAGAL: {', '.join(failed)}")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
