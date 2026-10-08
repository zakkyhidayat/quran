#!/usr/bin/env python3
"""Hasilkan ui/theme/OriginalColors.kt (skema warna 'Asli', seed teal) dengan tiga tingkat kontras.

Pakai: pip install materialyoucolor && python tools/gen_colors.py
"""
from pathlib import Path

from materialyoucolor.dynamiccolor.material_dynamic_colors import MaterialDynamicColors as M
from materialyoucolor.hct import Hct
from materialyoucolor.scheme.scheme_tonal_spot import SchemeTonalSpot

SEED = 0xFF006A60
OUT = Path(__file__).resolve().parent.parent / "app/src/main/java/io/zakkyhidayat/quran/ui/theme/OriginalColors.kt"

ROLES = [
    "primary", "onPrimary", "primaryContainer", "onPrimaryContainer", "inversePrimary",
    "secondary", "onSecondary", "secondaryContainer", "onSecondaryContainer",
    "tertiary", "onTertiary", "tertiaryContainer", "onTertiaryContainer",
    "background", "onBackground", "surface", "onSurface", "surfaceVariant", "onSurfaceVariant", "surfaceTint",
    "inverseSurface", "inverseOnSurface",
    "error", "onError", "errorContainer", "onErrorContainer",
    "outline", "outlineVariant", "scrim",
    "surfaceBright", "surfaceDim",
    "surfaceContainerLowest", "surfaceContainerLow", "surfaceContainer", "surfaceContainerHigh", "surfaceContainerHighest",
    "primaryFixed", "primaryFixedDim", "onPrimaryFixed", "onPrimaryFixedVariant",
    "secondaryFixed", "secondaryFixedDim", "onSecondaryFixed", "onSecondaryFixedVariant",
    "tertiaryFixed", "tertiaryFixedDim", "onTertiaryFixed", "onTertiaryFixedVariant",
]

LEVELS = [("Standard", 0.0), ("Medium", 0.5), ("High", 1.0)]


def scheme_block(name: str, dark: bool, contrast: float) -> str:
    scheme = SchemeTonalSpot(Hct.from_int(SEED), dark, contrast)
    fn = "darkColorScheme" if dark else "lightColorScheme"
    lines = [f"internal val {name}: ColorScheme = {fn}("]
    for role in ROLES:
        argb = getattr(M, role).get_hct(scheme).to_int() & 0xFFFFFFFF
        lines.append(f"    {role} = Color(0x{argb:08X}),")
    lines.append(")")
    return "\n".join(lines)


def main() -> None:
    parts = [
        "// Dihasilkan oleh tools/gen_colors.py (seed teal 0xFF006A60). Jangan diedit manual.",
        "package io.zakkyhidayat.quran.ui.theme",
        "",
        "import androidx.compose.material3.ColorScheme",
        "import androidx.compose.material3.darkColorScheme",
        "import androidx.compose.material3.lightColorScheme",
        "import androidx.compose.ui.graphics.Color",
        "",
    ]
    for level, contrast in LEVELS:
        for dark in (False, True):
            parts.append(scheme_block(f"Original{'Dark' if dark else 'Light'}{level}", dark, contrast))
            parts.append("")
    OUT.write_text("\n".join(parts), encoding="utf-8")
    print(f"OK -> {OUT}")


if __name__ == "__main__":
    main()
