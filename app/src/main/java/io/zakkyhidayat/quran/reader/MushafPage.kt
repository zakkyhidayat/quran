package io.zakkyhidayat.quran.reader

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import io.zakkyhidayat.quran.data.AyahRef
import io.zakkyhidayat.quran.data.AyahText
import io.zakkyhidayat.quran.data.LineType
import io.zakkyhidayat.quran.data.PageLine
import io.zakkyhidayat.quran.data.Surah
import kotlin.math.roundToInt

private const val LINES_PER_PAGE = 15
private const val REFERENCE_PX = 100f
private const val DEFAULT_LINE_EM = 16.2f
private const val MIN_LINE_EM = 15.5f
private const val MAX_LINE_EM = 17f
private const val FILL_RATIO = 0.995f

@Volatile
private var fontCache: Set<String>? = null

private fun availableFonts(context: Context): Set<String> =
    fontCache ?: (context.assets.list("fonts")?.toSet() ?: emptySet()).also { fontCache = it }

internal fun pageFontFamily(context: Context, page: Int): FontFamily =
    if ("p$page.ttf" in availableFonts(context)) FontFamily(Font("fonts/p$page.ttf", context.assets)) else FontFamily.Default

// quran-common: glyph kaligrafi basmalah (U+FDFD), judul juz (U+E001..E01E), dan kata pembuka juz (U+E900..E91D).
internal fun commonFontFamily(context: Context): FontFamily =
    FontFamily(Font("fonts/quran-common.ttf", context.assets))

internal fun juzTitleGlyph(juz: Int): String = (0xE001 + juz - 1).toChar().toString()

internal fun juzOpeningGlyph(juz: Int): String = (0xE900 + juz - 1).toChar().toString()

internal fun surahNameFontFamily(context: Context): FontFamily =
    FontFamily(Font("fonts/surah_names.ttf", context.assets))

internal fun surahHeaderFontFamily(context: Context): FontFamily =
    FontFamily(Font("fonts/QCF_SurahHeader_COLOR-Regular.ttf", context.assets))

// KFGQPC Hafs Uthmanic Script: font teks Arab Unicode (sheet ayat, basmalah, hasil pencarian).
internal fun arabicFontFamily(context: Context): FontFamily =
    FontFamily(Font("fonts/UthmanicHafs_V22.ttf", context.assets))

@Composable
fun MushafPage(
    page: Int,
    lines: List<PageLine>,
    ayahTexts: List<AyahText>,
    surahs: Map<Int, Surah>,
    selected: AyahRef?,
    onAyahClick: (AyahRef) -> Unit,
    modifier: Modifier = Modifier,
    tajweed: Boolean = true,
    onSurahClick: (Int) -> Unit = {},
) {
    val context = LocalContext.current
    val pageFont = remember(page) { pageFontFamily(context, page) }
    val headerFont = remember { surahHeaderFontFamily(context) }
    val basmalahFont = remember { pageFontFamily(context, 1) }
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val glyphFilter = remember(tajweed, dark) { GlyphColors.filter(tajweed, dark) }
    val headerFilter = remember(dark) { GlyphColors.filter(true, dark) }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth
        val lineHeight = maxHeight / LINES_PER_PAGE
        val glyphSize = remember(page, lines, widthPx) { fitFontSize(measurer, density, lines, pageFont, widthPx) }
        val topPadding = lineHeight * ((LINES_PER_PAGE - lines.size) / 2f)

        ScreenReaderLayer(lines, ayahTexts, surahs, onAyahClick, onSurahClick)

        // Glyph V4 berupa kode private-use: tidak berguna untuk pembaca layar, jadi disembunyikan dari semantics.
        Column(Modifier.fillMaxSize().clearAndSetSemantics { }) {
            Box(Modifier.height(topPadding))
            lines.forEach { line ->
                Box(Modifier.fillMaxWidth().height(lineHeight), contentAlignment = Alignment.Center) {
                    when (line.type) {
                        LineType.Ayah -> AyahLine(line, pageFont, glyphSize, glyphFilter, selected, onAyahClick, lineHeight)
                        LineType.SurahName -> SurahHeader(surahs[line.surah], headerFont, headerFilter) { onSurahClick(line.surah!!) }
                        LineType.Basmallah -> BasmalahLine(glyphSize, glyphFilter, basmalahFont)
                    }
                }
            }
        }
    }
}

