package io.zakkyhidayat.quran.reader

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.graphics.luminance
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.togetherWith
import androidx.compose.animation.scaleIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import kotlinx.coroutines.launch
import io.zakkyhidayat.quran.settings.ReadingMode
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.draw.drawWithContent
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
import io.zakkyhidayat.quran.settings.CounterMode
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
private val HeaderGap = 10.dp
private val PageChromeHeight = HeaderHeight + HeaderGap
// Kertas B5: 176 x 250 mm.
private const val PageHeightOverWidth = 250f / 176f

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ReaderScreen(
    vm: AppViewModel,
    settings: AppSettings,
    /** Kembali ke daftar (ponsel); null di layar lebar, karena daftar tampil di samping. */
    onBack: (() -> Unit)?,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSurahInfo: (Int) -> Unit,
) {
    val surahs by vm.surahs.collectAsStateWithLifecycle()
    val pageMeta by vm.pageMeta.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()
    val sheetVisible by vm.sheetVisible.collectAsStateWithLifecycle()
    val detail by vm.detail.collectAsStateWithLifecycle()
    val extras by vm.ayahExtras.collectAsStateWithLifecycle()
    val bookmarks by vm.bookmarkStore.bookmarks.collectAsStateWithLifecycle()

    val startPage = remember { (vm.pendingPage.value ?: settings.lastPage).coerceIn(1, PAGE_COUNT) }
    val pagerState = rememberPagerState(initialPage = startPage - 1) { PAGE_COUNT }
    val mode = settings.readingMode
    val listMode = mode != ReadingMode.Mushaf
    // Mode daftar terakhir: saat kembali ke mushaf, lapisan daftar masih memudar dan harus tetap tampil seperti sebelumnya
    // (dulu sempat berubah jadi "terjemahan saja" sepersekian detik).
    var lastListMode by remember { mutableStateOf(if (listMode) mode else ReadingMode.AyahTranslation) }
    if (listMode) lastListMode = mode
    // Mode daftar punya pager sendiri (satu layar = satu halaman mushaf); posisinya disamakan saat berganti mode.
    val listPagerState = rememberPagerState(initialPage = startPage - 1) { PAGE_COUNT }
    val activePager = if (listMode) listPagerState else pagerState
    val currentPage by remember(listMode) { derivedStateOf { activePager.currentPage + 1 } }
    // Ayat tujuan di mode daftar (lompat ke ayat, baca terakhir): halaman itu digulir sampai ayatnya terlihat.
    var listTargetAyah by remember { mutableStateOf(vm.selected.value) }

    LaunchedEffect(listMode) {
        vm.pendingPage.collect { page ->
            if (page != null) {
                if (listMode) listTargetAyah = vm.selected.value
                activePager.scrollToPage(page - 1)
                vm.pendingPage.value = null
            }
        }
    }
    // Berpindah mode tetap di halaman yang sama.
    LaunchedEffect(listMode) {
        if (listMode) listPagerState.scrollToPage(pagerState.currentPage) else pagerState.scrollToPage(listPagerState.currentPage)
    }
    val prefetchContext = LocalContext.current
    val prefetchPalette = GlyphPalette.of(settings.tajweed, MaterialTheme.colorScheme.background.luminance() < 0.5f)
    LaunchedEffect(prefetchPalette) {
        snapshotFlow { pagerState.currentPage + 1 }.collectLatest { page ->
            withContext(Dispatchers.IO) { prefetchPageFonts(prefetchContext, page, prefetchPalette) }
        }
    }
    // Baca terakhir mengikuti pager yang sedang aktif (mushaf atau daftar).
    LaunchedEffect(listMode) {
        snapshotFlow { activePager.currentPage }.collectLatest { index ->
            vm.currentPage.value = index + 1
            delay(600)
            val page = index + 1
            // Baca terakhir per ayat: ayat yang sedang dipilih bila ada di halaman ini, kalau tidak ayat pertama halaman.
            val chosen = vm.selected.value
            val ayah = if (chosen != null && vm.mushaf.ayahPage(chosen.surah, chosen.ayah) == page) chosen else vm.mushaf.firstAyahOnPage(page)
            vm.settingsRepository.setPosition(page, ayah.surah, ayah.ayah)
        }
    }

    var showJump by remember { mutableStateOf(false) }
    val motion = MaterialTheme.motionScheme
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val resources = LocalResources.current
    val commonFont = remember { commonFontFamily(context) }
    val nameFont = remember { surahNameFontFamily(context) }
    val meta = pageMeta.getOrNull(currentPage - 1)
    val currentSurah = meta?.let { surahs[it.surah] }
    // Ayat untuk penghitung: di mode mushaf ayat pertama halaman; di mode daftar ayat pertama yang terlihat.
    var listFirst by remember { mutableStateOf<Pair<Int, AyahRef>?>(null) }
    val pageFirstAyah by produceState<AyahRef?>(null, currentPage) { value = vm.mushaf.firstAyahOnPage(currentPage) }
    val counterAyah = listFirst?.takeIf { listMode && it.first == currentPage }?.second ?: pageFirstAyah
    val counterMode = settings.counterMode
    val counter by produceState<CounterValue?>(null, counterAyah, counterMode, surahs) {
        val a = counterAyah
        if (a == null || surahs.isEmpty()) return@produceState
        value = CounterValue(counterMode, a, if (counterMode == CounterMode.Surah) null else vm.mushaf.divisionProgress(counterMode.kind(), a.surah, a.ayah))
    }
    val pageBookmarked = bookmarks.any { it.kind == BookmarkKind.Page && it.page == currentPage }

    Scaffold(containerColor = MaterialTheme.colorScheme.background, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { scaffoldPadding ->
        // Inset ditangani sendiri (systemBars); padding Scaffold di sini selalu nol karena contentWindowInsets kosong.
        Column(Modifier.fillMaxSize().padding(scaffoldPadding).windowInsetsPadding(WindowInsets.systemBars)) {
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
                    if (onBack != null) {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) }
                    }
                },
                actions = {
                    counter?.let { c ->
                        ReaderCounter(c, surahs[c.ayah.surah]) {
                            val next = CounterMode.entries[(counterMode.ordinal + 1) % CounterMode.entries.size]
                            scope.launch { vm.settingsRepository.setCounterMode(next) }
                        }
                    }
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
            // Halaman mushaf dan daftar ayat tetap tersusun berdampingan; berganti mode hanya memudarkan lapisan dan
            // menaikkan yang aktif ke atas (menerima sentuhan). Menyusun ulang halaman mushaf dari nol (font, ukuran)
            // memakan ~300 ms dan membuat transisi tersendat.
            // Pudar berurutan: lapisan yang keluar memudar di paruh pertama, yang masuk muncul di paruh kedua, sehingga
            // glif mushaf dan teks daftar tidak pernah tampak bertumpuk. Durasi total sama dengan sebelumnya.
            val modeProgress by animateFloatAsState(if (listMode) 1f else 0f, motion.defaultEffectsSpec(), label = "modeProgress")
            val listAlpha = (2f * modeProgress - 1f).coerceIn(0f, 1f)
            val mushafAlpha = (1f - 2f * modeProgress).coerceIn(0f, 1f)
            var listShown by remember { mutableStateOf(listMode) }
            if (listMode) listShown = true
            // Siapkan daftar ayat di belakang setelah halaman pertama tampil, agar perpindahan pertama tidak tersendat.
            LaunchedEffect(Unit) {
                delay(1_500)
                listShown = true
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .zIndex(if (listMode) 0f else 1f)
                        .graphicsLayer { alpha = mushafAlpha }
                        // Lapisan tersembunyi (alpha 0) tetap tersusun tapi tidak digambar sama sekali.
                        .drawWithContent { if (mushafAlpha > 0f) drawContent() }
                        .then(if (listMode) Modifier.clearAndSetSemantics { } else Modifier),
                ) {
                // Halaman mushaf selalu kiri-ke-kanan secara tata letak (halaman berikutnya di kiri, nama surah di kiri atas),
                // juga saat antarmuka berbahasa Arab/Urdu/Persia yang RTL.
                BoxWithConstraints(Modifier.fillMaxSize()) {
                // Ukuran blok halaman dihitung sekali di luar pager (sama untuk semua halaman), bukan per halaman.
                // Blok halaman berproporsi kertas B5 (176 x 250 mm), seperti mushaf cetak; header dan nomor menempel di sekelilingnya.
                val bleed = if (maxWidth - PageEndPadding < PageMaxWidth) PageStartBleed else 0.dp
                val available = minOf(maxWidth - PageEndPadding + bleed, PageMaxWidth)
                val fitHeight = (maxHeight - PageChromeHeight) / PageHeightOverWidth
                // Di layar pendek (lanskap ponsel) halaman yang dimuatkan ke tinggi menjadi terlalu kecil untuk dibaca;
                // halaman dibuat selebar layar dan digulir vertikal.
                val scrollPage = fitHeight < available * 0.6f
                val blockWidth = if (scrollPage) available else minOf(available, fitHeight)
                val blockHeight = blockWidth * PageHeightOverWidth
                // Di layar sempit blok menempel ke kiri; di layar lebar sisa ruang dibagi dua agar halaman tetap di tengah.
                val startOffset = ((maxWidth - blockWidth - PageEndPadding) / 2).coerceAtLeast(0.dp) - bleed
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    HorizontalPager(
                        state = pagerState,
                        reverseLayout = true,
                        beyondViewportPageCount = 1,
                        modifier = Modifier.fillMaxSize(),
                    ) { index ->
                        val page = index + 1
                        val lines by produceState<List<PageLine>?>(null, page) { value = vm.mushaf.page(page) }
                        val ayahTexts by produceState<List<AyahText>>(emptyList(), page) { value = vm.mushaf.pageAyahs(page) }
                        val pageInfo = pageMeta.getOrNull(index)
                        Box(
                            Modifier.fillMaxSize()
                                .then(if (scrollPage) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                                .pointerInput(selected) {
                                    detectTapGestures(onTap = { if (selected != null) vm.clearSelection() })
                                },
                        ) {
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
                                                .semantics { contentDescription = resources.getString(R.string.surah_info_cd, headSurah.nameLatin) },
                                        )
                                    }
                                    Box(Modifier.weight(1f))
                                    pageInfo?.let { info ->
                                        Text(
                                            text = juzTitleGlyph(info.juz),
                                            style = TextStyle(fontFamily = commonFont, fontSize = 18.sp, color = stripColor),
                                            modifier = Modifier.semantics { contentDescription = resources.getString(R.string.juz_n, info.juz) },
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
                                            pageSize = DpSize(blockWidth, blockHeight),
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
                            }
                        }
                    }
                }
                }
            }
                if (listShown) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .zIndex(if (listMode) 1f else 0f)
                            .graphicsLayer { alpha = listAlpha }
                            .drawWithContent { if (listAlpha > 0f) drawContent() }
                            .then(if (listMode) Modifier else Modifier.clearAndSetSemantics { }),
                    ) {
                AyahListReader(
                    vm = vm,
                    surahs = surahs,
                    translationIds = settings.translationIds,
                    showArabic = lastListMode == ReadingMode.AyahTranslation,
                    // Animasi teks Arab hanya saat daftar sudah terlihat; dari mushaf, daftar langsung muncul dalam mode tujuan.
                    animateArabic = listMode && listAlpha > 0f,
                    state = listPagerState,
                    targetAyah = listTargetAyah,
                    onFirstVisible = { page, ayah -> listFirst = page to ayah },
                    modifier = Modifier.fillMaxSize(),
                )
            }
                }
                // Petunjuk sekali untuk pengguna baru: mengambang di atas halaman, tidak mengubah ukurannya.
                androidx.compose.animation.AnimatedVisibility(
                    visible = !listMode && !settings.gestureHintDone,
                    enter = fadeIn(motion.defaultEffectsSpec()),
                    exit = fadeOut(motion.fastEffectsSpec()),
                    modifier = Modifier.align(Alignment.BottomCenter).zIndex(2f),
                ) {
                    GestureHint { scope.launch { vm.settingsRepository.setGestureHintDone() } }
                }
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
                extras = extras,
                onOpenAyah = { s, a -> vm.goToAyah(s, a, openSheet = true) },
                surahNames = surahs,
            )
        }
    }
}


