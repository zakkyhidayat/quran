#!/usr/bin/env python3
"""Tangkap file unduhan QUL dari folder Downloads dan taruh di data-src/<kategori>/.

Unduhan QUL perlu login, jadi unduh lewat browser seperti biasa; skrip ini mengawasi folder
Downloads, membuka .zip/.bz2/.gz, memeriksa isinya (SQLite atau JSON), lalu menyalinnya.
File di Downloads tidak diubah atau dihapus.

Contoh:
    python tools/qul_catch.py translation          # buka halaman QUL di browser, lalu awasi Downloads
    python tools/qul_catch.py tafsir --no-open
    python tools/qul_catch.py recitation --existing  # proses juga file yang sudah ada di Downloads
    python tools/qul_catch.py --list                # tampilkan isi data-src per kategori

Tekan Ctrl+C untuk berhenti.
"""
import argparse
import bz2
import gzip
import io
import shutil
import sqlite3
import subprocess
import sys
import time
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DATA_SRC = ROOT / "data-src"
PAGES = {
    "recitation": "https://qul.tarteel.ai/resources/recitation",
    "translation": "https://qul.tarteel.ai/resources/translation",
    "tafsir": "https://qul.tarteel.ai/resources/tafsir",
    "transliteration": "https://qul.tarteel.ai/resources/transliteration",
}
KEEP = (".db", ".sqlite", ".sqlite3", ".json")
PARTIAL = (".crdownload", ".part", ".download", ".tmp")
SQLITE_MAGIC = b"SQLite format 3\x00"


def unpack(path: Path) -> list[tuple[str, bytes]]:
    """Isi file unduhan sebagai (nama, bytes). Arsip dibuka di memori; nama dipipihkan (tanpa folder)."""
    data = path.read_bytes()
    name = path.name
    if zipfile.is_zipfile(io.BytesIO(data)):  # QUL kadang memberi ekstensi .bz2 pada file zip
        out = []
        with zipfile.ZipFile(io.BytesIO(data)) as z:
            for info in z.infolist():
                base = Path(info.filename).name  # cegah path traversal
                if not info.is_dir() and base and not base.startswith("."):
                    out.extend(unpack_bytes(base, z.read(info)))
        return out
    return unpack_bytes(name, data)


def unpack_bytes(name: str, data: bytes) -> list[tuple[str, bytes]]:
    for ext, opener in ((".bz2", bz2.decompress), (".gz", gzip.decompress)):
        if name.endswith(ext):
            try:
                return unpack_bytes(name[: -len(ext)], opener(data))
            except OSError:
                return []
    if name.endswith(".zip"):
        return []  # zip bersarang tidak didukung
    return [(name, data)]


def describe(name: str, data: bytes) -> str | None:
    """Ringkasan isi bila file layak disimpan, None bila bukan SQLite/JSON."""
    if data.startswith(SQLITE_MAGIC):
        tmp = DATA_SRC / ".probe.db"
        tmp.write_bytes(data)
        try:
            con = sqlite3.connect(f"file:{tmp}?mode=ro", uri=True)
            tables = [r[0] for r in con.execute("SELECT name FROM sqlite_master WHERE type='table'")]
            counts = [f"{t}({con.execute(f'SELECT count(*) FROM \"{t}\"').fetchone()[0]})" for t in tables]
            con.close()
            return "SQLite: " + ", ".join(counts)
        except sqlite3.DatabaseError:
            return None
        finally:
            tmp.unlink(missing_ok=True)
    if name.endswith(".json"):
        return f"JSON {len(data):,} byte"
    return None


def store(src: Path, category: str) -> None:
    dest_dir = DATA_SRC / category
    try:
        items = unpack(src)
    except (OSError, zipfile.BadZipFile) as e:
        print(f"  ! {src.name}: gagal dibuka ({e})")
        return
    saved = 0
    for name, data in items:
        if not name.endswith(KEEP) and not data.startswith(SQLITE_MAGIC):
            continue
        info = describe(name, data)
        if info is None:
            continue
        if data.startswith(SQLITE_MAGIC) and not name.endswith((".db", ".sqlite", ".sqlite3")):
            name += ".db"
        dest_dir.mkdir(parents=True, exist_ok=True)
        dest = dest_dir / name
        if dest.exists() and dest.read_bytes() == data:
            print(f"  = {category}/{name} sudah ada (sama)")
            continue
        n = 1
        while dest.exists():
            dest = dest_dir / f"{Path(name).stem}-{n}{Path(name).suffix}"
            n += 1
        dest.write_bytes(data)
        saved += 1
        print(f"  + {dest.relative_to(ROOT)}  [{info}]")
    if not saved and not items:
        print(f"  ! {src.name}: tidak ada SQLite/JSON di dalamnya")


def stable(path: Path) -> bool:
    """File selesai diunduh: bukan file parsial dan ukurannya tidak berubah selama 1 detik."""
    if path.suffix in PARTIAL or not path.is_file():
        return False
    size = path.stat().st_size
    time.sleep(1)
    return path.exists() and path.stat().st_size == size and size > 0


def watch(downloads: Path, category: str, existing: bool) -> None:
    seen = set() if existing else {p for p in downloads.iterdir()}
    print(f"Mengawasi {downloads} -> data-src/{category}/  (Ctrl+C untuk berhenti)")
    while True:
        for p in sorted(downloads.iterdir(), key=lambda p: p.stat().st_mtime if p.exists() else 0):
            if p in seen or p.suffix in PARTIAL or p.is_dir():
                continue
            if stable(p):
                seen.add(p)
                print(f"- {p.name}")
                store(p, category)
        time.sleep(1)


def list_data() -> None:
    for d in sorted(p for p in DATA_SRC.iterdir() if p.is_dir()) if DATA_SRC.exists() else []:
        files = sorted(f.name for f in d.rglob("*") if f.is_file())
        print(f"{d.name}/ ({len(files)})")
        for f in files:
            print(f"    {f}")


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("category", nargs="?", help="recitation, translation, tafsir, transliteration, atau nama folder lain")
    ap.add_argument("--downloads", type=Path, default=Path.home() / "Downloads")
    ap.add_argument("--no-open", action="store_true", help="jangan buka halaman QUL di browser")
    ap.add_argument("--existing", action="store_true", help="proses juga file yang sudah ada di Downloads")
    ap.add_argument("--list", action="store_true", help="tampilkan isi data-src")
    args = ap.parse_args()
    sys.stdout.reconfigure(line_buffering=True)
    if args.list:
        return list_data()
    if not args.category:
        ap.error("sebutkan kategori, mis. translation")
    DATA_SRC.mkdir(exist_ok=True)
    if not args.no_open and args.category in PAGES:
        subprocess.Popen(["xdg-open", PAGES[args.category]], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    try:
        watch(args.downloads, args.category, args.existing)
    except KeyboardInterrupt:
        print()


if __name__ == "__main__":
    sys.exit(main())
