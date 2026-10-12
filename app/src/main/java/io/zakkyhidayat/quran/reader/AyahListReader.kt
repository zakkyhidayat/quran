package io.zakkyhidayat.quran.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalClipboard
import io.zakkyhidayat.quran.data.BookmarkKind
import io.zakkyhidayat.quran.ui.AppIcons
import kotlinx.coroutines.launch
import io.zakkyhidayat.quran.ui.scaled
import io.zakkyhidayat.quran.ui.LocalReadingTextScale
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
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

/**
 * Mode baca daftar: satu layar = ayat-ayat dari satu halaman mushaf, berpindah halaman dengan geser seperti mode mushaf
 * (kanan ke kiri). Isi halaman yang panjang digulir ke bawah. [showArabic] false = terjemahan saja.
 * Mengetuk ayat membuka baris aksi (markah, salin, bagikan) di bawahnya; hanya satu ayat terbuka sekaligus.
 * [onFirstVisible] melaporkan ayat pertama yang terlihat di halaman aktif (untuk penghitung di bilah atas).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun AyahListReader(
    vm: AppViewModel,
    surahs: Map<Int, Surah>,
    translationIds: List<String>,
    showArabic: Boolean,
    animateArabic: Boolean = true,
    state: PagerState,
    targetAyah: AyahRef?,
    modifier: Modifier = Modifier,
    onFirstVisible: (page: Int, ayah: AyahRef) -> Unit = { _, _ -> },
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
    var openAyah by remember { mutableStateOf<AyahRef?>(null) }
    val bookmarks by vm.bookmarkStore.bookmarks.collectAsStateWithLifecycle()
    val bookmarkedAyahs = remember(bookmarks) {
        bookmarks.filter { it.kind == BookmarkKind.Ayah }.map { AyahRef(it.surah, it.ayah) }.toSet()
    }
    // Urutan halaman selalu seperti mushaf (halaman berikutnya di kiri), juga saat antarmuka RTL; isi halaman memakai
    // arah antarmuka.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        HorizontalPager(state = state, reverseLayout = true, beyondViewportPageCount = 1, modifier = modifier.fillMaxSize()) { index ->
            CompositionLocalProvider(LocalLayoutDirection provides contentDirection) {
                PageAyahs(
                    vm, index + 1, pages[index + 1].orEmpty(), surahs, translationIds, showArabic, animateArabic, targetAyah,
                    isCurrent = state.currentPage == index,
                    openAyah = openAyah,
                    onToggleAyah = { ref -> openAyah = if (openAyah == ref) null else ref },
                    bookmarkedAyahs = bookmarkedAyahs,
                    onFirstVisible = onFirstVisible,
                )
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
    animateArabic: Boolean,
    targetAyah: AyahRef?,
    isCurrent: Boolean,
    openAyah: AyahRef?,
    onToggleAyah: (AyahRef) -> Unit,
    bookmarkedAyahs: Set<AyahRef>,
    onFirstVisible: (page: Int, ayah: AyahRef) -> Unit,
) {
    val listState = rememberLazyListState()
    // Ayat pertama yang terlihat, hanya dari halaman yang sedang aktif.
    LaunchedEffect(isCurrent, ayahs) {
        if (!isCurrent) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex }.collect { i ->
            ayahs.getOrNull(i)?.let { onFirstVisible(page, AyahRef(it.surah, it.ayah)) }
        }
    }
    // Lompat ke ayat tertentu (dari daftar, pencarian, atau baca terakhir) bila ayat itu ada di halaman ini.
    LaunchedEffect(targetAyah, ayahs) {
        val i = ayahs.indexOfFirst { targetAyah != null && it.surah == targetAyah.surah && it.ayah == targetAyah.ayah }
        if (i > 0) listState.scrollToItem(i)
    }
    CenteredContent(Modifier.fillMaxSize()) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(ayahs, key = { "${it.surah}:${it.ayah}" }) { pos ->
                if (pos.ayah == 1) SurahTitle(surahs[pos.surah])
                val ref = AyahRef(pos.surah, pos.ayah)
                AyahRow(
                    vm, pos, surahs[pos.surah], translationIds, showArabic, animateArabic,
                    actionsOpen = openAyah == ref,
                    bookmarked = ref in bookmarkedAyahs,
                    onToggle = { onToggleAyah(ref) },
                )
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
private fun AyahRow(
    vm: AppViewModel,
    pos: AyahPos,
    surah: Surah?,
    translationIds: List<String>,
    showArabic: Boolean,
    animateArabic: Boolean,
    actionsOpen: Boolean,
    bookmarked: Boolean,
    onToggle: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val detail by produceState<AyahDetail?>(null, pos, translationIds) {
        value = vm.mushaf.ayahDetail(pos.surah, pos.ayah, translationIds)
    }
    val arabicFont = remember { arabicFontFamily(context) }
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = stringResource(if (actionsOpen) R.string.ayah_actions_hide else R.string.ayah_actions_show), onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        val motion = MaterialTheme.motionScheme
        val d = detail
        if (d == null) {
            Spacer(Modifier.height(48.dp))
            return@Column
        }
        // Teks Arab muncul/hilang dengan animasi saat berganti antara "ayat + terjemahan" dan "terjemahan saja".
        AnimatedVisibility(
            visible = showArabic,
            // Tanpa animasi (snap) bila daftar belum terlihat, agar tidak ada lompatan tata letak saat memudar masuk.
            enter = if (animateArabic) expandVertically(motion.defaultSpatialSpec()) + fadeIn(motion.defaultEffectsSpec()) else EnterTransition.None,
            exit = if (animateArabic) shrinkVertically(motion.defaultSpatialSpec()) + fadeOut(motion.defaultEffectsSpec()) else ExitTransition.None,
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                val style = MaterialTheme.typography.headlineSmall.scaled(LocalReadingTextScale.current.arabic, 1.9f)
                Text(
                    d.arabic,
                    style = style.copy(fontFamily = arabicFont),
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            d.translations.forEach { tr ->
                if (d.translations.size > 1) {
                    Text(tr.info.name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                val body = MaterialTheme.typography.bodyLarge.scaled(LocalReadingTextScale.current.translation, 1.5f)
                // Nomor ayat di depan terjemahan ("84. ..."), tanpa lencana terpisah.
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) { append("${pos.ayah}. ") }
                        append(translationTextOrPlaceholder(tr.text, MaterialTheme.colorScheme.primary, MaterialTheme.typography.labelSmall.fontSize))
                    },
                    style = body,
                )
            }
        }
        AnimatedVisibility(
            visible = actionsOpen,
            enter = expandVertically(motion.defaultSpatialSpec()) + fadeIn(motion.defaultEffectsSpec()),
            exit = shrinkVertically(motion.defaultSpatialSpec()) + fadeOut(motion.defaultEffectsSpec()),
        ) {
            val plainText = remember(d) { ayahShareText(d, "${surah?.nameLatin ?: ""} ${pos.surah}:${pos.ayah}") }
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalIconToggleButton(checked = bookmarked, onCheckedChange = { vm.toggleAyahBookmark(AyahRef(pos.surah, pos.ayah), pos.page) }) {
                    Icon(
                        if (bookmarked) AppIcons.Bookmark else AppIcons.BookmarkBorder,
                        contentDescription = if (bookmarked) stringResource(R.string.remove_bookmark) else stringResource(R.string.bookmark_ayah),
                    )
                }
                FilledTonalIconButton(onClick = { scope.launch { copyAyahText(clipboard, plainText) } }) {
                    Icon(AppIcons.ContentCopy, contentDescription = stringResource(R.string.copy))
                }
                FilledTonalIconButton(onClick = { shareAyahText(context, plainText) }) {
                    Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share))
                }
            }
        }
    }
    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}
