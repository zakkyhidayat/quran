package io.zakkyhidayat.quran.search

import androidx.compose.ui.res.stringResource
import io.zakkyhidayat.quran.R
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.zakkyhidayat.quran.AppViewModel
import io.zakkyhidayat.quran.data.ArabicText
import io.zakkyhidayat.quran.data.SearchResult
import io.zakkyhidayat.quran.reader.arabicFontFamily
import io.zakkyhidayat.quran.settings.AppSettings
import io.zakkyhidayat.quran.ui.CenteredContent
import kotlinx.coroutines.delay

private sealed interface Hit {
    data class SurahHit(val surah: Int) : Hit
    data class PageHit(val page: Int) : Hit
    data class AyahHit(val surah: Int, val ayah: Int) : Hit
    data class Result(val value: SearchResult) : Hit
}

private val AYAH_REF = Regex("^(\\d{1,3})\\s*[:.\\s]\\s*(\\d{1,3})$")
private val NUMBER = Regex("^\\d{1,3}$")

/** Rujukan ayat seperti "2:255", "2.255", atau "2 255" -> (surah, ayat); null bila bukan rujukan. Batas surah/ayat dicek pemanggil. */
internal fun parseAyahReference(query: String): Pair<Int, Int>? =
    AYAH_REF.find(query.trim())?.let { it.groupValues[1].toInt() to it.groupValues[2].toInt() }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SearchScreen(vm: AppViewModel, settings: AppSettings, onBack: () -> Unit) {
    val textState = rememberTextFieldState()
    val searchBarState = rememberSearchBarState()
    val query = textState.text.toString()
    val surahs by vm.surahs.collectAsStateWithLifecycle()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    val hits by produceState<List<Hit>>(emptyList(), query, surahs, settings.translationIds) {
        val q = query.trim()
        if (q.isEmpty()) { value = emptyList(); return@produceState }
        delay(250)
        val quick = mutableListOf<Hit>()
        parseAyahReference(q)?.let { (s, a) ->
            if (surahs[s]?.let { a in 1..it.ayahCount } == true) quick += Hit.AyahHit(s, a)
        }
        if (NUMBER.matches(q)) {
            val n = q.toInt()
            if (n in 1..114) quick += Hit.SurahHit(n)
            if (n in 1..604) quick += Hit.PageHit(n)
        }
        val key = q.lowercase().filter { it.isLetterOrDigit() }
        if (key.length >= 2 && !ArabicText.containsArabic(q)) {
            surahs.values.filter { it.nameLatin.lowercase().filter { c -> c.isLetterOrDigit() }.contains(key) }
                .forEach { quick += Hit.SurahHit(it.id) }
        }
        value = quick
        val found = if (ArabicText.containsArabic(q)) vm.mushaf.searchArabic(q) else vm.mushaf.searchTranslations(q, settings.translationIds)
        value = quick + found.map { Hit.Result(it) }
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
                    placeholder = { Text(stringResource(R.string.search_placeholder)) },
                    leadingIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) } },
                    trailingIcon = {
                        if (query.isNotEmpty()) IconButton(onClick = { textState.clearText() }) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear)) }
                    },
                    modifier = Modifier.focusRequester(focus),
                )
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        CenteredContent(Modifier.padding(padding)) {
            if (query.isBlank()) {
                Hint(stringResource(R.string.search_hint))
            } else if (hits.isEmpty()) {
                Hint(stringResource(R.string.no_results))
            } else {
                LazyColumn(Modifier.fillMaxSize().imePadding()) {
                    items(hits) { hit -> HitRow(hit, vm, onBack) }
                }
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HitRow(hit: Hit, vm: AppViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val surahs by vm.surahs.collectAsStateWithLifecycle()
    when (hit) {
        is Hit.SurahHit -> {
            val s = surahs[hit.surah] ?: return
            ListItem(
                onClick = { vm.clearSelection(); vm.goToPage(s.firstPage); onBack() },
                supportingContent = { Text(stringResource(R.string.search_surah_sub, s.id, s.ayahCount, s.firstPage)) },
            ) { Text(stringResource(R.string.surah_title, s.nameLatin)) }
        }
        is Hit.PageHit -> ListItem(onClick = { vm.clearSelection(); vm.goToPage(hit.page); onBack() }) { Text(stringResource(R.string.page_n, hit.page)) }
        is Hit.AyahHit -> ListItem(
            onClick = { vm.goToAyah(hit.surah, hit.ayah, openSheet = true); onBack() },
            supportingContent = { Text(stringResource(R.string.open_ayah)) },
        ) { Text("${surahs[hit.surah]?.nameLatin.orEmpty()} ${hit.surah}:${hit.ayah}") }
        is Hit.Result -> {
            val r = hit.value
            ListItem(
                onClick = { vm.goToAyah(r.surah, r.ayah, openSheet = true); onBack() },
                overlineContent = { Text("${surahs[r.surah]?.nameLatin.orEmpty()} ${r.surah}:${r.ayah} • ${r.source}") },
            ) {
                    if (r.highlightStart >= 0) {
                        Text(
                            buildAnnotatedString {
                                append(r.snippet.substring(0, r.highlightStart))
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
                                    append(r.snippet.substring(r.highlightStart, r.highlightEnd))
                                }
                                append(r.snippet.substring(r.highlightEnd))
                            },
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    } else {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            val style = MaterialTheme.typography.titleLarge
                            val hafs = remember { arabicFontFamily(context) }
                            Text(r.snippet, style = style.copy(fontFamily = hafs, lineHeight = style.fontSize * 1.9f), maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
                        }
                    }
            }
        }
    }
}
