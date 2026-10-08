package io.zakkyhidayat.quran.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
private val PagePadding = 8.dp
private val HeaderHeight = 28.dp
private val FooterHeight = 24.dp
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
) {
    val surahs by vm.surahs.collectAsStateWithLifecycle()
    val pageMeta by vm.pageMeta.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()
    val sheetVisible by vm.sheetVisible.collectAsStateWithLifecycle()
    val detail by vm.detail.collectAsStateWithLifecycle()
    val bookmarks by vm.bookmarkStore.bookmarks.collectAsStateWithLifecycle()

    val startPage = remember { (vm.pendingPage.value ?: settings.lastPage).coerceIn(1, PAGE_COUNT) }
    val pagerState = rememberPagerState(initialPage = startPage - 1) { PAGE_COUNT }
    var barsVisible by rememberSaveable { mutableStateOf(false) }
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
            vm.settingsRepository.setLastPage(index + 1)
        }
    }

    val meta = pageMeta.getOrNull(currentPage - 1)
    val currentSurah = meta?.let { surahs[it.surah] }
    val pageBookmarked = bookmarks.any { it.kind == BookmarkKind.Page && it.page == currentPage }

    Scaffold(containerColor = MaterialTheme.colorScheme.background, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { _ ->
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
            // Bilah atas mendorong halaman ke bawah, tidak menimpanya.
            AnimatedVisibility(
                visible = barsVisible,
                enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) + expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
                exit = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()) + shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
            ) {
                TopAppBar(
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    title = {
                        Column {
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
            }
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
                        detectTapGestures(onTap = { if (selected != null) vm.clearSelection() else barsVisible = !barsVisible })
                    },
                ) {
                    // Blok halaman berproporsi kertas B5 (176 x 250 mm), seperti mushaf cetak; header dan nomor menempel di sekelilingnya.
                    val available = minOf(maxWidth - PagePadding * 2, PageMaxWidth)
                    val blockWidth = minOf(available, (maxHeight - PageChromeHeight) / PageHeightOverWidth)
                    val blockHeight = blockWidth * PageHeightOverWidth
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(Modifier.width(blockWidth).height(HeaderHeight).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(pageInfo?.let { surahs[it.surah]?.nameLatin }.orEmpty(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Box(Modifier.weight(1f))
                            Text("Juz ${pageInfo?.juz ?: ""}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                )
                            }
                        }
                        Box(Modifier.width(blockWidth).height(FooterHeight), contentAlignment = Alignment.Center) {
                            Text("$page", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
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
            )
        }
    }
}
