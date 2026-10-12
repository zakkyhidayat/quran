package io.zakkyhidayat.quran.explore

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.zakkyhidayat.quran.AppViewModel
import io.zakkyhidayat.quran.R
import io.zakkyhidayat.quran.data.AyahTheme
import io.zakkyhidayat.quran.ui.CenteredContent
import io.zakkyhidayat.quran.ui.NumberBadge
import io.zakkyhidayat.quran.ui.segmentedItemColors

/** Daftar surah dengan jumlah tema; bila ada kata pencarian, daftar berganti menjadi tema yang cocok di semua surah. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ThemesScreen(vm: AppViewModel, onBack: () -> Unit, onOpenSurah: (Int) -> Unit, onOpenAyah: (Int, Int) -> Unit) {
    val textState = rememberTextFieldState()
    val query = textState.text.toString()
    val surahs by vm.surahs.collectAsStateWithLifecycle()
    val themes by produceState<List<AyahTheme>?>(null) { value = vm.mushaf.allThemes() }
    val perSurah by remember(themes) { derivedStateOf { themes.orEmpty().groupingBy { it.surah }.eachCount().toList().sortedBy { it.first } } }
    val matches by remember(themes) {
        derivedStateOf {
            val q = query.trim()
            if (q.isEmpty()) emptyList() else themes.orEmpty().filter { it.theme.contains(q, ignoreCase = true) || it.keywords?.contains(q, ignoreCase = true) == true }
        }
    }
    val searching = query.isNotBlank()
    Scaffold(
        topBar = { ExploreSearchTopBar(textState, stringResource(R.string.themes_search), onBack) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        CenteredContent(Modifier.padding(padding)) {
            when {
                themes == null -> CenterState(null)
                searching && matches.isEmpty() -> CenterState(stringResource(R.string.explore_no_results))
                perSurah.isEmpty() -> CenterState(stringResource(R.string.explore_no_results))
                searching -> LazyColumn(Modifier.fillMaxSize().imePadding(), contentPadding = ExploreListPadding, verticalArrangement = ExploreListSpacing) {
                    itemsIndexed(matches, key = { _, t -> "${t.surah}:${t.ayahFrom}:${t.ayahTo}:${t.theme.hashCode()}" }) { index, t ->
                        ThemeRow(t, index, matches.size, surahName = surahs[t.surah]?.nameLatin) { onOpenAyah(t.surah, t.ayahFrom) }
                    }
                }
                else -> LazyColumn(Modifier.fillMaxSize().imePadding(), contentPadding = ExploreListPadding, verticalArrangement = ExploreListSpacing) {
                    itemsIndexed(perSurah, key = { _, p -> p.first }) { index, (surah, count) ->
                        val name = surahs[surah]?.nameLatin.orEmpty()
                        val countText = stringResource(R.string.themes_count, count)
                        SegmentedListItem(
                            onClick = { onOpenSurah(surah) },
                            modifier = Modifier.clickLabel(stringResource(R.string.themes_open_surah, name)),
                            shapes = ListItemDefaults.segmentedShapes(index, perSurah.size),
                            colors = segmentedItemColors(),
                            leadingContent = { NumberBadge(surah) },
                            supportingContent = { Text(countText) },
                        ) { Text(name) }
                    }
                }
            }
        }
    }
}

/** Tema satu surah; ketuk membuka pembaca di ayat awal tema. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahThemesScreen(vm: AppViewModel, surahId: Int, onBack: () -> Unit, onOpenAyah: (Int, Int) -> Unit) {
    val surahs by vm.surahs.collectAsStateWithLifecycle()
    val themes by produceState<List<AyahTheme>?>(null, surahId) { value = vm.mushaf.allThemes().filter { it.surah == surahId } }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(surahs[surahId]?.nameLatin.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) } },
            )
        },
    ) { padding ->
        CenteredContent(Modifier.padding(padding)) {
            val list = themes
            when {
                list == null -> CenterState(null)
                list.isEmpty() -> CenterState(stringResource(R.string.explore_no_results))
                else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = ExploreListPadding, verticalArrangement = ExploreListSpacing) {
                    itemsIndexed(list, key = { i, _ -> i }) { index, t -> ThemeRow(t, index, list.size, surahName = null) { onOpenAyah(t.surah, t.ayahFrom) } }
                }
            }
        }
    }
}

/** Baris tema: teks tema dan rentang ayat ("2:6–7"); [surahName] diisi bila baris tampil lintas surah. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ThemeRow(t: AyahTheme, index: Int, count: Int, surahName: String?, onClick: () -> Unit) {
    val range = formatAyahRange(t.surah, t.ayahFrom, t.ayahTo)
    SegmentedListItem(
        onClick = onClick,
        modifier = Modifier.clickLabel(stringResource(R.string.open_in_reader)),
        shapes = ListItemDefaults.segmentedShapes(index, count),
        colors = segmentedItemColors(),
        overlineContent = { Text(if (surahName == null) range else "$surahName $range") },
    ) { Text(t.theme, maxLines = 4, overflow = TextOverflow.Ellipsis) }
}
