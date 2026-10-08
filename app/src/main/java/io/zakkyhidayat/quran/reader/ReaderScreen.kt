package io.zakkyhidayat.quran.reader

import kotlinx.coroutines.launch
import io.zakkyhidayat.quran.settings.ReadingMode
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.res.stringResource
import io.zakkyhidayat.quran.R
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
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
private val PageEndPadding = 8.dp

// Blok halaman digeser ke luar tepi kiri layar sebesar ini (dan dilebarkan sama besar) supaya margin kiri lebih sempit.
private val PageStartBleed = (-2).dp
private val HeaderHeight = 32.dp
private val FooterHeight = 32.dp
private val HeaderGap = 10.dp
private val PageChromeHeight = HeaderHeight + HeaderGap + FooterHeight
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
    val mode = settings.readingMode
    val listMode = mode != ReadingMode.Mushaf
    // Mode daftar: halaman ayat teratas yang terlihat, dan tujuan gulir (halaman + ayat) saat melompat.
    var listPage by remember { mutableIntStateOf(startPage) }
    var listTarget by remember { mutableStateOf(startPage to vm.selected.value) }
    val currentPage by remember(listMode) { derivedStateOf { if (listMode) listPage else pagerState.currentPage + 1 } }

    LaunchedEffect(listMode) {
        vm.pendingPage.collect { page ->
            if (page != null) {
                if (listMode) {
                    listTarget = page to vm.selected.value
                } else {
                    pagerState.scrollToPage(page - 1)
                }
                vm.pendingPage.value = null
            }
        }
    }
    // Berpindah mode tetap di posisi yang sama.
    LaunchedEffect(listMode) {
        if (listMode) {
            listPage = pagerState.currentPage + 1
            listTarget = listPage to vm.selected.value
        } else {
            pagerState.scrollToPage(listPage - 1)
        }
    }
    LaunchedEffect(listMode) {
        if (listMode) return@LaunchedEffect
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
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val commonFont = remember { commonFontFamily(context) }
    val nameFont = remember { surahNameFontFamily(context) }
    val meta = pageMeta.getOrNull(currentPage - 1)
    val currentSurah = meta?.let { surahs[it.surah] }
    val pageBookmarked = bookmarks.any { it.kind == BookmarkKind.Page && it.page == currentPage }

    Scaffold(containerColor = MaterialTheme.colorScheme.background, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { _ ->
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
            // Bilah atas permanen; halaman berada di ruang di bawahnya.
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Column(Modifier.clickable(onClickLabel = stringResource(R.string.jump_to_ayah)) { showJump = true }) {
                        Text(currentSurah?.nameLatin.orEmpty(), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.juz_page, meta?.juz ?: "", currentPage), style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    if (onOpenIndex != null) {
                        IconButton(onClick = onOpenIndex) { Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.index_cd)) }
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSearch) { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search)) }
                    IconButton(onClick = { vm.togglePageBookmark(currentPage) }) {
                        Icon(
                            if (pageBookmarked) AppIcons.Bookmark else AppIcons.BookmarkBorder,
                            contentDescription = if (pageBookmarked) stringResource(R.string.remove_page_bookmark) else stringResource(R.string.bookmark_page),
                        )
                    }
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings)) }
                },
            )
            if (listMode) {
                AyahListReader(
                    vm = vm,
                    surahs = surahs,
                    translationIds = settings.translationIds,
                    showArabic = mode == ReadingMode.AyahTranslation,
                    targetPage = listTarget.first,
                    targetAyah = listTarget.second,
                    onPageChange = { page, first ->
                        listPage = page
                        vm.currentPage.value = page
                        scope.launch {
                            vm.settingsRepository.setLastPage(page)
                            vm.settingsRepository.setLastAyah(first.surah, first.ayah)
                        }
                    },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            } else {
                // Halaman mushaf selalu kiri-ke-kanan secara tata letak (halaman berikutnya di kiri, nama surah di kiri atas),
                // juga saat antarmuka berbahasa Arab/Urdu/Persia yang RTL.
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
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
                                    val headSurah = pageInfo?.let { surahs[it.surah] }
                                    // Nama surah (font nama surah) disamakan dengan judul juz kaligrafi: warna sama, tinggi tinta sama (~21 sp).
                                    val stripColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    if (headSurah != null) {
                                        Text(
                                            text = headSurah.nameGlyph.toString(),
                                            style = TextStyle(
                                                fontFamily = nameFont,
                                                fontSize = 18.sp,
                                                // Metrik vertikal font ini ~2,6 em; dibatasi supaya tidak mendorong tata letak.
                                                lineHeight = 21.sp,
                                                lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
                                                color = stripColor,
                                            ),
                                            modifier = Modifier
                                                .height(24.dp)
                                                .wrapContentHeight(Alignment.CenterVertically, unbounded = true)
                                                .clickable(onClickLabel = stringResource(R.string.open_surah_info)) { onOpenSurahInfo(headSurah.id) }
                                                .semantics { contentDescription = context.getString(R.string.surah_info_cd, headSurah.nameLatin) },
                                        )
                                    }
                                    Box(Modifier.weight(1f))
                                    pageInfo?.let { info ->
                                        Text(
                                            text = juzTitleGlyph(info.juz),
                                            style = TextStyle(fontFamily = commonFont, fontSize = 18.sp, color = stripColor),
                                            modifier = Modifier.semantics { contentDescription = context.getString(R.string.juz_n, info.juz) },
                                        )
                                    }
                                }
                                Spacer(Modifier.height(HeaderGap))
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
                                    ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.next_surah_cd, next?.nameLatin.orEmpty()), modifier = Modifier.size(20.dp)) }
                                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                        Text("$page", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(
                                        onClick = { previous?.let { vm.clearSelection(); vm.goToPage(it.firstPage) } },
                                        enabled = previous != null,
                                        modifier = Modifier.size(32.dp),
                                    ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.previous_surah_cd, previous?.nameLatin.orEmpty()), modifier = Modifier.size(20.dp)) }
                                }
                            }
                        }
                    }
                }
            }
            if (!listMode && !settings.gestureHintDone) {
                GestureHint { scope.launch { vm.settingsRepository.setGestureHintDone() } }
            }
            // Pill cara baca di barisnya sendiri, jadi tidak menutupi halaman; tinggi halaman menyesuaikan.
            ReadingModeBar(mode) { scope.launch { vm.settingsRepository.setReadingMode(it) } }
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


