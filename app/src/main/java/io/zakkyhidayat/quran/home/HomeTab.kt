package io.zakkyhidayat.quran.home

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.zakkyhidayat.quran.AppViewModel
import io.zakkyhidayat.quran.R
import io.zakkyhidayat.quran.data.AyahDetail
import io.zakkyhidayat.quran.data.AyahRange
import io.zakkyhidayat.quran.data.Collections
import io.zakkyhidayat.quran.data.CollectionKind
import io.zakkyhidayat.quran.data.SunnahReading
import io.zakkyhidayat.quran.index.ExploreCarousel
import io.zakkyhidayat.quran.index.ExploreEntry
import io.zakkyhidayat.quran.reader.arabicFontFamily
import io.zakkyhidayat.quran.settings.AppSettings
import io.zakkyhidayat.quran.ui.AppIcons
import io.zakkyhidayat.quran.ui.segmentedItemColors
import java.time.LocalDateTime
import kotlinx.coroutines.delay

/** Tujuan yang dibuka dari beranda: kumpulan ayat (judul + rujukan) atau daftar koleksi. */
sealed interface HomeTarget {
    data class Passage(val title: String, val subtitle: String?, val refs: String) : HomeTarget
    data class Collection(val kind: CollectionKind) : HomeTarget
    data class Sunnah(val reading: SunnahReading) : HomeTarget
}

private val ListPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp)
private const val HISTORY_SHOWN = 5

