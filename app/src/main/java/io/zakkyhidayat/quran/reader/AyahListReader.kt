package io.zakkyhidayat.quran.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.zakkyhidayat.quran.AppViewModel
import io.zakkyhidayat.quran.R
import io.zakkyhidayat.quran.data.AyahDetail
import io.zakkyhidayat.quran.data.AyahPos
import io.zakkyhidayat.quran.data.AyahRef
import io.zakkyhidayat.quran.data.Surah
import io.zakkyhidayat.quran.ui.CenteredContent
import io.zakkyhidayat.quran.ui.NumberBadge
import kotlinx.coroutines.flow.distinctUntilChanged

// Satu baris daftar: judul surah (sebelum ayat pertama) atau satu ayat.
private sealed interface ListRow {
    val key: String

    data class Header(val surah: Int) : ListRow {
        override val key get() = "s$surah"
    }

    data class Ayah(val pos: AyahPos) : ListRow {
        override val key get() = "a${pos.surah}:${pos.ayah}"
    }
}

/**
 * Mode baca daftar: seluruh Al-Qur'an sebagai satu daftar ayat bergulir, dengan judul di setiap awal surah.
 * [showArabic] false = terjemahan saja. Posisi disinkronkan lewat halaman: [targetPage] menggulir daftar ke halaman itu,
 * dan halaman ayat teratas yang terlihat dilaporkan lewat [onPageChange].
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun AyahListReader(
    vm: AppViewModel,
    surahs: Map<Int, Surah>,
    translationIds: List<String>,
    showArabic: Boolean,
    targetPage: Int,
    targetAyah: AyahRef?,
    onPageChange: (page: Int, first: AyahRef) -> Unit,
    modifier: Modifier = Modifier,
) {
    val index by produceState<List<AyahPos>?>(null) { value = vm.mushaf.ayahIndex() }
    val rows = remember(index) {
        index?.flatMap { pos -> if (pos.ayah == 1) listOf(ListRow.Header(pos.surah), ListRow.Ayah(pos)) else listOf(ListRow.Ayah(pos)) }
    }
    val state = rememberLazyListState()

    if (rows == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }
        return
    }

    // Gulir ke ayat tujuan (bila ada di halaman tujuan) atau ke ayat pertama halaman itu.
    LaunchedEffect(targetPage, targetAyah, rows) {
        val wanted = rows.indexOfFirst {
            it is ListRow.Ayah && if (targetAyah != null && it.pos.page == targetPage) {
                it.pos.surah == targetAyah.surah && it.pos.ayah == targetAyah.ayah
            } else {
                it.pos.page >= targetPage
            }
        }
        if (wanted < 0) return@LaunchedEffect
        val current = (rows.getOrNull(state.firstVisibleItemIndex) as? ListRow.Ayah)?.pos
        if (current != null && current.page == targetPage && targetAyah == null) return@LaunchedEffect
        // Tampilkan juga judul surah bila ayat tujuan adalah ayat pertama.
        val start = if (wanted > 0 && rows[wanted - 1] is ListRow.Header) wanted - 1 else wanted
        state.scrollToItem(start)
    }
    LaunchedEffect(rows) {
        snapshotFlow { state.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { i ->
                val pos = rows.drop(i).firstNotNullOfOrNull { (it as? ListRow.Ayah)?.pos } ?: return@collect
                onPageChange(pos.page, AyahRef(pos.surah, pos.ayah))
            }
    }

    CenteredContent(modifier.fillMaxSize()) {
        LazyColumn(state = state, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(rows, key = { it.key }, contentType = { it::class }) { row ->
                when (row) {
                    is ListRow.Header -> SurahTitle(surahs[row.surah])
                    is ListRow.Ayah -> AyahRow(vm, row.pos, translationIds, showArabic)
                }
            }
        }
    }
}

@Composable
private fun SurahTitle(surah: Surah?) {
    if (surah == null) return
    val context = LocalContext.current
    val nameFont = remember { surahNameFontFamily(context) }
    val commonFont = remember { commonFontFamily(context) }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            surah.nameGlyph.toString(),
            style = TextStyle(fontFamily = nameFont, fontSize = 40.sp, color = MaterialTheme.colorScheme.primary),
            modifier = Modifier.clearAndSetSemantics { },
        )
        Text(stringResource(R.string.surah_title, surah.nameLatin), style = MaterialTheme.typography.titleMedium)
        // Al-Fatihah: basmalah adalah ayat 1; At-Taubah tanpa basmalah.
        if (surah.id != 1 && surah.id != 9) {
            Spacer(Modifier.height(12.dp))
            Text(
                "﷽",
                style = TextStyle(fontFamily = commonFont, fontSize = 36.sp, color = MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.clearAndSetSemantics { },
            )
        }
    }
    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun AyahRow(vm: AppViewModel, pos: AyahPos, translationIds: List<String>, showArabic: Boolean) {
    val context = LocalContext.current
    val detail by produceState<AyahDetail?>(null, pos, translationIds) {
        value = vm.mushaf.ayahDetail(pos.surah, pos.ayah, translationIds)
    }
    val arabicFont = remember { arabicFontFamily(context) }
    Column(
        Modifier
            .fillMaxWidth()
            .clickable { vm.selectAyah(AyahRef(pos.surah, pos.ayah)) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NumberBadge(pos.ayah, Modifier.size(36.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                "${pos.surah}:${pos.ayah}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val d = detail
        if (d == null) {
            Spacer(Modifier.height(48.dp))
            return@Column
        }
        if (showArabic) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                val style = MaterialTheme.typography.headlineSmall
                Text(
                    d.arabic,
                    style = style.copy(fontFamily = arabicFont, lineHeight = style.fontSize * 1.9f),
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (d.translations.isEmpty() && !showArabic) {
            Text(
                stringResource(R.string.no_translation_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        d.translations.forEach { tr ->
            if (d.translations.size > 1) {
                Text(tr.info.name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            val body = MaterialTheme.typography.bodyLarge
            Text(
                translationText(tr.text, MaterialTheme.colorScheme.primary, MaterialTheme.typography.labelSmall.fontSize),
                style = body.copy(lineHeight = body.fontSize * 1.5f),
            )
        }
    }
    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}
