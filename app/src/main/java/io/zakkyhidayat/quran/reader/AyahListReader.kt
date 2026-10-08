package io.zakkyhidayat.quran.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.zakkyhidayat.quran.AppViewModel
import io.zakkyhidayat.quran.R
import io.zakkyhidayat.quran.data.AyahDetail
import io.zakkyhidayat.quran.data.AyahPos
import io.zakkyhidayat.quran.data.AyahRef
import io.zakkyhidayat.quran.data.Surah
import io.zakkyhidayat.quran.ui.CenteredContent
import io.zakkyhidayat.quran.ui.NumberBadge

/**
 * Mode baca daftar: satu layar = ayat-ayat dari satu halaman mushaf, berpindah halaman dengan geser seperti mode mushaf
 * (kanan ke kiri). Isi halaman yang panjang digulir ke bawah. [showArabic] false = terjemahan saja.
 * Ayat tidak bisa diketuk: terjemahannya sudah tampil di layar.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun AyahListReader(
    vm: AppViewModel,
    surahs: Map<Int, Surah>,
    translationIds: List<String>,
    showArabic: Boolean,
    state: PagerState,
    targetAyah: AyahRef?,
    modifier: Modifier = Modifier,
) {
    // Terjemahan saja tanpa satu pun terjemahan terpasang: satu pesan, bukan petunjuk berulang di setiap ayat.
    val installed by vm.translations.collectAsStateWithLifecycle()
    if (!showArabic && installed.none { it.id in translationIds }) {
        Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.no_translation_hint),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    val byPage by produceState<Map<Int, List<AyahPos>>?>(null) { value = vm.mushaf.ayahIndex().groupBy { it.page } }
    val pages = byPage
    if (pages == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }
        return
    }

    val contentDirection = LocalLayoutDirection.current
    // Urutan halaman selalu seperti mushaf (halaman berikutnya di kiri), juga saat antarmuka RTL; isi halaman memakai
    // arah antarmuka.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        HorizontalPager(state = state, reverseLayout = true, beyondViewportPageCount = 1, modifier = modifier.fillMaxSize()) { index ->
            CompositionLocalProvider(LocalLayoutDirection provides contentDirection) {
                PageAyahs(vm, index + 1, pages[index + 1].orEmpty(), surahs, translationIds, showArabic, targetAyah)
            }
        }
    }
}

@Composable
private fun PageAyahs(
    vm: AppViewModel,
    page: Int,
    ayahs: List<AyahPos>,
    surahs: Map<Int, Surah>,
    translationIds: List<String>,
    showArabic: Boolean,
    targetAyah: AyahRef?,
) {
    val listState = rememberLazyListState()
    // Lompat ke ayat tertentu (dari daftar, pencarian, atau baca terakhir) bila ayat itu ada di halaman ini.
    LaunchedEffect(targetAyah, ayahs) {
        val i = ayahs.indexOfFirst { targetAyah != null && it.surah == targetAyah.surah && it.ayah == targetAyah.ayah }
        if (i > 0) listState.scrollToItem(i)
    }
    CenteredContent(Modifier.fillMaxSize()) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(ayahs, key = { "${it.surah}:${it.ayah}" }) { pos ->
                if (pos.ayah == 1) SurahTitle(surahs[pos.surah])
                AyahRow(vm, pos, translationIds, showArabic)
            }
            item(key = "page") {
                Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ) {
                        Text("$page", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp))
                    }
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
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Satu penanda nomor ayat saja (teks Arab sudah membawa tanda akhir ayat).
        NumberBadge(pos.ayah, Modifier.size(36.dp))
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
