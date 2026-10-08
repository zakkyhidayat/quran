package io.zakkyhidayat.quran.reader

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import io.zakkyhidayat.quran.ui.JumpToAyahDialog
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.zakkyhidayat.quran.AppViewModel
import io.zakkyhidayat.quran.data.AyahRef
import io.zakkyhidayat.quran.data.AyahText
import io.zakkyhidayat.quran.data.BookmarkKind
import io.zakkyhidayat.quran.data.PageLine
import io.zakkyhidayat.quran.settings.AppSettings
import io.zakkyhidayat.quran.ui.AppIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

private const val PAGE_COUNT = 604
private val PageMaxWidth = 640.dp
// Margin tidak simetris: kiri rapat ke tepi layar, kanan memberi sedikit ruang.
private val PageEndPadding = 4.dp

// Blok halaman digeser ke luar tepi kiri layar sebesar ini (dan dilebarkan sama besar) supaya margin kiri lebih sempit.
private val PageStartBleed = 3.dp
private val HeaderHeight = 28.dp
private val FooterHeight = 32.dp
private val PageChromeHeight = HeaderHeight + FooterHeight
// Kertas B5: 176 x 250 mm.
private const val PageHeightOverWidth = 250f / 176f

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ReaderScreen(
    vm: AppViewModel,
    settings: AppSettings,
    onOpenIndex: (() -> Unit)?,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSurahInfo: (Int) -> Unit,
) {
    val surahs by vm.surahs.collectAsStateWithLifecycle()
    val pageMeta by vm.pageMeta.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()
    val sheetVisible by vm.sheetVisible.collectAsStateWithLifecycle()
    val detail by vm.detail.collectAsStateWithLifecycle()
    val bookmarks by vm.bookmarkStore.bookmarks.collectAsStateWithLifecycle()

    val startPage = remember { (vm.pendingPage.value ?: settings.lastPage).coerceIn(1, PAGE_COUNT) }
    val pagerState = rememberPagerState(initialPage = startPage - 1) { PAGE_COUNT }
    val currentPage by remember { derivedStateOf { pagerState.currentPage + 1 } }

    LaunchedEffect(Unit) {
        vm.pendingPage.collect { page ->
            if (page != null) {
                pagerState.scrollToPage(page - 1)
                vm.pendingPage.value = null
            }
        }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { pagerState.currentPage }.collectLatest { index ->
            vm.currentPage.value = index + 1
            delay(600)
            val page = index + 1
            // Baca terakhir per ayat: ayat yang sedang dipilih bila ada di halaman ini, kalau tidak ayat pertama halaman.
            val chosen = vm.selected.value
            val ayah = if (chosen != null && vm.mushaf.ayahPage(chosen.surah, chosen.ayah) == page) chosen else vm.mushaf.firstAyahOnPage(page)
            vm.settingsRepository.setLastPage(page)
            vm.settingsRepository.setLastAyah(ayah.surah, ayah.ayah)
        }
    }

    var showJump by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val commonFont = remember { commonFontFamily(context) }
    val meta = pageMeta.getOrNull(currentPage - 1)
    val currentSurah = meta?.let { surahs[it.surah] }
    val pageBookmarked = bookmarks.any { it.kind == BookmarkKind.Page && it.page == currentPage }

    Scaffold(containerColor = MaterialTheme.colorScheme.background, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { _ ->
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
            // Bilah atas permanen; halaman berada di ruang di bawahnya.
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Column(Modifier.clickable(onClickLabel = "Lompat ke ayat") { showJump = true }) {
                        Text(currentSurah?.nameLatin.orEmpty(), style = MaterialTheme.typography.titleMedium)
                        Text("Juz ${meta?.juz ?: ""} • Hal. $currentPage", style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    if (onOpenIndex != null) {
                        IconButton(onClick = onOpenIndex) { Icon(Icons.Default.Menu, contentDescription = "Daftar surah dan juz") }
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSearch) { Icon(Icons.Default.Search, contentDescription = "Cari") }
                    IconButton(onClick = { vm.togglePageBookmark(currentPage) }) {
                        Icon(
                            if (pageBookmarked) AppIcons.Bookmark else AppIcons.BookmarkBorder,
                            contentDescription = if (pageBookmarked) "Hapus bookmark halaman" else "Bookmark halaman",
                        )
                    }
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, contentDescription = "Pengaturan") }
                },
            )
            HorizontalPager(
                state = pagerState,
                reverseLayout = true,
                beyondViewportPageCount = 1,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) { index ->
                val page = index + 1
                val lines by produceState<List<PageLine>?>(null, page) { value = vm.mushaf.page(page) }
                val ayahTexts by produceState<List<AyahText>>(emptyList(), page) { value = vm.mushaf.pageAyahs(page) }
                val pageInfo = pageMeta.getOrNull(index)
                BoxWithConstraints(
                    Modifier.fillMaxSize().pointerInput(selected) {
                        detectTapGestures(onTap = { if (selected != null) vm.clearSelection() })
                    },
                ) {
                    // Blok halaman berproporsi kertas B5 (176 x 250 mm), seperti mushaf cetak; header dan nomor menempel di sekelilingnya.
                    val bleed = if (maxWidth - PageEndPadding < PageMaxWidth) PageStartBleed else 0.dp
                    val available = minOf(maxWidth - PageEndPadding + bleed, PageMaxWidth)
                    val blockWidth = minOf(available, (maxHeight - PageChromeHeight) / PageHeightOverWidth)
                    val blockHeight = blockWidth * PageHeightOverWidth
                    // Di layar sempit blok menempel ke kiri; di layar lebar sisa ruang dibagi dua agar halaman tetap di tengah.
                    val startOffset = ((maxWidth - blockWidth - PageEndPadding) / 2).coerceAtLeast(0.dp) - bleed
                    Column(Modifier.align(Alignment.CenterStart).offset(x = startOffset), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(Modifier.width(blockWidth).height(HeaderHeight).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(pageInfo?.let { surahs[it.surah]?.nameLatin }.orEmpty(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Box(Modifier.weight(1f))
                            pageInfo?.let { info ->
                                Text(
                                    text = juzTitleGlyph(info.juz),
                                    style = TextStyle(fontFamily = commonFont, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
                                    modifier = Modifier.semantics { contentDescription = "Juz ${info.juz}" },
                                )
                            }
                        }
                        Box(Modifier.size(blockWidth, blockHeight), contentAlignment = Alignment.Center) {
                            val loaded = lines
                            if (loaded == null) {
                                LoadingIndicator()
                            } else {
                                MushafPage(
                                    page = page,
                                    lines = loaded,
                                    ayahTexts = ayahTexts,
                                    surahs = surahs,
                                    selected = selected,
                                    onAyahClick = { vm.selectAyah(it) },
                                    tajweed = settings.tajweed,
                                    onSurahClick = onOpenSurahInfo,
                                )
                            }
                        }
                        val pageSurah = pageInfo?.surah
                        val previous = pageSurah?.let { surahs[it - 1] }
                        val next = pageSurah?.let { surahs[it + 1] }
                        // Mushaf dibaca kanan ke kiri: surah berikutnya di kiri, sebelumnya di kanan.
                        Row(Modifier.width(blockWidth).height(FooterHeight), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { next?.let { vm.clearSelection(); vm.goToPage(it.firstPage) } },
                                enabled = next != null,
                                modifier = Modifier.size(32.dp),
                            ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Surah berikutnya: ${next?.nameLatin.orEmpty()}", modifier = Modifier.size(20.dp)) }
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                Text("$page", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(
                                onClick = { previous?.let { vm.clearSelection(); vm.goToPage(it.firstPage) } },
                                enabled = previous != null,
                                modifier = Modifier.size(32.dp),
                            ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Surah sebelumnya: ${previous?.nameLatin.orEmpty()}", modifier = Modifier.size(20.dp)) }
                        }
                    }
                }
            }
        }
    }

    if (showJump) {
        JumpToAyahDialog(
            surahs = surahs,
            initial = selected ?: meta?.let { AyahRef(it.surah, 1) },
            onDismiss = { showJump = false },
            onJump = { surah, ayah -> showJump = false; vm.goToAyah(surah, ayah) },
        )
    }

    val shown = detail
    if (sheetVisible && shown != null) {
        val ref = AyahRef(shown.surah, shown.ayah)
        ModalBottomSheet(onDismissRequest = vm::dismissSheet) {
            AyahSheetContent(
                detail = shown,
                surah = surahs[shown.surah],
                bookmarked = bookmarks.any { it.kind == BookmarkKind.Ayah && it.surah == shown.surah && it.ayah == shown.ayah },
                onToggleBookmark = { vm.toggleAyahBookmark(ref, shown.page) },
                onPrevious = { vm.moveSelection(-1) },
                onNext = { vm.moveSelection(1) },
                showTransliteration = settings.showTransliteration,
            )
        }
    }
}
