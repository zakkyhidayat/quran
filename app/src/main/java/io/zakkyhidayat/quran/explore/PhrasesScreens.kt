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
import androidx.compose.material3.ListItemDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import io.zakkyhidayat.quran.AppViewModel
import io.zakkyhidayat.quran.R
import io.zakkyhidayat.quran.data.ArabicText
import io.zakkyhidayat.quran.data.PhraseAyah
import io.zakkyhidayat.quran.data.PhraseSummary
import io.zakkyhidayat.quran.ui.CenteredContent
import io.zakkyhidayat.quran.ui.segmentedItemColors

/** Daftar frasa mutasyabihat (frasa Arab dan jumlah ayatnya) dengan pencarian teks Arab. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PhrasesScreen(vm: AppViewModel, onBack: () -> Unit, onOpenPhrase: (Int) -> Unit) {
    val textState = rememberTextFieldState()
    val query = textState.text.toString()
    val phrases by produceState<List<PhraseSummary>?>(null) { value = vm.mushaf.phrases() }
    val shown by remember(phrases) {
        derivedStateOf {
            val q = ArabicText.normalize(query.trim())
            if (q.isEmpty()) phrases.orEmpty() else phrases.orEmpty().filter { ArabicText.normalize(it.phrase).contains(q) }
        }
    }
    Scaffold(
        topBar = { ExploreSearchTopBar(textState, stringResource(R.string.phrases_search), onBack) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        CenteredContent(Modifier.padding(padding)) {
            when {
                phrases == null -> CenterState(null)
                shown.isEmpty() -> CenterState(stringResource(R.string.explore_no_results))
                else -> LazyColumn(Modifier.fillMaxSize().imePadding(), contentPadding = ExploreListPadding, verticalArrangement = ExploreListSpacing) {
                    itemsIndexed(shown, key = { _, p -> p.id }) { index, p ->
                        SegmentedListItem(
                            onClick = { onOpenPhrase(p.id) },
                            modifier = Modifier.clickLabel(stringResource(R.string.phrases_open)),
                            shapes = ListItemDefaults.segmentedShapes(index, shown.size),
                            colors = segmentedItemColors(),
                            supportingContent = { Text(stringResource(R.string.topic_ayah_count, p.ayahCount)) },
                        ) {
                            // Frasa kosong bila pemotongan kata tak dapat dipastikan: tampilkan letak sumbernya.
                            if (p.phrase.isEmpty()) Text("${p.surah}:${p.ayah} (${p.fromWord}–${p.toWord})")
                            else ArabicSnippet(p.phrase, maxLines = 2)
                        }
                    }
                }
            }
        }
    }
}

/** Semua ayat yang memuat satu frasa, kata frasa disorot. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhraseScreen(vm: AppViewModel, phraseId: Int, onBack: () -> Unit, onOpenAyah: (Int, Int) -> Unit) {
    val phrase by produceState<PhraseSummary?>(null, phraseId) { value = vm.mushaf.phrases().firstOrNull { it.id == phraseId } }
    val ayahs by produceState<List<PhraseAyah>?>(null, phraseId) { value = vm.mushaf.phraseAyahs(phraseId) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.phrase_title), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) } },
            )
        },
    ) { padding ->
        CenteredContent(Modifier.padding(padding)) {
            val list = ayahs
            when {
                list == null -> CenterState(null)
                list.isEmpty() -> CenterState(stringResource(R.string.explore_no_results))
                else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = ExploreListPadding, verticalArrangement = ExploreListSpacing) {
                    phrase?.takeIf { it.phrase.isNotEmpty() }?.let { p ->
                        item { ArabicSnippet(p.phrase, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium) }
                    }
                    item { SectionHeading(stringResource(R.string.topic_ayah_count, list.size)) }
                    itemsIndexed(list, key = { _, a -> "${a.surah}:${a.ayah}" }) { index, a ->
                        AyahRow(vm, a.surah, a.ayah, index, list.size, onClick = { onOpenAyah(a.surah, a.ayah) }, wordRanges = listOf(a.fromWord..a.toWord))
                    }
                }
            }
        }
    }
}
