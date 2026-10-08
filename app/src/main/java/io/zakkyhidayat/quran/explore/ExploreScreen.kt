package io.zakkyhidayat.quran.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.zakkyhidayat.quran.AppViewModel
import io.zakkyhidayat.quran.R
import io.zakkyhidayat.quran.data.AyahRef
import io.zakkyhidayat.quran.data.Topic
import io.zakkyhidayat.quran.ui.CenteredContent
import io.zakkyhidayat.quran.ui.segmentedItemColors

private val ListPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)

/** Daftar topik dan konsep dengan pencarian nama (Latin, Arab, atau deskripsi). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExploreScreen(vm: AppViewModel, onBack: () -> Unit, onOpenTopic: (Int) -> Unit) {
    val textState = rememberTextFieldState()
    val searchBarState = androidx.compose.material3.rememberSearchBarState()
    val query = textState.text.toString()
    val topics by produceState<List<Topic>?>(null) { value = vm.mushaf.topics() }
    val shown by remember(topics) {
        derivedStateOf {
            val q = query.trim().lowercase()
            val all = topics.orEmpty()
            if (q.isEmpty()) all else all.filter {
                it.name.lowercase().contains(q) || it.nameAr?.contains(q) == true || it.description?.lowercase()?.contains(q) == true
            }
        }
    }
    Scaffold(
        topBar = {
            Surface(
                shape = SearchBarDefaults.inputFieldShape,
                color = SearchBarDefaults.colors().containerColor,
                tonalElevation = SearchBarDefaults.TonalElevation,
                modifier = Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
            ) {
                SearchBarDefaults.InputField(
                    searchBarState = searchBarState,
                    textFieldState = textState,
                    onSearch = {},
                    placeholder = { Text(stringResource(R.string.explore_search)) },
                    leadingIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) } },
                    trailingIcon = {
                        if (query.isNotEmpty()) IconButton(onClick = { textState.clearText() }) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear)) }
                    },
                )
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        CenteredContent(Modifier.padding(padding)) {
            if (topics != null && shown.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.explore_empty), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(Modifier.fillMaxSize().imePadding(), contentPadding = ListPadding, verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                    itemsIndexed(shown, key = { _, t -> t.id }) { index, t -> TopicRow(t, index, shown.size) { onOpenTopic(t.id) } }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun TopicRow(t: Topic, index: Int, count: Int, onClick: () -> Unit) {
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index, count),
        colors = segmentedItemColors(),
        supportingContent = { Text(stringResource(R.string.topic_ayah_count, t.ayahCount)) },
        trailingContent = t.nameAr?.let { { Text(it, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) } },
    ) { Text(t.name, maxLines = 2, overflow = TextOverflow.Ellipsis) }
}

/** Satu topik: deskripsi, subtopik, dan ayat-ayatnya; ketuk ayat membuka pembaca di ayat itu. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TopicScreen(vm: AppViewModel, topicId: Int, onBack: () -> Unit, onOpenTopic: (Int) -> Unit, onOpenAyah: (Int, Int) -> Unit) {
    val surahs by vm.surahs.collectAsStateWithLifecycle()
    var loaded by remember(topicId) { mutableStateOf(false) }
    var topic by remember(topicId) { mutableStateOf<Topic?>(null) }
    var children by remember(topicId) { mutableStateOf(emptyList<Topic>()) }
    var related by remember(topicId) { mutableStateOf(emptyList<Topic>()) }
    var ayahs by remember(topicId) { mutableStateOf(emptyList<AyahRef>()) }
    LaunchedEffect(topicId) {
        topic = vm.mushaf.topic(topicId)
        children = vm.mushaf.topicChildren(topicId)
        related = vm.mushaf.relatedTopics(topicId)
        ayahs = vm.mushaf.topicAyahs(topicId)
        loaded = true
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(topic?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) } },
            )
        },
    ) { padding ->
        CenteredContent(Modifier.padding(padding)) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = ListPadding, verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                topic?.description?.let { desc ->
                    item { Text(desc, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 12.dp)) }
                }
                if (children.isNotEmpty()) {
                    item { SectionTitle(stringResource(R.string.topic_subtopics)) }
                    itemsIndexed(children, key = { _, t -> "c${t.id}" }) { index, t -> TopicRow(t, index, children.size) { onOpenTopic(t.id) } }
                }
                if (related.isNotEmpty()) {
                    item { SectionTitle(stringResource(R.string.topic_related)) }
                    itemsIndexed(related, key = { _, t -> "r${t.id}" }) { index, t -> TopicRow(t, index, related.size) { onOpenTopic(t.id) } }
                }
                if (ayahs.isNotEmpty()) {
                    item { SectionTitle(stringResource(R.string.topic_ayahs)) }
                    itemsIndexed(ayahs, key = { _, a -> "a${a.surah}:${a.ayah}" }) { index, a ->
                        val preview by produceState("", a) { value = vm.mushaf.ayahArabic(a.surah, a.ayah) }
                        SegmentedListItem(
                            onClick = { onOpenAyah(a.surah, a.ayah) },
                            shapes = ListItemDefaults.segmentedShapes(index, ayahs.size),
                            colors = segmentedItemColors(),
                            supportingContent = { Text(preview, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        ) { Text("${surahs[a.surah]?.nameLatin.orEmpty()} ${a.surah}:${a.ayah}") }
                    }
                }
                if (loaded && ayahs.isEmpty() && children.isEmpty()) {
                    item { Text(stringResource(R.string.explore_empty), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
}