/** Beranda: ayat harian, histori bacaan, bacaan sunnah sesuai waktu, dan koleksi ayat. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeTab(
    vm: AppViewModel,
    onOpenReader: () -> Unit,
    onOpen: (HomeTarget) -> Unit,
    onOpenExplore: (ExploreEntry) -> Unit = {},
    onVotdGestureChanged: (Boolean) -> Unit = {},
) {
    val exploreCounts by vm.exploreCounts.collectAsStateWithLifecycle()
    val surahs by vm.surahs.collectAsStateWithLifecycle()
    val history by vm.history.entries.collectAsStateWithLifecycle()
    val now = remember { LocalDateTime.now() }
    val sunnah = remember(now) { SunnahReading.ordered(now) }
    val sunnahTitles = sunnah.map { sunnahTitle(it) }
    val sunnahNotes = sunnah.map { sunnahNote(it) }
    val collectionTitles = CollectionKind.entries.map { collectionTitle(it) }
    val historyShown = history.take(HISTORY_SHOWN)

    LazyColumn(Modifier.fillMaxSize(), contentPadding = ListPadding, verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        item(key = "votd") { VerseOfTheDay(vm, onOpenReader, onVotdGestureChanged) }

        item(key = "history_h") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionHeader(stringResource(R.string.home_history), Modifier.weight(1f))
                if (history.isNotEmpty()) TextButton(onClick = { vm.clearHistory() }) { Text(stringResource(R.string.home_history_clear)) }
            }
        }
        if (historyShown.isEmpty()) {
            item(key = "history_empty") {
                Text(
                    stringResource(R.string.home_history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                )
            }
        }
        itemsIndexed(historyShown, key = { _, h -> "h_${h.surah}" }) { index, h ->
            val surah = surahs[h.surah]
            SegmentedListItem(
                onClick = { vm.goToAyah(h.surah, h.ayah); onOpenReader() },
                shapes = ListItemDefaults.segmentedShapes(index, historyShown.size),
                colors = segmentedItemColors(),
                leadingContent = {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ) {
                        androidx.compose.foundation.layout.Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                            Icon(AppIcons.Schedule, contentDescription = null)
                        }
                    }
                },
                supportingContent = {
                    Text(
                        android.text.format.DateUtils.getRelativeTimeSpanString(h.readAt, System.currentTimeMillis(), android.text.format.DateUtils.MINUTE_IN_MILLIS).toString(),
                    )
                },
            ) { Text("${surah?.nameLatin.orEmpty()} ${h.surah}:${h.ayah}") }
        }

        exploreCounts?.takeIf { it.any }?.let { counts ->
            // Carousel meluber ke tepi layar: padding horizontal daftar dibatalkan, carousel punya padding sendiri.
            item(key = "explore") { ExploreCarousel(counts, onOpenExplore, Modifier.layout { m, c -> val w = c.maxWidth + 32.dp.roundToPx(); val p = m.measure(c.copy(minWidth = w, maxWidth = w)); layout(c.maxWidth, p.height) { p.place(-16.dp.roundToPx(), 0) } }.padding(top = 12.dp)) }
        }

        item(key = "sunnah_h") { SectionHeader(stringResource(R.string.home_sunnah)) }
        itemsIndexed(sunnah, key = { _, s -> "s_${s.name}" }) { index, s ->
            val nowLabel = stringResource(R.string.home_sunnah_now)
            val overline: (@Composable () -> Unit)? =
                if (s.time.isNow(now)) { { Text(nowLabel, color = MaterialTheme.colorScheme.primary) } } else null
            SegmentedListItem(
                onClick = { onOpen(HomeTarget.Sunnah(s)) },
                shapes = ListItemDefaults.segmentedShapes(index, sunnah.size),
                colors = segmentedItemColors(),
                leadingContent = {
                    val icon = if (s.time == io.zakkyhidayat.quran.data.SunnahTime.Night) AppIcons.DarkMode else AppIcons.MenuBook
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ) {
                        androidx.compose.foundation.layout.Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                            Icon(icon, contentDescription = null)
                        }
                    }
                },
                overlineContent = overline,
                supportingContent = { Text(sunnahNotes[index], maxLines = 2, overflow = TextOverflow.Ellipsis) },
            ) { Text(sunnahTitles[index]) }
        }

        item(key = "collections_h") { SectionHeader(stringResource(R.string.home_collections)) }
        // Satu item agar jarak antarbaris sama dengan jarak antarkolom (8 dp); di LazyColumn jarak antaritem hanya 2 dp.
        item(key = "collections") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CollectionKind.entries.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { kind ->
                            Card(
                                onClick = { onOpen(HomeTarget.Collection(kind)) },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                shape = MaterialTheme.shapes.large,
                                modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 112.dp),
                            ) {
                                Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    ) {
                                        androidx.compose.foundation.layout.Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                                            Icon(collectionIcon(kind), contentDescription = null)
                                        }
                                    }
                                    Text(collectionTitle(kind), style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 4.dp, top = 20.dp, bottom = 8.dp),
    )
}

/**
 * Tiga potongan harian kurasi yang dapat digeser, dibuka bertahap: pagi sejak 04.00, siang sejak 12.00, malam sejak 20.00.
 * Kartu yang belum waktunya tidak tampil; yang sudah lewat tetap bisa digeser ke belakang.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VerseOfTheDay(vm: AppViewModel, onOpenReader: () -> Unit, onGestureChanged: (Boolean) -> Unit) {
    val settings by vm.settingsRepository.settings.collectAsStateWithLifecycle(AppSettings())
    val surahs by vm.surahs.collectAsStateWithLifecycle()
    val translations by vm.translations.collectAsStateWithLifecycle()
    // Jam diperbarui tiap menit; yang dibaca hanya hasil turunannya, jadi beranda baru disusun ulang saat potongan berganti.
    val clock = remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            clock.value = LocalDateTime.now()
        }
    }
    val unlocked by remember { derivedStateOf { Collections.unlockedSlots(clock.value) } }
    val day by remember { derivedStateOf { Collections.slotDay(clock.value) } }
    val pool by produceState<List<AyahRange>>(emptyList()) {
        value = vm.collections.dailyPool()
    }
    val ranges = remember(pool, day) { Collections.picksForDay(pool, day) }
    val pagerState = rememberPagerState(initialPage = unlocked - 1) { unlocked }
    // Saat potongan baru terbuka dan aplikasi sedang dibuka, pager pindah ke potongan itu.
    var seenUnlocked by remember { mutableIntStateOf(unlocked) }
    LaunchedEffect(unlocked) {
        if (unlocked > seenUnlocked) pagerState.animateScrollToPage(unlocked - 1)
        seenUnlocked = unlocked
    }
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxWidth().height(300.dp).padding(top = 8.dp),
        key = { it },
    ) { page ->
        VerseOfTheDayCard(
            vm = vm,
            range = ranges.getOrNull(page),
            surahs = surahs,
            settings = settings,
            translations = translations,
            slotLabel = stringResource(DAILY_SLOT_LABELS[page]),
            page = page,
            total = unlocked,
            onOpenReader = onOpenReader,
            onGestureChanged = onGestureChanged,
        )
    }
}

private val DAILY_SLOT_LABELS = listOf(
    R.string.home_votd_morning,
    R.string.home_votd_afternoon,
    R.string.home_votd_night,
)

@Composable
private fun VerseOfTheDayCard(
    vm: AppViewModel,
    range: AyahRange?,
    surahs: Map<Int, io.zakkyhidayat.quran.data.Surah>,
    settings: AppSettings,
    translations: List<io.zakkyhidayat.quran.data.TranslationInfo>,
    slotLabel: String,
    page: Int,
    total: Int,
    onOpenReader: () -> Unit,
    onGestureChanged: (Boolean) -> Unit,
) {
    val onGestureChangedState = rememberUpdatedState(onGestureChanged)
    val ayahs by produceState<List<AyahDetail>>(emptyList(), range, settings.translationIds, translations) {
        val r = range ?: run {
            value = emptyList()
            return@produceState
        }
        value = (r.from..r.to).map { vm.mushaf.ayahDetail(r.surah, it, settings.translationIds.take(1)) }
    }
    val context = LocalContext.current
    val arabicFont = remember { arabicFontFamily(context) }
    val r = range
    val shape = MaterialTheme.shapes.extraLarge
    val container = MaterialTheme.colorScheme.primaryContainer
    val gradient = Brush.linearGradient(
        listOf(container, lerp(container, MaterialTheme.colorScheme.surfaceContainerHigh, 0.28f)),
    )
    Card(
        onClick = {
            if (r != null) { vm.goToAyah(r.surah, r.from); onOpenReader() }
        },
        enabled = r != null,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
        shape = shape,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    onGestureChangedState.value(true)
                    try {
                        waitForUpOrCancellation()
                    } finally {
                        onGestureChangedState.value(false)
                    }
                }
            },
    ) {
        Column(
            Modifier.fillMaxSize().background(gradient, shape).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.Favorite, contentDescription = null)
                Text(stringResource(R.string.home_votd), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 8.dp).weight(1f))
                Text(slotLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    r?.let { "${surahs[it.surah]?.nameLatin.orEmpty()} $it" }.orEmpty(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                // Nomor hanya tampil bila ada lebih dari satu potongan terbuka; kartu yang belum waktunya tidak dibocorkan.
                if (total > 1) {
                    Text("${page + 1}/$total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            if (ayahs.isNotEmpty()) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        ayahs.joinToString(" ") { it.arabic },
                        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = arabicFont, lineHeight = MaterialTheme.typography.bodyLarge.fontSize * 1.8f),
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val translation = ayahs.mapNotNull { a -> a.translations.firstOrNull()?.text?.let(::plainTranslation)?.takeIf { it.isNotBlank() } }
                if (translation.isNotEmpty()) {
                    Text(
                        translation.joinToString(" "),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Buang penanda catatan kaki (<sup>…</sup>) dan tag lain dari teks terjemahan. */
