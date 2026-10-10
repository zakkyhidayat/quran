package io.zakkyhidayat.quran.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.zakkyhidayat.quran.AppViewModel
import io.zakkyhidayat.quran.R
import io.zakkyhidayat.quran.data.AyahWords
import io.zakkyhidayat.quran.reader.arabicFontFamily
import io.zakkyhidayat.quran.ui.segmentedItemColors

internal val ExploreListPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)
internal val ExploreListSpacing = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)

/** Bilah atas berisi kolom pencarian dengan tombol kembali, seperti di layar Jelajahi topik. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExploreSearchTopBar(textState: TextFieldState, placeholder: String, onBack: () -> Unit) {
    val searchBarState = androidx.compose.material3.rememberSearchBarState()
    val query = textState.text.toString()
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
            placeholder = { Text(placeholder) },
            leadingIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) } },
            trailingIcon = {
                if (query.isNotEmpty()) IconButton(onClick = { textState.clearText() }) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear)) }
            },
        )
    }
}

/** Pesan di tengah layar untuk daftar kosong, atau indikator muat bila [message] null. */
@Composable
internal fun CenterState(message: String?) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        if (message == null) CircularProgressIndicator()
        else Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
internal fun SectionHeading(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp).semantics { heading() },
    )
}

/** Teks Arab bertata letak kanan-ke-kiri dengan font Hafs; [highlights] = rentang karakter yang disorot. */
@Composable
internal fun ArabicSnippet(
    text: String,
    highlights: List<IntRange> = emptyList(),
    style: TextStyle = MaterialTheme.typography.titleLarge,
    maxLines: Int = Int.MAX_VALUE,
) {
    val context = LocalContext.current
    val font = remember { arabicFontFamily(context) }
    val mark = SpanStyle(background = MaterialTheme.colorScheme.primaryContainer, color = MaterialTheme.colorScheme.onPrimaryContainer)
    val annotated = remember(text, highlights, mark) {
        buildAnnotatedString {
            append(text)
            highlights.forEach { r -> if (r.first >= 0 && r.last < text.length) addStyle(mark, r.first, r.last + 1) }
        }
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Text(
            annotated,
            style = style.copy(fontFamily = font, lineHeight = 36.sp),
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Baris ayat bersama untuk semua layar Jelajahi: "Nama surah s:a", teks Arab (kata di [wordRanges] disorot, indeks 1-based),
 * label opsional di kanan. Ketuk -> [onClick] (biasanya membuka pembaca di ayat itu).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun AyahRow(
    vm: AppViewModel,
    surah: Int,
    ayah: Int,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    wordRanges: List<IntRange> = emptyList(),
    trailing: String? = null,
    maxLines: Int = 3,
) {
    val surahs by vm.surahs.collectAsStateWithLifecycle()
    val loaded by produceState<AyahWords?>(null, surah, ayah) { value = vm.mushaf.ayahWords(surah, ayah) }
    val title = "${surahs[surah]?.nameLatin.orEmpty()} $surah:$ayah"
    val openLabel = stringResource(R.string.open_in_reader)
    SegmentedListItem(
        onClick = onClick,
        modifier = Modifier.clickLabel(openLabel),
        shapes = ListItemDefaults.segmentedShapes(index, count),
        colors = segmentedItemColors(),
        supportingContent = loaded?.let { l ->
            {
                if (l.words.isEmpty()) ArabicSnippet(l.text, maxLines = maxLines)
                else ArabicSnippet(l.words.joinToString(" "), highlightCharRanges(l.words, wordRanges), maxLines = maxLines)
            }
        },
        trailingContent = trailing?.let { { Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) } },
    ) { Text(title) }
}

/** Label aksi ketuk untuk pembaca layar ("ketuk dua kali untuk <label>") tanpa mengganti aksi klik item itu sendiri. */
internal fun Modifier.clickLabel(label: String): Modifier = semantics { onClick(label = label, action = null) }