@Composable
private fun ReadingModeBar(mode: ReadingMode, onSelect: (ReadingMode) -> Unit) {
    val options = listOf(
        Triple(ReadingMode.Mushaf, R.string.mode_mushaf, AppIcons.MenuBook),
        Triple(ReadingMode.AyahTranslation, R.string.mode_ayah_translation, AppIcons.Translate),
        Triple(ReadingMode.Translation, R.string.mode_translation, AppIcons.Notes),
    )
    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Row(Modifier.padding(4.dp).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEach { (value, label, icon) ->
                    val chosen = mode == value
                    val name = stringResource(label)
                    Surface(
                        selected = chosen,
                        onClick = { onSelect(value) },
                        shape = CircleShape,
                        color = if (chosen) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                        contentColor = if (chosen) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.semantics {
                            role = Role.Tab
                            if (!chosen) contentDescription = name
                        },
                    ) {
                        Row(
                            Modifier.heightIn(min = 40.dp).padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                            // Label hanya pada mode aktif, agar pill tetap ringkas di layar sempit.
                            if (chosen) {
                                Spacer(Modifier.width(8.dp))
                                Text(name, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Petunjuk sekali untuk pengguna baru, di baris sendiri di atas pill (tidak menutupi halaman).
@Composable
private fun GestureHint(onDismiss: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.hint_title), style = MaterialTheme.typography.titleSmall)
                listOf(R.string.hint_swipe, R.string.hint_tap_ayah, R.string.hint_tap_surah).forEach {
                    Text("• " + stringResource(it), style = MaterialTheme.typography.bodyMedium)
                }
            }
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.hint_ok)) }
        }
    }
}
