#!/usr/bin/env python3
"""Catat dan periksa berkas data sumber (data-src/) terhadap docs/data-manifest.json.

Pakai:
  python tools/data_manifest.py            # bandingkan data-src/ dengan manifest; keluar 1 bila ada perbedaan
  python tools/data_manifest.py --write    # tulis ulang manifest dari data-src/ (setelah memperbarui data dengan sengaja)

Lihat docs/DATA_SOURCES.md untuk daftar sumber dan langkah pembaruan.
"""
import hashlib
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "data-src"
MANIFEST = ROOT / "docs/data-manifest.json"

# Berkas yang dibaca tools/build_db.py atau disalin ke app/src/main/assets/fonts.
SINGLE_FILES = [
    "layout/qpc-v4-tajweed-15-lines.db",
    "qpc-v4/db/qpc-v4.db",
    "qpc-hafs/qpc-hafs.db",
    "quran-id-with-footnote-tags/quran-id-with-footnote-tags.db",
    "the-sabiq-company-with-footnote-tags/the-sabiq-company-with-footnote-tags.db",
    "king-fahad-quran-complex-with-footnote-tags/king-fahad-quran-complex-with-footnote-tags.db",
    "en-sahih-international-with-footnote-tags/en-sahih-international-with-footnote-tags.db",
    "surah-info-id/surah-info-id.db",
    "surah-info-en/surah-info-en.db",
    "translit-tajweed-simple/english-transliteration-tajweed-simple.db",
    "meta/quran-metadata-juz.sqlite",
    "meta/quran-metadata-hizb.sqlite",
    "meta/quran-metadata-rub.sqlite",
    "meta/quran-metadata-manzil.sqlite",
    "meta/quran-metadata-ruku.sqlite",
    "meta/quran-metadata-sajda.sqlite",
    "meta/quran-metadata-surah-name.sqlite",
    "surah-names/surah_names.ttf",
    "fonts-extra/QCF_SurahHeader_COLOR-Regular.ttf",
    "fonts-extra/UthmanicHafs_V22.ttf",
    "fonts-extra/quran-common.ttf",
]
# Sumber paket terjemahan unduhan (tools/build_translation_packs.py); boleh belum diunduh.
# Folder terjemahan sumber QUL (tools/build_translation_packs.py); semua berkas .db di foldernya dicatat.
OPTIONAL_DIRS = ["translation", "translation-footnote", "translation-split", "translation-footnote-split"]
# Sumber penjelajahan tematik (tools/build_db.py, load_explore); semua berkas .db/.sqlite/.json di foldernya dicatat.
EXPLORE_DIRS = ["topics", "ayah-theme", "similar-ayah", "mutashabihat", "morphology"]
PAGE_FONTS = "ttf"  # p1.ttf .. p604.ttf, dihitung sebagai satu entri gabungan


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def is_optional(key: str) -> bool:
    return key.split("/")[0] in OPTIONAL_DIRS + EXPLORE_DIRS


def collect() -> dict:
    entries = {}
    for rel in SINGLE_FILES:
        path = SRC / rel
        entries[rel] = {"sha256": sha256(path), "bytes": path.stat().st_size} if path.exists() else None
    for folder in OPTIONAL_DIRS:
        files = sorted((SRC / folder).glob("*.db")) if (SRC / folder).is_dir() else []
        for f in files:
            entries[f"{folder}/{f.name}"] = {"sha256": sha256(f), "bytes": f.stat().st_size}
        if not files:
            entries[f"{folder}/*.db"] = None
    for folder in EXPLORE_DIRS:
        files = sorted(p for p in (SRC / folder).iterdir() if p.suffix in (".db", ".sqlite", ".json")) if (SRC / folder).is_dir() else []
        for f in files:
            entries[f"{folder}/{f.name}"] = {"sha256": sha256(f), "bytes": f.stat().st_size}
        if not files:
            entries[f"{folder}/*"] = None
    fonts = [SRC / PAGE_FONTS / f"p{n}.ttf" for n in range(1, 605)]
    present = [f for f in fonts if f.exists()]
    if len(present) == 604:
        combined = hashlib.sha256()
        for f in fonts:
            combined.update(hashlib.sha256(f.read_bytes()).digest())
        entries["ttf/p1..p604.ttf"] = {"sha256": combined.hexdigest(), "bytes": sum(f.stat().st_size for f in fonts), "files": 604}
    else:
        entries["ttf/p1..p604.ttf"] = None
    return entries


def main() -> int:
    current = collect()
    if "--write" in sys.argv:
        missing = [k for k, v in current.items() if v is None and not is_optional(k)]
        if missing:
            print("Tidak bisa menulis manifest, berkas belum lengkap:", *missing, sep="\n  ")
            return 2
        current = {k: v for k, v in current.items() if v is not None}
        MANIFEST.parent.mkdir(parents=True, exist_ok=True)
        MANIFEST.write_text(json.dumps(current, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        print(f"Manifest ditulis: {MANIFEST} ({len(current)} entri)")
        return 0

    recorded = json.loads(MANIFEST.read_text(encoding="utf-8")) if MANIFEST.exists() else {}
    problems = 0
    for key, now in current.items():
        before = recorded.get(key)
        if now is None and is_optional(key) and not any(k.startswith(key.split("/")[0] + "/") for k in recorded):
            print(f"BELUM DIUNDUH {key}")
        elif now is None:
            print(f"HILANG   {key}")
            problems += 1
        elif before is None:
            print(f"BARU     {key} (belum ada di manifest)")
            problems += 1
        elif now["sha256"] != before["sha256"]:
            print(f"BERUBAH  {key}  ({before['bytes']} -> {now['bytes']} byte)")
            problems += 1
        else:
            print(f"sama     {key}")
    if problems:
        print(f"\n{problems} perbedaan. Bila disengaja: jalankan build_db.py, uji aplikasi, lalu `--write`.")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
