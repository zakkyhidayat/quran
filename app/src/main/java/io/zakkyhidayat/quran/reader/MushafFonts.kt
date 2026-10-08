package io.zakkyhidayat.quran.reader

import android.content.Context
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily

@Volatile
private var fontCache: Set<String>? = null

private fun availableFonts(context: Context): Set<String> =
    fontCache ?: (context.assets.list("fonts")?.toSet() ?: emptySet()).also { fontCache = it }

internal suspend fun pageFontFamily(context: Context, page: Int, palette: GlyphPalette = GlyphPalette.LightTajweed): FontFamily =
    if ("p$page.ttf" in availableFonts(context)) PalettedFonts.load(context, "p$page.ttf", palette.index) else FontFamily.Default

internal fun cachedPageFont(page: Int, palette: GlyphPalette): FontFamily? = PalettedFonts.cached("p$page.ttf", palette.index)

/** Siapkan font halaman di sekitar posisi baca (di thread latar belakang) agar geser halaman tidak menunggu disk. */
internal fun prefetchPageFonts(context: Context, center: Int, palette: GlyphPalette) {
    val available = availableFonts(context)
    for (page in listOf(center, center - 1, center + 1, center - 2, center + 2)) {
        if (page in 1..604 && "p$page.ttf" in available) PalettedFonts.loadBlocking(context, "p$page.ttf", palette.index)
    }
}

// quran-common: glyph kaligrafi basmalah (U+FDFD), judul juz (U+E001..E01E), dan kata pembuka juz (U+E900..E91D).
internal fun commonFontFamily(context: Context): FontFamily = PalettedFonts.loadBlocking(context, "quran-common.ttf", 0)

internal fun juzTitleGlyph(juz: Int): String = (0xE001 + juz - 1).toChar().toString()

internal fun juzOpeningGlyph(juz: Int): String = (0xE900 + juz - 1).toChar().toString()

internal fun surahNameFontFamily(context: Context): FontFamily = PalettedFonts.loadBlocking(context, "surah_names.ttf", 0)

internal suspend fun surahHeaderFontFamily(context: Context, dark: Boolean): FontFamily =
    // Mode gelap: palet 1 dengan isian bingkai (warna 18, bawaannya hitam) diganti hijau tua (warna 12) agar serasi dengan nomor ayat.
    if (dark) PalettedFonts.load(context, "QCF_SurahHeader_COLOR-Regular.ttf", 1, listOf(18 to 12))
    else PalettedFonts.load(context, "QCF_SurahHeader_COLOR-Regular.ttf", 0)

// KFGQPC Hafs Uthmanic Script: font teks Arab Unicode (sheet ayat, basmalah, hasil pencarian).
internal fun arabicFontFamily(context: Context): FontFamily = PalettedFonts.loadBlocking(context, "UthmanicHafs_V22.ttf", 0)

/**
 * Font antarmuka (nama surah, judul juz, teks Hafs) dimuat sekali di thread latar belakang saat aplikasi mulai, sehingga
 * layar daftar dan lembar ayat tidak memuat font dari aset di thread utama setiap kali disusun ulang (dulu membuat
 * transisi kembali ke daftar kehilangan animasinya).
 */
internal fun preloadUiFonts(context: Context) {
    for (name in listOf("surah_names.ttf", "quran-common.ttf", "UthmanicHafs_V22.ttf")) {
        runCatching { PalettedFonts.loadBlocking(context, name, 0) }
    }
}