// Satu simpul aksesibilitas per surah dan per ayat dengan teks Arab Unicode; ketuk dua kali membuka terjemahan.
@Composable
private fun ScreenReaderLayer(
    lines: List<PageLine>,
    ayahTexts: List<AyahText>,
    surahs: Map<Int, Surah>,
    onAyahClick: (AyahRef) -> Unit,
    onSurahClick: (Int) -> Unit,
) {
    val byRef = remember(ayahTexts) { ayahTexts.associateBy { AyahRef(it.surah, it.ayah) } }
    val seen = HashSet<AyahRef>()
    Column {
        lines.forEach { line ->
            when (line.type) {
                LineType.SurahName -> surahs[line.surah]?.let { surah ->
                    Box(
                        Modifier.size(1.dp).semantics {
                            heading()
                            contentDescription = "Surah ${surah.nameLatin}"
                            onClick(label = "Buka info surah") { onSurahClick(surah.id); true }
                        },
                    )
                }
                LineType.Ayah -> line.words.forEach { word ->
                    val ref = AyahRef(word.surah, word.ayah)
                    val text = byRef[ref]
                    if (text != null && seen.add(ref)) {
                        val name = surahs[ref.surah]?.nameLatin.orEmpty()
                        Box(
                            Modifier.size(1.dp).semantics {
                                role = Role.Button
                                contentDescription = "$name ayat ${ref.ayah}. ${text.text}"
                                onClick(label = "Tampilkan terjemahan") { onAyahClick(ref); true }
                            },
                        )
                    }
                }
                LineType.Basmallah -> Box(Modifier.size(1.dp).semantics { contentDescription = "Bismillahirrahmanirrahim" })
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
    onAyahClick: (AyahRef) -> Unit,
    cellHeight: Dp,
) {
    val style = TextStyle(fontFamily = font, fontSize = size)
    // Abu-abu netral (warna teks di atas latar), bukan warna tema: warna tajwid (hijau, merah, biru) tetap terbaca.
    // Terang: 10% (lebih pucat); gelap: 18% (lebih terang).
    // Buram (sudah dicampur dengan latar) supaya tumpang tindih antar baris tidak tampak sebagai garis lebih gelap.
    val highlightAlpha = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) 0.18f else 0.10f
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = highlightAlpha).compositeOver(MaterialTheme.colorScheme.background)
    val density = LocalDensity.current
    val gapPx = with(density) { size.toPx() * 0.25f }
    val pad = with(density) { 2.dp.toPx() }
    val corner = with(density) { 8.dp.toPx() }
    // Tinggi sorotan = satu sel baris penuh (tanpa celah antar baris); tumpang tindih 1 px penuh di tiap sisi menutup piksel sambungan sepenuhnya (anti-aliasing setengah piksel meninggalkan garis lebih terang).
    val highlightHeight = with(density) { cellHeight.toPx() }
    val words = line.words
    val isSelected = remember(line, selected) {
        BooleanArray(words.size) { i -> selected != null && words[i].surah == selected.surah && words[i].ayah == selected.ayah }
    }
    // Batas kiri/kanan tiap kata, diisi saat tata letak dan dibaca saat menggambar sorotan.
    val bounds = remember(line) { FloatArray(words.size * 2) }

    JustifiedRow(
        centered = line.centered,
        minGapPx = gapPx,
        bounds = bounds,
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                var i = 0
                while (i < isSelected.size) {
                    if (!isSelected[i]) { i++; continue }
                    var j = i
                    while (j + 1 < isSelected.size && isSelected[j + 1]) j++
                    // Teks berjalan kanan ke kiri: kata pertama paling kanan.
                    // Sisi kiri tanpa pad: di sana biasanya kata pertama ayat berikutnya, jangan sampai tertutup sorotan.
                    val left = bounds[2 * j]
                    val right = bounds[2 * i + 1] + pad
                    val height = minOf(highlightHeight, this.size.height)
                    drawRoundRect(highlight, Offset(left, (this.size.height - height) / 2f - 1f), Size(right - left, height + 2f), CornerRadius(corner))
                    i = j + 1
                }
            }
            .pointerInput(line) {
                // Ketuk di mana pun pada baris memilih ayat dari kata terdekat, termasuk di celah antarkata.
                detectTapGestures { tap ->
                    var best = -1
                    var bestDistance = Float.MAX_VALUE
                    for (i in words.indices) {
                        val d = if (tap.x < bounds[2 * i]) bounds[2 * i] - tap.x else if (tap.x > bounds[2 * i + 1]) tap.x - bounds[2 * i + 1] else 0f
                        if (d < bestDistance) { bestDistance = d; best = i }
                    }
                    if (best >= 0) onAyahClick(AyahRef(words[best].surah, words[best].ayah))
                }
            },
    ) {
        words.forEach { word ->
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

// Susun kata dari kanan ke kiri. Baris penuh dibagi rata (justifikasi); baris pendek ditengahkan dengan jarak tetap.
@Composable
private fun JustifiedRow(
    centered: Boolean,
    minGapPx: Float,
    bounds: FloatArray,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val placeables = measurables.map { it.measure(Constraints()) }
        val width = constraints.maxWidth
        val height = placeables.maxOfOrNull { it.height } ?: 0
        val total = placeables.sumOf { it.width }
        val count = placeables.size
        val gap = if (centered || count < 2) minGapPx else ((width - total).toFloat() / (count - 1)).coerceAtLeast(0f)
        val used = total + gap * (count - 1).coerceAtLeast(0)
        var x = if (centered) width - (width - used) / 2f else width.toFloat()
        layout(width, height) {
            placeables.forEachIndexed { i, placeable ->
                x -= placeable.width
                placeable.place(x.roundToInt(), (height - placeable.height) / 2)
                bounds[2 * i] = x
                bounds[2 * i + 1] = x + placeable.width
                x -= gap
            }
        }
    }
}

// Basmalah memakai glyph ayat 1:1 dari font halaman 1 (U+FC41..FC44 = bismi / Allahi / alrrahmani / alrraheemi),
// sehingga gaya dan warna tajwidnya sama dengan teks ayat di halaman.
private const val BASMALAH_GLYPHS = "ﱁﱂﱃﱄ"

@Composable
private fun BasmalahLine(size: TextUnit, filter: ColorFilter?, font: FontFamily) {
    val style = TextStyle(fontFamily = font, fontSize = size)
    val gap = with(LocalDensity.current) { size.toPx() * 0.25f }
    val bounds = remember { FloatArray(BASMALAH_GLYPHS.length * 2) }
    JustifiedRow(centered = true, minGapPx = gap, bounds = bounds, modifier = Modifier.fillMaxWidth()) {
        BASMALAH_GLYPHS.forEach { glyph ->
            Text(
                text = glyph.toString(),
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

// Bingkai header berwarna (font QCF_SurahHeader): lebar 3,267 em dan tinggi 0,41 em, simetris terhadap baseline.
private const val HEADER_UPEM = 2500f
private const val HEADER_FRAME_EM = 8167f / HEADER_UPEM
private const val HEADER_FRAME_LEFT_EM = 41f / HEADER_UPEM
private const val HEADER_FRAME_HEIGHT_EM = 1026f / HEADER_UPEM

// Bingkai diskalakan vertikal supaya ada jarak dengan baris ayat di atas dan basmalah di bawahnya.
private const val HEADER_SCALE_Y = 0.66f

@Composable
private fun SurahHeader(surah: Surah?, font: FontFamily, filter: ColorFilter?, onClick: () -> Unit) {
    if (surah == null) return
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val fontPx = constraints.maxWidth * 0.99f / HEADER_FRAME_EM
        val fontSize = with(density) { fontPx.toSp() }
        val layout = remember(surah.id, font, fontSize) {
            measurer.measure(
                text = surah.nameGlyph.toString(),
                style = TextStyle(fontFamily = font, fontSize = fontSize),
                softWrap = false,
                maxLines = 1,
                constraints = Constraints(),
            )
        }
        // Tinggi bingkai bisa melebihi satu baris; requiredHeight membiarkannya meluap ke baris di sekitarnya.
        Spacer(
            Modifier
                .fillMaxWidth()
                .requiredHeight(with(density) { (fontPx * HEADER_FRAME_HEIGHT_EM).toDp() })
                .pointerInput(surah.id) { detectTapGestures { onClick() } }
                .graphicsLayer {
                    colorFilter = filter
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawBehind {
                    val frameWidth = HEADER_FRAME_EM * fontPx
                    val x = (size.width - frameWidth) / 2f - HEADER_FRAME_LEFT_EM * fontPx
                    scale(1f, HEADER_SCALE_Y, pivot = Offset(size.width / 2f, size.height / 2f)) {
                        drawText(layout, topLeft = Offset(x, size.height / 2f - layout.firstBaseline))
                    }
                },
        )
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
