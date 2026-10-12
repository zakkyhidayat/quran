package io.zakkyhidayat.quran.explore

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import io.zakkyhidayat.quran.AppViewModel
import io.zakkyhidayat.quran.R
import io.zakkyhidayat.quran.data.LemmaCount
import io.zakkyhidayat.quran.data.RootAyah
import io.zakkyhidayat.quran.data.RootSummary
import io.zakkyhidayat.quran.ui.CenteredContent
import io.zakkyhidayat.quran.ui.segmentedItemColors

/** Daftar akar kata (huruf Arab dan jumlah kata) dengan pencarian huruf Arab atau transliterasi Latin. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RootsScreen(vm: AppViewModel, onBack: () -> Unit, onOpenRoot: (Int) -> Unit) {
    val textState = rememberTextFieldState()
    val query = textState.text.toString()
    val roots by produceState<List<RootSummary>?>(null) { value = vm.mushaf.roots() }
    val shown by remember(roots) { derivedStateOf { roots.orEmpty().filter { matchesRootQuery(query, it.arabic, it.latin) } } }
    Scaffold(
        topBar = { ExploreSearchTopBar(textState, stringResource(R.string.roots_search), onBack) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        CenteredContent(Modifier.padding(padding)) {
            when {
                roots == null -> CenterState(null)
                shown.isEmpty() -> CenterState(stringResource(R.string.explore_no_results))
                else -> LazyColumn(Modifier.fillMaxSize().imePadding(), contentPadding = ExploreListPadding, verticalArrangement = ExploreListSpacing) {
                    itemsIndexed(shown, key = { _, r -> r.id }) { index, r ->
                        SegmentedListItem(
                            onClick = { onOpenRoot(r.id) },
                            modifier = Modifier.clickLabel(stringResource(R.string.roots_open)),
                            shapes = ListItemDefaults.segmentedShapes(index, shown.size),
                            colors = segmentedItemColors(),
                            supportingContent = {
                                val words = stringResource(R.string.words_count, r.wordsCount)
                                Text(if (r.latin.isNullOrBlank()) words else "${r.latin} · $words")
                            },
                        ) { ArabicSnippet(r.arabic, style = MaterialTheme.typography.titleLarge, maxLines = 1) }
                    }
                }
            }
        }
    }
}

/** Satu akar: lema dengan jumlah kemunculan, lalu ayat-ayat tempat akar muncul (posisi kata disorot). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RootScreen(vm: AppViewModel, rootId: Int, onBack: () -> Unit, onOpenAyah: (Int, Int) -> Unit) {
    val root by produceState<RootSummary?>(null, rootId) { value = vm.mushaf.roots().firstOrNull { it.id == rootId } }
    val lemmas by produceState<List<LemmaCount>?>(null, rootId) { value = vm.mushaf.rootLemmas(rootId) }
    val ayahs by produceState<List<RootAyah>?>(null, rootId) { value = vm.mushaf.rootAyahs(rootId) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(root?.arabic.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) } },
            )
        },
    ) { padding ->
        CenteredContent(Modifier.padding(padding)) {
            val lemmaList = lemmas
            val ayahList = ayahs
            if (lemmaList == null || ayahList == null) CenterState(null)
            else LazyColumn(Modifier.fillMaxSize(), contentPadding = ExploreListPadding, verticalArrangement = ExploreListSpacing) {
                if (lemmaList.isNotEmpty()) {
                    item { SectionHeading(stringResource(R.string.roots_lemmas)) }
                    itemsIndexed(lemmaList, key = { _, l -> "l${l.id}" }) { index, l ->
                        // Lema hanya informasi (tidak bisa diketuk), jadi ListItem biasa, bukan item bersegmen yang interaktif.
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            modifier = Modifier.clip(MaterialTheme.shapes.medium),
                            headlineContent = { ArabicSnippet(l.text, maxLines = 1) },
                            trailingContent = { Text(stringResource(R.string.times_count, l.count), style = MaterialTheme.typography.labelLarge) },
                        )
                    }
                }
                item { SectionHeading(stringResource(R.string.roots_ayahs)) }
                if (ayahList.isEmpty()) item { Text(stringResource(R.string.explore_no_results)) }
                itemsIndexed(ayahList, key = { _, a -> "a${a.surah}:${a.ayah}" }) { index, a ->
                    AyahRow(vm, a.surah, a.ayah, index, ayahList.size, onClick = { onOpenAyah(a.surah, a.ayah) }, wordRanges = a.words.map { it..it })
                }
            }
        }
    }
}
