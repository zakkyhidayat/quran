package io.zakkyhidayat.quran.reader

import androidx.compose.ui.platform.LocalResources
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.unit.DpSize
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.res.stringResource
import io.zakkyhidayat.quran.R
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
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
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
internal fun commonFontFamily(context: Context): FontFamily =
    FontFamily(Font("fonts/quran-common.ttf", context.assets))

internal fun juzTitleGlyph(juz: Int): String = (0xE001 + juz - 1).toChar().toString()

internal fun juzOpeningGlyph(juz: Int): String = (0xE900 + juz - 1).toChar().toString()

internal fun surahNameFontFamily(context: Context): FontFamily =
    FontFamily(Font("fonts/surah_names.ttf", context.assets))

internal suspend fun surahHeaderFontFamily(context: Context, dark: Boolean): FontFamily =
    // Mode gelap: palet 1 dengan isian bingkai (warna 18, bawaannya hitam) diganti hijau tua (warna 12) agar serasi dengan nomor ayat.
    if (dark) PalettedFonts.load(context, "QCF_SurahHeader_COLOR-Regular.ttf", 1, listOf(18 to 12))
    else PalettedFonts.load(context, "QCF_SurahHeader_COLOR-Regular.ttf", 0)

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
    /** Ukuran halaman; diberikan pemanggil (sudah dihitungnya) agar tidak perlu sub-komposisi BoxWithConstraints. */
    pageSize: DpSize,
    modifier: Modifier = Modifier,
    tajweed: Boolean = true,
    onSurahClick: (Int) -> Unit = {},
) {
    val context = LocalContext.current
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val palette = GlyphPalette.of(tajweed, dark)
    // Semua font dimuat di thread IO; selama belum siap (jarang, karena dipanggil lebih dulu oleh prefetch) halaman kosong.
    val pageFont by produceState(cachedPageFont(page, palette), page, palette) { value = pageFontFamily(context, page, palette) }
    val headerFont by produceState<FontFamily?>(null, dark) { value = surahHeaderFontFamily(context, dark) }
    val basmalahFont by produceState(cachedPageFont(1, palette), palette) { value = pageFontFamily(context, 1, palette) }
    val font = pageFont
    val header = headerFont
    val basmalah = basmalahFont
    if (font == null || header == null || basmalah == null) {
        Box(modifier.fillMaxSize())
        return
    }
    val density = LocalDensity.current
    val resolver = LocalFontFamilyResolver.current

    val screenReader = rememberTouchExplorationEnabled()
    Box(modifier.size(pageSize)) {
        val maxHeight = pageSize.height
        val widthPx = with(density) { pageSize.width.roundToPx() }
        val nominal = maxHeight / LINES_PER_PAGE
        // Semua pengukuran teks (ukuran glyph, ~130 kata) dikerjakan di thread latar belakang; thread utama hanya
        // menggambar hasilnya. Halaman tetangga disusun lebih dulu oleh pager, jadi biasanya siap sebelum terlihat.
        val prepared by produceState<PreparedPage?>(null, page, lines, widthPx, font, basmalah, header, density, surahs) {
            value = withContext(Dispatchers.Default) {
                preparePage(TextMeasurer(resolver, density, LayoutDirection.Ltr, cacheSize = 0), density, lines, font, basmalah, header, surahs, widthPx)
            }
        }
        val ready = prepared ?: return@Box
        // Baris bingkai surah lebih tinggi dari baris ayat (bingkai tidak diubah proporsinya); baris ayat di halaman itu
        // dirapatkan secukupnya agar total tetap seukuran halaman.
        val headerLineHeight = with(density) { (widthPx * HEADER_WIDTH_RATIO * HEADER_FRAME_HEIGHT_EM / HEADER_FRAME_EM * HEADER_LINE_RATIO).toDp() }
        val headerCount = lines.count { it.type == LineType.SurahName }
        val otherCount = lines.size - headerCount
        val lineHeight = if (headerCount == 0) nominal else minOf(nominal, (maxHeight - headerLineHeight * headerCount) / otherCount)
        val topPadding = (maxHeight - lineHeight * otherCount - headerLineHeight * headerCount).coerceAtLeast(0.dp) / 2f

        // Lapisan pembaca layar (satu node per ayat) hanya disusun saat TalkBack aktif: tanpa itu tidak ada gunanya, dan
        // menyusunnya untuk setiap halaman tetangga menambah kerja saat geser.
        if (screenReader) ScreenReaderLayer(lines, ayahTexts, surahs, onAyahClick, onSurahClick)

        // Glyph V4 berupa kode private-use: tidak berguna untuk pembaca layar, jadi disembunyikan dari semantics.
        Column(Modifier.fillMaxSize().clearAndSetSemantics { }) {
            Box(Modifier.height(topPadding))
            lines.forEach { line ->
                val thisHeight = if (line.type == LineType.SurahName) headerLineHeight else lineHeight
                Box(Modifier.fillMaxWidth().height(thisHeight), contentAlignment = Alignment.Center) {
                    when (line.type) {
                        LineType.Ayah -> AyahLine(line, ready.words[line] ?: emptyList(), ready.gapPx, selected, onAyahClick, thisHeight)
                        LineType.SurahName -> ready.headers[line]?.let { SurahHeader(it, widthPx) { onSurahClick(line.surah!!) } }
                        LineType.Basmallah -> GlyphRow(ready.basmalah, centered = true, minGapPx = ready.gapPx, bounds = remember { FloatArray(ready.basmalah.size * 2) })
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
    val resources = LocalResources.current
    val byRef = remember(ayahTexts) { ayahTexts.associateBy { AyahRef(it.surah, it.ayah) } }
    val seen = HashSet<AyahRef>()
    Column {
        lines.forEach { line ->
            when (line.type) {
                LineType.SurahName -> surahs[line.surah]?.let { surah ->
                    Box(
                        Modifier.size(1.dp).semantics {
                            heading()
                            contentDescription = resources.getString(R.string.surah_cd, surah.nameLatin)
                            onClick(label = resources.getString(R.string.open_surah_info)) { onSurahClick(surah.id); true }
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
                                contentDescription = resources.getString(R.string.ayah_cd, name, ref.ayah, text.text)
                                onClick(label = resources.getString(R.string.show_translation)) { onAyahClick(ref); true }
                            },
                        )
                    }
                }
                LineType.Basmallah -> Box(Modifier.size(1.dp).semantics { contentDescription = "Bismillahirrahmanirrahim" })
            }
        }
    }
}



// Basmalah memakai glyph ayat 1:1 dari font halaman 1 (U+FC41..FC44 = bismi / Allahi / alrrahmani / alrraheemi),
// sehingga gaya dan warna tajwidnya sama dengan teks ayat di halaman.
private const val BASMALAH_GLYPHS = "ﱁﱂﱃﱄ"


/** Hasil persiapan satu halaman: tata letak tiap kata per baris ayat, glyph basmalah, dan jarak minimum antarkata. */
private class PreparedPage(
    val words: Map<PageLine, List<TextLayoutResult>>,
    val basmalah: List<TextLayoutResult>,
    val headers: Map<PageLine, TextLayoutResult>,
    val gapPx: Float,
)

private fun preparePage(
    measurer: TextMeasurer,
    density: Density,
    lines: List<PageLine>,
    font: FontFamily,
    basmalahFont: FontFamily,
    headerFont: FontFamily,
    surahs: Map<Int, Surah>,
    widthPx: Int,
): PreparedPage {
    val size = fitFontSize(measurer, density, lines, font, widthPx)
    val style = TextStyle(fontFamily = font, fontSize = size)
    fun measure(text: String, style: TextStyle) =
        measurer.measure(text = text, style = style, softWrap = false, maxLines = 1, constraints = Constraints())
    val words = lines.filter { it.type == LineType.Ayah }.associateWith { line -> line.words.map { measure(it.text, style) } }
    val basmalahStyle = TextStyle(fontFamily = basmalahFont, fontSize = size)
    val basmalah = BASMALAH_GLYPHS.map { measure(it.toString(), basmalahStyle) }
    val headerStyle = TextStyle(fontFamily = headerFont, fontSize = with(density) { headerFontPx(widthPx).toSp() })
    val headers = lines.filter { it.type == LineType.SurahName }.mapNotNull { line ->
        surahs[line.surah]?.let { line to measure(it.nameGlyph.toString(), headerStyle) }
    }.toMap()
    return PreparedPage(words, basmalah, headers, with(density) { size.toPx() * 0.25f })
}

/**
 * Satu baris glyph yang digambar langsung di kanvas (bukan satu Text per kata): kanan ke kiri, baris penuh dibagi rata,
 * baris pendek ditengahkan dengan jarak tetap. [bounds] diisi batas kiri/kanan tiap kata untuk sorotan dan ketukan.
 */
@Composable
private fun GlyphRow(
    layouts: List<TextLayoutResult>,
    centered: Boolean,
    minGapPx: Float,
    bounds: FloatArray,
    modifier: Modifier = Modifier,
    behind: DrawScope.() -> Unit = {},
) {
    val height = layouts.maxOfOrNull { it.size.height } ?: 0
    Spacer(
        modifier
            .fillMaxWidth()
            .height(with(LocalDensity.current) { height.toDp() })
            .drawBehind {
                val width = size.width
                val total = layouts.sumOf { it.size.width }.toFloat()
                val count = layouts.size
                val gap = if (centered || count < 2) minGapPx else ((width - total) / (count - 1)).coerceAtLeast(0f)
                val used = total + gap * (count - 1).coerceAtLeast(0)
                var x = if (centered) width - (width - used) / 2f else width
                layouts.forEachIndexed { i, layout ->
                    x -= layout.size.width
                    val left = x.roundToInt().toFloat()
                    bounds[2 * i] = left
                    bounds[2 * i + 1] = left + layout.size.width
                    x -= gap
                }
                behind()
                layouts.forEachIndexed { i, layout ->
                    drawText(layout, topLeft = Offset(bounds[2 * i], (size.height - layout.size.height) / 2f))
                }
            },
    )
}

@Composable
private fun AyahLine(
    line: PageLine,
    layouts: List<TextLayoutResult>,
    gapPx: Float,
    selected: AyahRef?,
    onAyahClick: (AyahRef) -> Unit,
    cellHeight: Dp,
) {
    // Abu-abu netral (warna teks di atas latar), bukan warna tema: warna tajwid (hijau, merah, biru) tetap terbaca.
    // Terang: 10% (lebih pucat); gelap: 18% (lebih terang). Buram (sudah dicampur dengan latar) supaya tumpang tindih
    // antar baris tidak tampak sebagai garis lebih gelap.
    val highlightAlpha = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) 0.18f else 0.10f
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = highlightAlpha).compositeOver(MaterialTheme.colorScheme.background)
    val density = LocalDensity.current
    val pad = with(density) { 2.dp.toPx() }
    val corner = with(density) { 8.dp.toPx() }
    // Tinggi sorotan = satu sel baris penuh; tumpang tindih 1 px di tiap sisi menutup sambungan antarbaris.
    val highlightHeight = with(density) { cellHeight.toPx() }
    val words = line.words
    val isSelected = remember(line, selected) {
        BooleanArray(words.size) { i -> selected != null && words[i].surah == selected.surah && words[i].ayah == selected.ayah }
    }
    val bounds = remember(line) { FloatArray(words.size * 2) }

    GlyphRow(
        layouts = layouts,
        centered = line.centered,
        minGapPx = gapPx,
        bounds = bounds,
        modifier = Modifier.pointerInput(line) {
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
        var i = 0
        while (i < isSelected.size) {
            if (!isSelected[i]) { i++; continue }
            var j = i
            while (j + 1 < isSelected.size && isSelected[j + 1]) j++
            // Teks berjalan kanan ke kiri: kata pertama paling kanan. Sisi kiri tanpa pad: di sana biasanya kata pertama
            // ayat berikutnya, jangan sampai tertutup sorotan.
            val left = bounds[2 * j]
            val right = bounds[2 * i + 1] + pad
            val height = minOf(highlightHeight, size.height)
            drawRoundRect(highlight, Offset(left, (size.height - height) / 2f - 1f), Size(right - left, height + 2f), CornerRadius(corner))
            i = j + 1
        }
    }
}

// Bingkai header berwarna (font QCF_SurahHeader): lebar 3,267 em dan tinggi 0,41 em, simetris terhadap baseline.
private const val HEADER_UPEM = 2500f
private const val HEADER_FRAME_EM = 8167f / HEADER_UPEM
private const val HEADER_FRAME_LEFT_EM = 41f / HEADER_UPEM
private const val HEADER_FRAME_HEIGHT_EM = 1026f / HEADER_UPEM

// Bingkai diskalakan vertikal supaya ada jarak dengan baris ayat di atas dan basmalah di bawahnya.
// Lebar bingkai 94% lebar halaman; baris bingkai 112% tinggi bingkai (jarak ~6% di atas dan bawah). Proporsi bingkai tetap asli.
private const val HEADER_WIDTH_RATIO = 0.94f
private const val HEADER_LINE_RATIO = 1.12f

private fun headerFontPx(widthPx: Int): Float = widthPx * HEADER_WIDTH_RATIO / HEADER_FRAME_EM

@Composable
private fun SurahHeader(layout: TextLayoutResult, widthPx: Int, onClick: () -> Unit) {
    val density = LocalDensity.current
    val fontPx = headerFontPx(widthPx)
    // Tinggi bingkai bisa melebihi satu baris; requiredHeight membiarkannya meluap ke baris di sekitarnya.
    Spacer(
        Modifier
            .fillMaxWidth()
            .requiredHeight(with(density) { (fontPx * HEADER_FRAME_HEIGHT_EM).toDp() })
            .pointerInput(layout) { detectTapGestures { onClick() } }
            .drawBehind {
                val frameWidth = HEADER_FRAME_EM * fontPx
                val x = (size.width - frameWidth) / 2f - HEADER_FRAME_LEFT_EM * fontPx
                drawText(layout, topLeft = Offset(x, size.height / 2f - layout.firstBaseline))
            },
    )
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

/** true selama layanan eksplorasi sentuh (TalkBack dan sejenisnya) aktif; ikut berubah saat dinyalakan/dimatikan. */
@Composable
private fun rememberTouchExplorationEnabled(): Boolean {
    val context = LocalContext.current
    val manager = remember { context.getSystemService(android.view.accessibility.AccessibilityManager::class.java) }
    var enabled by remember { mutableStateOf(manager.isTouchExplorationEnabled) }
    DisposableEffect(manager) {
        val listener = android.view.accessibility.AccessibilityManager.TouchExplorationStateChangeListener { enabled = it }
        manager.addTouchExplorationStateChangeListener(listener)
        onDispose { manager.removeTouchExplorationStateChangeListener(listener) }
    }
    return enabled
}
