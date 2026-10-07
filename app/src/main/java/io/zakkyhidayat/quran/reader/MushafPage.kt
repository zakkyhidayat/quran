package io.zakkyhidayat.quran.reader

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import io.zakkyhidayat.quran.data.AyahRef
import io.zakkyhidayat.quran.data.LineType
import io.zakkyhidayat.quran.data.PageLine
import io.zakkyhidayat.quran.data.Surah
import io.zakkyhidayat.quran.data.Word

private const val LINES_PER_PAGE = 15
private const val REFERENCE_PX = 100f
private const val DEFAULT_LINE_EM = 16.2f
private const val MIN_LINE_EM = 15.5f
private const val MAX_LINE_EM = 17f
private const val FILL_RATIO = 0.97f

private const val BASMALLAH =
    "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ"

@Volatile
private var fontCache: Set<String>? = null

private fun availableFonts(context: Context): Set<String> =
    fontCache ?: (context.assets.list("fonts")?.toSet() ?: emptySet()).also { fontCache = it }

internal fun pageFontFamily(context: Context, page: Int): FontFamily =
    if ("p$page.ttf" in availableFonts(context)) FontFamily(Font("fonts/p$page.ttf", context.assets)) else FontFamily.Default

internal fun surahNameFontFamily(context: Context): FontFamily =
    FontFamily(Font("fonts/surah_names.ttf", context.assets))

@Composable
fun MushafPage(
    page: Int,
    lines: List<PageLine>,
    surahs: Map<Int, Surah>,
    selected: AyahRef?,
    onWordClick: (Word) -> Unit,
    modifier: Modifier = Modifier,
    tajweed: Boolean = true,
) {
    val context = LocalContext.current
    val pageFont = remember(page) { pageFontFamily(context, page) }
    val surahFont = remember { surahNameFontFamily(context) }
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val glyphFilter = remember(tajweed, dark) { GlyphColors.filter(tajweed, dark) }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth
        val lineHeight = maxHeight / LINES_PER_PAGE
        val glyphSize = remember(page, lines, widthPx) { fitFontSize(measurer, density, lines, pageFont, widthPx) }
        val topPadding = lineHeight * ((LINES_PER_PAGE - lines.size) / 2f)

        Column(Modifier.fillMaxSize()) {
            Box(Modifier.height(topPadding))
            lines.forEach { line ->
                Box(Modifier.fillMaxWidth().height(lineHeight), contentAlignment = Alignment.Center) {
                    when (line.type) {
                        LineType.Ayah -> AyahLine(line, pageFont, glyphSize, glyphFilter, selected, onWordClick)
                        LineType.SurahName -> SurahHeader(surahs[line.surah], surahFont, glyphSize, lineHeight)
                        LineType.Basmallah -> Text(
                            text = BASMALLAH,
                            style = TextStyle(fontSize = glyphSize, color = MaterialTheme.colorScheme.onSurface),
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AyahLine(
    line: PageLine,
    font: FontFamily,
    size: TextUnit,
    filter: ColorFilter?,
    selected: AyahRef?,
    onWordClick: (Word) -> Unit,
) {
    val style = TextStyle(fontFamily = font, fontSize = size)
    val highlight = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
    val gap = with(LocalDensity.current) { (size.toPx() * 0.25f).toDp() }
    val arrangement = if (line.centered) Arrangement.spacedBy(gap, Alignment.CenterHorizontally) else Arrangement.SpaceBetween
    // Glyph V4 adalah satu kata per kode; kata disusun kanan ke kiri dengan jarak dibagi rata (justifikasi).
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = arrangement, verticalAlignment = Alignment.CenterVertically) {
            line.words.forEach { word ->
                val isSelected = selected != null && word.surah == selected.surah && word.ayah == selected.ayah
                Box(
                    Modifier
                        .then(if (isSelected) Modifier.clip(MaterialTheme.shapes.small).background(highlight) else Modifier)
                        .pointerInput(word) { detectTapGestures(onTap = { onWordClick(word) }) },
                ) {
                    Text(
                        text = word.text,
                        style = style,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.graphicsLayer {
                            colorFilter = filter
                            compositingStrategy = CompositingStrategy.Offscreen
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SurahHeader(surah: Surah?, font: FontFamily, glyphSize: TextUnit, lineHeight: Dp) {
    if (surah == null) return
    Surface(
        modifier = Modifier.fillMaxWidth().height(lineHeight * 0.86f),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = surah.nameGlyph.toString(),
                style = TextStyle(
                    fontFamily = font,
                    fontSize = glyphSize * 1.05f,
                    lineHeight = glyphSize * 1.05f,
                    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
                ),
                maxLines = 1,
                softWrap = false,
                // Glyph nama surah punya ascent besar; geser ke atas supaya terlihat di tengah bingkai.
                modifier = Modifier.offset(y = with(LocalDensity.current) { (-glyphSize.toPx() * 0.16f).toDp() }),
            )
        }
    }
}

private fun fitFontSize(
    measurer: TextMeasurer,
    density: Density,
    lines: List<PageLine>,
    font: FontFamily,
    widthPx: Int,
): TextUnit {
    val referenceSp = with(density) { REFERENCE_PX.toSp() }
    var widest = 0
    for (line in lines) {
        if (line.type != LineType.Ayah || line.centered) continue
        val result = measurer.measure(
            text = line.words.joinToString("") { it.text },
            style = TextStyle(fontFamily = font, fontSize = referenceSp),
            softWrap = false,
            maxLines = 1,
            constraints = Constraints(),
        )
        widest = maxOf(widest, result.size.width)
    }
    // Lebar baris penuh ~16 em; halaman tanpa baris penuh (hal. 1) dan halaman dengan satu baris pendek memakai dasar itu.
    val widestEm = if (widest == 0) DEFAULT_LINE_EM else (widest / REFERENCE_PX).coerceIn(MIN_LINE_EM, MAX_LINE_EM)
    return with(density) { (widthPx * FILL_RATIO / widestEm).toSp() }
}
