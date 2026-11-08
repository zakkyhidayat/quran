#!/usr/bin/env python3
"""Hasilkan app/src/main/java/io/zakkyhidayat/quran/ui/Icons.kt dari Material Symbols (lihat docs/UI_UX.md).

Sumber: paket npm `@material-symbols/svg-400` (Apache-2.0), gaya Rounded, bobot 400. Ikon yang dulu bergaya Filled di set
Material Icons lama memakai varian `-fill`; garis murni (panah, tutup, cari) sama saja. Dua ikon gambar sendiri
(Quarter, TextLines) tidak ada di Symbols dan ditulis apa adanya.

Pemakaian:
  mkdir /tmp/ms && cd /tmp/ms && npm pack @material-symbols/svg-400@0.48.3 && mkdir pkg && tar -xzf *.tgz -C pkg
  python tools/gen_icons.py /tmp/ms/pkg/package          # tulis Icons.kt
  python tools/gen_icons.py /tmp/ms/pkg/package --check  # hanya periksa bahwa Icons.kt sudah sesuai

Path Symbols memakai viewBox "0 -960 960 960" (sumbu y negatif). Skrip menggeser y sebesar +960 supaya viewport
Compose 0..960 bisa dipakai langsung, tanpa grup transformasi saat dijalankan.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "app/src/main/java/io/zakkyhidayat/quran/ui/Icons.kt"
VERSION = "0.48.3"

# nama Kotlin: (nama simbol, isi, cermin RTL)
ICONS: dict[str, tuple[str, bool, bool]] = {
    # Pengganti Icons.Default/AutoMirrored yang dulu dari paket icons-core.
    "ArrowBack": ("arrow_back", True, True),
    "KeyboardArrowRight": ("keyboard_arrow_right", True, True),
    "KeyboardArrowLeft": ("keyboard_arrow_left", True, True),
    "KeyboardArrowUp": ("keyboard_arrow_up", True, False),
    "KeyboardArrowDown": ("keyboard_arrow_down", True, False),
    "Close": ("close", True, False),
    "Delete": ("delete", True, False),
    "Search": ("search", True, False),
    "Check": ("check", True, False),
    "Share": ("share", True, False),
    "Settings": ("settings", True, False),
    "Info": ("info", True, False),
    "Refresh": ("refresh", True, False),
    # Ikon yang sudah ada di AppIcons.
    "Bookmark": ("bookmark", True, False),
    "BookmarkBorder": ("bookmark", False, False),
    "ContentCopy": ("content_copy", True, False),
    "Shuffle": ("shuffle", True, False),
    "LightMode": ("light_mode", True, False),
    "DarkMode": ("dark_mode", True, False),
    "BrightnessAuto": ("brightness_auto", True, False),
    "Palette": ("palette", True, False),
    "AutoAwesome": ("star_shine", True, False),  # auto_awesome diganti nama di Symbols
    "Contrast": ("contrast", True, False),
    "FormatColorText": ("format_color_text", True, False),
    "ExpandMore": ("keyboard_arrow_down", True, False),
    "ExpandLess": ("keyboard_arrow_up", True, False),
    "Category": ("category", True, False),
    "Lightbulb": ("lightbulb", True, False),
    "FormatQuote": ("format_quote", True, False),
    "CompareArrows": ("compare_arrows", True, False),
    "AccountTree": ("account_tree", True, False),
    "MenuBook": ("menu_book", True, False),
    "Home": ("home", True, False),
    "GridView": ("grid_view", True, False),
    "Notes": ("notes", True, False),
    "Shield": ("shield", True, False),
    "Warning": ("warning", True, False),
    "Alarm": ("alarm", True, False),
    "Schedule": ("schedule", True, False),
    "Backup": ("backup", True, False),
    "Upload": ("upload", True, False),
    "Download": ("download", True, False),
    "OpenInNew": ("open_in_new", True, False),
    "Favorite": ("favorite", True, False),
    "Translate": ("translate", True, False),
    "PieChart": ("pie_chart", True, False),
    "CalendarViewWeek": ("calendar_view_week", True, False),
    "FormatListNumbered": ("format_list_numbered", True, False),
    # Halaman Pengaturan (status data, izin notifikasi, cadangan, tentang).
    "CheckCircle": ("check_circle", True, False),
    "Notifications": ("notifications", True, False),
    "UploadFile": ("upload_file", True, False),
    "Restore": ("settings_backup_restore", True, False),
    "Database": ("database", True, False),
    "FontDownload": ("font_download", True, False),
    "Groups": ("groups", True, False),
    "Language": ("language", True, False),
}

# Gambar sendiri, viewport 24.
CUSTOM = {
    "Quarter": (
        "Seperempat lingkaran terpisah dari sisanya: untuk rub' (seperempat hizb).",
        "M13,2.05V11h8.95C21.48,6.28 17.72,2.52 13,2.05zM11,2.05C5.95,2.55 2,6.82 2,12c0,5.52 4.48,10 10,10 5.18,0 9.45,-3.95 9.95,-9H11V2.05z",
    ),
    "TextLines": ("Tiga baris teks dengan panjang berbeda.", "M3,4h18v2H3zM3,11h14v2H3zM3,18h18v2H3z"),
}

ARGS = {"M": 2, "L": 2, "H": 1, "V": 1, "C": 6, "S": 4, "Q": 4, "T": 2, "A": 7, "Z": 0}
TOKEN = re.compile(r"([MmLlHhVvCcSsQqTtAaZz])|(-?(?:\d+\.?\d*|\.\d+)(?:[eE][-+]?\d+)?)")


def shift_y(d: str, dy: float = 960.0) -> str:
    """Geser semua koordinat y absolut sebesar dy; perintah relatif tidak berubah, kecuali moveto relatif pertama."""
    tokens = TOKEN.findall(d)
    out: list[str] = []
    i = 0
    cmd = ""
    first = True
    n = len(tokens)

    def fmt(v: float) -> str:
        s = f"{v:.4f}".rstrip("0").rstrip(".")
        return s if s not in ("", "-0") else "0"

    while i < n:
        c, num = tokens[i]
        if c:
            cmd = c
            i += 1
            if cmd in "Zz":
                out.append("Z")
                continue
        need = ARGS[cmd.upper()]
        vals: list[float] = []
        for _ in range(need):
            c2, num2 = tokens[i]
            assert not c2, f"argumen kurang untuk {cmd}: {d[:60]}"
            vals.append(float(num2))
            i += 1
        up = cmd.isupper()
        k = cmd.upper()
        if up:
            if k in "MLTCSQ":
                for j in range(1, need, 2):
                    vals[j] += dy
            elif k == "V":
                vals[0] += dy
            elif k == "A":
                vals[6] += dy
        elif first and k == "M":
            vals[1] += dy  # moveto relatif pertama dibaca absolut
        first = False
        # ulangan implisit setelah M menjadi L (m menjadi l)
        out.append(cmd + " " + " ".join(fmt(v) for v in vals))
        if k == "M":
            cmd = "L" if up else "l"
    return " ".join(out)


def read_path(pkg: Path, symbol: str, fill: bool) -> str:
    f = pkg / "rounded" / f"{symbol}{'-fill' if fill else ''}.svg"
    if not f.exists() and fill:
        f = pkg / "rounded" / f"{symbol}.svg"
    text = f.read_text(encoding="utf-8")
    assert 'viewBox="0 -960 960 960"' in text, f
    paths = re.findall(r'<path d="([^"]+)"', text)
    assert len(paths) == 1, (f, len(paths))
    return paths[0]


def build(pkg: Path) -> str:
    lines = [
        "package io.zakkyhidayat.quran.ui",
        "",
        "import androidx.compose.ui.graphics.Color",
        "import androidx.compose.ui.graphics.SolidColor",
        "import androidx.compose.ui.graphics.vector.ImageVector",
        "import androidx.compose.ui.graphics.vector.addPathNodes",
        "import androidx.compose.ui.unit.dp",
        "",
        f"// DIHASILKAN oleh tools/gen_icons.py dari Material Symbols (npm @material-symbols/svg-400 {VERSION}, Apache-2.0),",
        "// gaya Rounded, bobot 400. Jangan diedit tangan: ubah daftar di skrip lalu jalankan ulang.",
        "// ikon dengan autoMirror = true berbalik pada tata letak kanan-ke-kiri (Arab, Persia, Urdu).",
        "object AppIcons {",
    ]
    for name, (symbol, fill, mirror) in ICONS.items():
        d = shift_y(read_path(pkg, symbol, fill))
        extra = ", autoMirror = true" if mirror else ""
        lines.append(f'    val {name}: ImageVector by lazy {{ symbol("{name}", "{d}"{extra}) }}')
    lines.append("")
    for name, (doc, d) in CUSTOM.items():
        lines.append(f"    /** {doc} */")
        lines.append(f'    val {name}: ImageVector by lazy {{ custom("{name}", "{d}") }}')
    lines += [
        "}",
        "",
        "// Path Symbols sudah digeser ke viewport 0..960 oleh skrip (aslinya viewBox 0 -960 960 960).",
        "private fun symbol(name: String, pathData: String, autoMirror: Boolean = false): ImageVector =",
        "    ImageVector.Builder(name, 24.dp, 24.dp, 960f, 960f, autoMirror = autoMirror)",
        "        .addPath(addPathNodes(pathData), fill = SolidColor(Color.Black))",
        "        .build()",
        "",
        "private fun custom(name: String, pathData: String): ImageVector =",
        "    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)",
        "        .addPath(addPathNodes(pathData), fill = SolidColor(Color.Black))",
        "        .build()",
        "",
    ]
    return "\n".join(lines)


def main() -> int:
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    if not args:
        print(__doc__)
        return 2
    text = build(Path(args[0]))
    if "--check" in sys.argv:
        ok = OUT.read_text(encoding="utf-8") == text
        print("Icons.kt sesuai" if ok else "Icons.kt berbeda dari hasil skrip")
        return 0 if ok else 1
    OUT.write_text(text, encoding="utf-8")
    print(f"ditulis {OUT.relative_to(ROOT)}: {len(ICONS)} ikon Symbols + {len(CUSTOM)} gambar sendiri")
    return 0


if __name__ == "__main__":
    sys.exit(main())
