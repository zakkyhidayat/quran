package io.zakkyhidayat.quran.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import io.zakkyhidayat.quran.reader.arabicFontFamily
import io.zakkyhidayat.quran.settings.AppSettings
import io.zakkyhidayat.quran.ui.AppIcons
import io.zakkyhidayat.quran.ui.segmentedItemColors
import java.time.LocalDate
import java.time.LocalDateTime

/** Tujuan yang dibuka dari beranda: kumpulan ayat (judul + rujukan) atau daftar koleksi. */
sealed interface HomeTarget {
    data class Passage(val title: String, val subtitle: String?, val refs: String) : HomeTarget
    data class Collection(val kind: CollectionKind) : HomeTarget
}

private val ListPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp)
private const val HISTORY_SHOWN = 5

/** Beranda: ayat hari ini, bacaan sunnah sesuai waktu, histori bacaan, dan koleksi ayat. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeTab(vm: AppViewModel, onOpenReader: () -> Unit, onOpen: (HomeTarget) -> Unit) {
    val surahs by vm.surahs.collectAsStateWithLifecycle()
    val history by vm.history.entries.collectAsStateWithLifecycle()
    val now = remember { LocalDateTime.now() }
    val sunnah = remember(now) { SunnahReading.ordered(now) }
    val sunnahTitles = sunnah.map { sunnahTitle(it) }
    val sunnahNotes = sunnah.map { sunnahNote(it) }
    val collectionTitles = CollectionKind.entries.map { collectionTitle(it) }
    val historyShown = history.take(HISTORY_SHOWN)

    LazyColumn(Modifier.fillMaxSize(), contentPadding = ListPadding, verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        item(key = "votd") { VerseOfTheDay(vm, onOpenReader) }

        item(key = "sunnah_h") { SectionHeader(stringResource(R.string.home_sunnah)) }
        itemsIndexed(sunnah, key = { _, s -> "s_${s.name}" }) { index, s ->
            val nowLabel = stringResource(R.string.home_sunnah_now)
            val overline: (@Composable () -> Unit)? =
                if (s.time.isNow(now)) { { Text(nowLabel, color = MaterialTheme.colorScheme.primary) } } else null
            SegmentedListItem(
                onClick = { onOpen(HomeTarget.Passage(sunnahTitles[index], sunnahNotes[index], s.refs)) },
                shapes = ListItemDefaults.segmentedShapes(index, sunnah.size),
                colors = segmentedItemColors(),
                leadingContent = { Icon(if (s.time == io.zakkyhidayat.quran.data.SunnahTime.Night) AppIcons.DarkMode else AppIcons.MenuBook, contentDescription = null) },
                overlineContent = overline,
                supportingContent = { Text(sunnahNotes[index], maxLines = 2, overflow = TextOverflow.Ellipsis) },
            ) { Text(sunnahTitles[index]) }
        }

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
                leadingContent = { Icon(AppIcons.Schedule, contentDescription = null) },
                supportingContent = {
                    Text(
                        android.text.format.DateUtils.getRelativeTimeSpanString(h.readAt, System.currentTimeMillis(), android.text.format.DateUtils.MINUTE_IN_MILLIS).toString(),
                    )
                },
            ) { Text("${surah?.nameLatin.orEmpty()} ${h.surah}:${h.ayah}") }
        }

        item(key = "collections_h") { SectionHeader(stringResource(R.string.home_collections)) }
        itemsIndexed(CollectionKind.entries, key = { _, k -> "c_${k.name}" }) { index, kind ->
            SegmentedListItem(
                onClick = { onOpen(HomeTarget.Collection(kind)) },
                shapes = ListItemDefaults.segmentedShapes(index, CollectionKind.entries.size),
                colors = segmentedItemColors(),
                leadingContent = { Icon(collectionIcon(kind), contentDescription = null) },
            ) { Text(collectionTitles[index]) }
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
 * Ayat hari ini: potongan 1-4 ayat dari koleksi kurasi (solusi, doa, adab), sama untuk semua pengguna pada hari yang
 * sama, berganti tiap tengah malam. Bukan ayat acak, supaya maknanya tidak terputus dari ayat sebelum/sesudahnya.
 */
@Composable
private fun VerseOfTheDay(vm: AppViewModel, onOpenReader: () -> Unit) {
    val settings by vm.settingsRepository.settings.collectAsStateWithLifecycle(AppSettings())
    val surahs by vm.surahs.collectAsStateWithLifecycle()
    val translations by vm.translations.collectAsStateWithLifecycle()
    val range by produceState<AyahRange?>(null) {
        value = Collections.pickForDay(vm.collections.dailyPool(), LocalDate.now())
    }
    val ayahs by produceState<List<AyahDetail>>(emptyList(), range, settings.translationIds, translations) {
        val r = range ?: return@produceState
        value = (r.from..r.to).map { vm.mushaf.ayahDetail(r.surah, it, settings.translationIds.take(1)) }
    }
    val context = LocalContext.current
    val arabicFont = remember { arabicFontFamily(context) }
    val r = range
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable(enabled = r != null) {
            if (r != null) { vm.goToAyah(r.surah, r.from); onOpenReader() }
        },
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.home_votd), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 8.dp).weight(1f))
                if (r != null) {
                    Text(
                        "${surahs[r.surah]?.nameLatin.orEmpty()} $r",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (ayahs.isNotEmpty()) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        ayahs.joinToString(" ") { it.arabic },
                        style = MaterialTheme.typography.headlineSmall.copy(fontFamily = arabicFont, lineHeight = MaterialTheme.typography.headlineSmall.fontSize * 1.9f),
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                val translation = ayahs.mapNotNull { a -> a.translations.firstOrNull()?.text?.let(::plainTranslation)?.takeIf { it.isNotBlank() } }
                if (translation.isNotEmpty()) {
                    Text(translation.joinToString(" "), style = MaterialTheme.typography.bodyLarge)
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
