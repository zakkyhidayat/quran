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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.zakkyhidayat.quran.AppViewModel
import io.zakkyhidayat.quran.R
import io.zakkyhidayat.quran.data.SimilarAyah
import io.zakkyhidayat.quran.data.SimilarSource
import io.zakkyhidayat.quran.ui.CenteredContent

/** Ayat yang punya ayat serupa, terbanyak dulu; dicari lewat nama surah atau rujukan seperti "2:255". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimilarScreen(vm: AppViewModel, onBack: () -> Unit, onOpenSource: (Int, Int) -> Unit) {
    val textState = rememberTextFieldState()
    val query = textState.text.toString()
    val surahs by vm.surahs.collectAsStateWithLifecycle()
    val sources by produceState<List<SimilarSource>?>(null) { value = vm.mushaf.similarSources() }
    val shown by remember(sources, surahs) {
        derivedStateOf { sources.orEmpty().filter { matchesAyahQuery(query, it.surah, it.ayah, surahs[it.surah]?.nameLatin.orEmpty()) } }
    }
    Scaffold(
        topBar = { ExploreSearchTopBar(textState, stringResource(R.string.similar_search), onBack) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        CenteredContent(Modifier.padding(padding)) {
            when {
                sources == null -> CenterState(null)
                shown.isEmpty() -> CenterState(stringResource(R.string.explore_no_results))
                else -> LazyColumn(Modifier.fillMaxSize().imePadding(), contentPadding = ExploreListPadding, verticalArrangement = ExploreListSpacing) {
                    itemsIndexed(shown, key = { _, s -> "${s.surah}:${s.ayah}" }) { index, s ->
                        AyahRow(
                            vm, s.surah, s.ayah, index, shown.size,
                            onClick = { onOpenSource(s.surah, s.ayah) },
                            trailing = stringResource(R.string.matches_count, s.matches),
                            maxLines = 2,
                        )
                    }
                }
            }
        }
    }
}

/** Ayat sumber di atas, lalu ayat-ayat yang mirip dengan kata yang cocok disorot, beserta skor dan cakupannya. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimilarDetailScreen(vm: AppViewModel, surah: Int, ayah: Int, onBack: () -> Unit, onOpenAyah: (Int, Int) -> Unit) {
    val matches by produceState<List<SimilarAyah>?>(null, surah, ayah) { value = vm.mushaf.similarOf(surah, ayah) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.similar_title), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) } },
            )
        },
    ) { padding ->
        CenteredContent(Modifier.padding(padding)) {
            val list = matches
            if (list == null) CenterState(null)
            else LazyColumn(Modifier.fillMaxSize(), contentPadding = ExploreListPadding, verticalArrangement = ExploreListSpacing) {
                item { SectionHeading(stringResource(R.string.similar_source)) }
                item { AyahRow(vm, surah, ayah, 0, 1, onClick = { onOpenAyah(surah, ayah) }, maxLines = 6) }
                item { SectionHeading(stringResource(R.string.similar_matches)) }
                if (list.isEmpty()) item { Text(stringResource(R.string.explore_no_results)) }
                itemsIndexed(list, key = { _, m -> "${m.surah}:${m.ayah}" }) { index, m ->
                    AyahRow(
                        vm, m.surah, m.ayah, index, list.size,
                        onClick = { onOpenAyah(m.surah, m.ayah) },
                        // from_word/to_word menunjuk kata yang cocok di ayat yang mirip (bukan di ayat sumber).
                        wordRanges = if (m.fromWord != null && m.toWord != null) listOf(m.fromWord..m.toWord) else emptyList(),
                        trailing = stringResource(R.string.similar_score, m.score, m.coverage),
                        maxLines = 4,
                    )
                }
            }
        }
    }
}
