package io.zakkyhidayat.quran.reader

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import io.zakkyhidayat.quran.AppViewModel
import io.zakkyhidayat.quran.ui.LocalReadingTextScale
import io.zakkyhidayat.quran.ui.scaled

/**
 * Teks Arab satu ayat di daftar (mode ayat, kumpulan doa): glyph font halaman mushaf, berwarna bila [tajweed] aktif dan
 * font berwarnanya sudah siap. Bila font halaman belum terpasang, dipakai teks Hafs [fallbackText].
 */
@Composable
internal fun AyahArabicText(
    vm: AppViewModel,
    surah: Int,
    ayah: Int,
    page: Int,
    fallbackText: String,
    tajweed: Boolean,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    val context = LocalContext.current
    val glyphText by produceState<String?>(null, surah, ayah) {
        value = vm.mushaf.ayahGlyphText(surah, ayah).takeIf { it.isNotBlank() }
    }
    val arabicFont = remember { arabicFontFamily(context) }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val plainPalette = GlyphPalette.of(false, isDark)
    val tajweedPalette = GlyphPalette.of(true, isDark)
    val plainGlyphFont by produceState<FontFamily?>(
        cachedPageFont(page, plainPalette).takeIf { hasPageFont(context, page) },
        page,
        plainPalette,
    ) {
        value = if (hasPageFont(context, page)) pageFontFamily(context, page, plainPalette) else null
    }
    val tajweedGlyphFont by produceState<FontFamily?>(
        cachedPageFont(page, tajweedPalette).takeIf { hasPageFont(context, page) },
        page,
        tajweedPalette,
    ) {
        value = if (hasPageFont(context, page)) pageFontFamily(context, page, tajweedPalette) else null
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val style = MaterialTheme.typography.headlineSmall.scaled(LocalReadingTextScale.current.arabic, 1.9f)
        val glyphs = glyphText?.takeIf { plainGlyphFont != null }
        if (glyphs == null) {
            Text(fallbackText, style = style.copy(fontFamily = arabicFont), textAlign = TextAlign.Start, modifier = modifier)
        } else {
            // Tajwid berganti seketika: font berwarna dipakai begitu siap, tanpa lapisan dan animasi.
            Text(
                glyphs,
                style = style.copy(fontFamily = (if (tajweed) tajweedGlyphFont else null) ?: plainGlyphFont),
                textAlign = TextAlign.Start,
                modifier = modifier,
            )
        }
    }
}
