#!/usr/bin/env python3
"""Periksa aturan UI yang bisa dibaca dari kode (lihat docs/UI_UX.md, bagian "Aturan UI yang bisa diperiksa").

Aturan:
  1. Warna literal `Color(0x...)` hanya boleh di app/src/main/java/io/zakkyhidayat/quran/ui/theme/.
  2. Impor `androidx.compose.material.icons` (set Material Icons lama) dilarang; ikon datang dari AppIcons (Material
     Symbols, dihasilkan tools/gen_icons.py).
  3. `Icon(... contentDescription = null)` perlu ditinjau: sah bila ikon dekoratif di samping teks,
     tidak sah bila ikon itu satu-satunya isi tombol. Hanya tanda untuk ditinjau, tidak otomatis salah.

Pemakaian:
  python tools/check_ui_rules.py            # laporkan, selalu keluar 0
  python tools/check_ui_rules.py --strict   # keluar 1 bila ada warna literal di luar ui/theme atau impor ikon lama

Belum dipasang di CI. Kedua baseline kosong, jadi pelanggaran apa pun dianggap baru.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "app/src/main/java"
THEME = SRC / "io/zakkyhidayat/quran/ui/theme"

HEX = re.compile(r"Color\(0x[0-9A-Fa-f]+")
LEGACY_ICON = re.compile(r"^import androidx\.compose\.material\.icons\b", re.M)
NULL_CD = re.compile(r"Icon\(.*contentDescription\s*=\s*null")

# Keadaan saat aturan ditambahkan (dihitung ulang dengan skrip ini). Berkas di sini tidak dihitung sebagai baru.
BASELINE_HEX: set[str] = set()  # kosong: warna tajwid dipindah ke ui/theme/TajweedColors.kt
BASELINE_LEGACY_ICON: set[str] = set()  # kosong: ikon sudah Material Symbols (ui/Icons.kt, dihasilkan tools/gen_icons.py)


def rel(path: Path) -> str:
    return path.relative_to(SRC).as_posix()


def main() -> int:
    strict = "--strict" in sys.argv
    files = sorted(SRC.rglob("*.kt"))

    hex_hits: dict[str, int] = {}
    legacy_hits: list[str] = []
    null_cd_hits: list[tuple[str, int]] = []

    for f in files:
        text = f.read_text(encoding="utf-8")
        r = rel(f)
        in_theme = f.is_relative_to(THEME)
        if not in_theme:
            n = len(HEX.findall(text))
            if n:
                hex_hits[r] = n
        if LEGACY_ICON.search(text):
            legacy_hits.append(r)
        for i, line in enumerate(text.splitlines(), 1):
            if NULL_CD.search(line):
                null_cd_hits.append((r, i))

    new_hex = {k: v for k, v in hex_hits.items() if k not in BASELINE_HEX}

    print("1. Warna literal di luar ui/theme")
    if hex_hits:
        for k, v in sorted(hex_hits.items()):
            tag = "" if k in BASELINE_HEX else "  <- BARU"
            print(f"   {k}: {v}{tag}")
    else:
        print("   tidak ada")

    new_legacy = [k for k in legacy_hits if k not in BASELINE_LEGACY_ICON]
    print(f"2. Impor material.icons lama: {len(legacy_hits)} berkas (harus nol; pakai AppIcons)")
    for k in legacy_hits:
        print(f"   {k}{'  <- BARU' if k in new_legacy else ''}")

    print(f"3. Icon dengan contentDescription = null untuk ditinjau: {len(null_cd_hits)} (pencarian per baris; panggilan multi-baris tidak terdeteksi)")
    for k, i in null_cd_hits:
        print(f"   {k}:{i}")

    failed = bool(new_hex or new_legacy)
    if new_hex:
        print(f"\nPelanggaran aturan 1 baru: {', '.join(sorted(new_hex))}")
    if new_legacy:
        print(f"\nPelanggaran aturan 2 baru: {', '.join(sorted(new_legacy))}")
    return 1 if (strict and failed) else 0


if __name__ == "__main__":
    sys.exit(main())
