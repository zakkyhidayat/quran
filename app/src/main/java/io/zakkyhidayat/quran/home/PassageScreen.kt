package io.zakkyhidayat.quran.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.zakkyhidayat.quran.AppLanguage
import io.zakkyhidayat.quran.AppViewModel
import io.zakkyhidayat.quran.R
import io.zakkyhidayat.quran.data.AyahDetail
import io.zakkyhidayat.quran.data.AyahRange
import io.zakkyhidayat.quran.data.CollectionItem
import io.zakkyhidayat.quran.data.CollectionKind
import io.zakkyhidayat.quran.reader.arabicFontFamily
import io.zakkyhidayat.quran.reader.translationText
import io.zakkyhidayat.quran.settings.AppSettings
import io.zakkyhidayat.quran.ui.CenteredContent
import io.zakkyhidayat.quran.ui.LocalReadingTextScale
import io.zakkyhidayat.quran.ui.NumberBadge
import io.zakkyhidayat.quran.ui.scaled
import io.zakkyhidayat.quran.ui.segmentedItemColors

private val ListPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)

/** Daftar butir satu koleksi (misalnya "Doa dalam Al-Quran"); butir dibuka sebagai kumpulan ayat. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CollectionScreen(vm: AppViewModel, kind: CollectionKind, onBack: () -> Unit, onOpenPassage: (HomeTarget.Passage) -> Unit) {
    val context = LocalContext.current
    val items by produceState<List<CollectionItem>>(emptyList(), kind) {
        value = vm.collections.items(kind, AppLanguage.effective(context))
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(collectionTitle(kind)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) } },
            )
        },
    ) { padding ->
        CenteredContent(Modifier.padding(padding)) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = ListPadding, verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                itemsIndexed(items, key = { _, it -> it.index }) { index, item ->
                    SegmentedListItem(
                        onClick = { onOpenPassage(HomeTarget.Passage(item.title, item.description, item.refs)) },
                        shapes = ListItemDefaults.segmentedShapes(index, items.size),
                        colors = segmentedItemColors(),
                        leadingContent = { NumberBadge(index + 1) },
                        supportingContent = {
                            Text(
                                item.description ?: item.refs.replace(",", ", "),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    ) { Text(item.title) }
                }
            }
        }
    }
}

/**
 * Kumpulan ayat dari beberapa rentang (misalnya "2:285-286,2:255,112"): tiap rentang punya judul dengan tombol buka di
 * pembaca, lalu teks Arab dan terjemahan aktif per ayat.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassageScreen(vm: AppViewModel, target: HomeTarget.Passage, onBack: () -> Unit, onOpenReader: () -> Unit) {
    val surahs by vm.surahs.collectAsStateWithLifecycle()
    val settings by vm.settingsRepository.settings.collectAsStateWithLifecycle(AppSettings())
    // Rentang "seluruh surah" diisi jumlah ayatnya setelah daftar surah termuat.
    val ranges = remember(target.refs, surahs) {
        AyahRange.parseList(target.refs).map { r -> if (r.to == 0) r.copy(to = surahs[r.surah]?.ayahCount ?: 0) else r }.filter { it.to > 0 }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(target.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) } },
            )
        },
    ) { padding ->
        CenteredContent(Modifier.padding(padding)) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                target.subtitle?.let { note ->
                    item(key = "note") {
                        Text(note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
                    }
                }
                ranges.forEachIndexed { i, r ->
                    item(key = "r$i") {
                        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            val name = surahs[r.surah]?.nameLatin.orEmpty()
                            val label = if (r.from == r.to) "$name ${r.surah}:${r.from}" else "$name ${r.surah}:${r.from}-${r.to}"
                            Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                            FilledTonalButton(onClick = { vm.goToAyah(r.surah, r.from); onOpenReader() }) { Text(stringResource(R.string.open_in_reader)) }
                        }
                    }
                    items((r.from..r.to).toList(), key = { a -> "a$i-$a" }) { ayah ->
                        PassageAyah(vm, r.surah, ayah, settings.translationIds) { vm.goToAyah(r.surah, ayah); onOpenReader() }
                    }
                }
            }
        }
    }
}

@Composable
private fun PassageAyah(vm: AppViewModel, surah: Int, ayah: Int, translationIds: List<String>, onClick: () -> Unit) {
    val translations by vm.translations.collectAsStateWithLifecycle()
    val detail by produceState<AyahDetail?>(null, surah, ayah, translationIds, translations) {
        value = vm.mushaf.ayahDetail(surah, ayah, translationIds)
    }
    val context = LocalContext.current
    val arabicFont = remember { arabicFontFamily(context) }
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp)) {
        val d = detail
        if (d == null) {
            Spacer(Modifier.height(48.dp))
            return@Column
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Text(
                d.arabic,
                style = MaterialTheme.typography.headlineSmall.scaled(LocalReadingTextScale.current.arabic, 1.9f).copy(fontFamily = arabicFont),
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            d.translations.forEach { tr ->
                if (d.translations.size > 1) {
                    Text(tr.info.name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) { append("$ayah. ") }
                        append(translationText(tr.text, MaterialTheme.colorScheme.primary, MaterialTheme.typography.labelSmall.fontSize))
                    },
                    style = MaterialTheme.typography.bodyLarge.scaled(LocalReadingTextScale.current.translation, 1.5f),
                )
            }
        }
    }
    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}
