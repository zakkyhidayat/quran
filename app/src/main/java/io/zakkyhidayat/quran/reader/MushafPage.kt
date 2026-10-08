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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
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
    ayahTexts: List<AyahText>,
    surahs: Map<Int, Surah>,
    selected: AyahRef?,
    onAyahClick: (AyahRef) -> Unit,
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

        ScreenReaderLayer(lines, ayahTexts, surahs, onAyahClick)

        // Glyph V4 berupa kode private-use: tidak berguna untuk pembaca layar, jadi disembunyikan dari semantics.
        Column(Modifier.fillMaxSize().clearAndSetSemantics { }) {
            Box(Modifier.height(topPadding))
            lines.forEach { line ->
                Box(Modifier.fillMaxWidth().height(lineHeight), contentAlignment = Alignment.Center) {
                    when (line.type) {
                        LineType.Ayah -> AyahLine(line, pageFont, glyphSize, glyphFilter, selected, onAyahClick)
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

// Satu simpul aksesibilitas per surah dan per ayat dengan teks Arab Unicode; ketuk dua kali membuka terjemahan.
@Composable
private fun ScreenReaderLayer(
    lines: List<PageLine>,
    ayahTexts: List<AyahText>,
    surahs: Map<Int, Surah>,
    onAyahClick: (AyahRef) -> Unit,
) {
    val byRef = remember(ayahTexts) { ayahTexts.associateBy { AyahRef(it.surah, it.ayah) } }
    val seen = HashSet<AyahRef>()
    Column {
        lines.forEach { line ->
            when (line.type) {
                LineType.SurahName -> surahs[line.surah]?.let { surah ->
                    Box(Modifier.size(1.dp).semantics { heading(); contentDescription = "Surah ${surah.nameLatin}" })
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
                LineType.Basmallah -> Unit
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
) {
    val style = TextStyle(fontFamily = font, fontSize = size)
    val highlight = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
    val density = LocalDensity.current
    val gapPx = with(density) { size.toPx() * 0.25f }
    val pad = with(density) { 3.dp.toPx() }
    val corner = with(density) { 8.dp.toPx() }
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
                    val left = bounds[2 * j] - pad
                    val right = bounds[2 * i + 1] + pad
                    drawRoundRect(highlight, Offset(left, 0f), Size(right - left, this.size.height), CornerRadius(corner))
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