internal fun plainTranslation(raw: String): String =
    raw.replace(Regex("<sup[^>]*>.*?</sup>"), "").replace(Regex("<[^>]+>"), "").replace(Regex("\\s+"), " ").trim()

@Composable
internal fun sunnahTitle(s: SunnahReading): String = stringResource(
    when (s) {
        SunnahReading.KahfFriday -> R.string.sunnah_kahf_friday
        SunnahReading.MulkSajdah -> R.string.sunnah_mulk_sajdah
        SunnahReading.Kursi -> R.string.sunnah_kursi
        SunnahReading.BaqarahEnd -> R.string.sunnah_baqarah_end
        SunnahReading.ThreeQuls -> R.string.sunnah_three_quls
        SunnahReading.KahfFirstTen -> R.string.sunnah_kahf_ten
    },
)

@Composable
internal fun sunnahNote(s: SunnahReading): String = stringResource(
    when (s) {
        SunnahReading.KahfFriday -> R.string.sunnah_kahf_friday_note
        SunnahReading.MulkSajdah -> R.string.sunnah_mulk_sajdah_note
        SunnahReading.Kursi -> R.string.sunnah_kursi_note
        SunnahReading.BaqarahEnd -> R.string.sunnah_baqarah_end_note
        SunnahReading.ThreeQuls -> R.string.sunnah_three_quls_note
        SunnahReading.KahfFirstTen -> R.string.sunnah_kahf_ten_note
    },
)

@Composable
internal fun collectionTitle(kind: CollectionKind): String = stringResource(
    when (kind) {
        CollectionKind.Dua -> R.string.collection_dua
        CollectionKind.Solution -> R.string.collection_solution
        CollectionKind.Etiquette -> R.string.collection_etiquette
        CollectionKind.MajorSins -> R.string.collection_major_sins
    },
)

private fun collectionIcon(kind: CollectionKind) = when (kind) {
    CollectionKind.Dua -> AppIcons.Favorite
    CollectionKind.Solution -> AppIcons.AutoAwesome
    CollectionKind.Etiquette -> AppIcons.Notes
    CollectionKind.MajorSins -> AppIcons.Warning
}
