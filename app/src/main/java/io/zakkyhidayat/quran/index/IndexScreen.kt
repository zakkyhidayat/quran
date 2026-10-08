package io.zakkyhidayat.quran.index

import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.ui.res.stringResource
import io.zakkyhidayat.quran.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.LeadingIconTab
import androidx.compose.material3.Tab
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.mutableStateOf
import io.zakkyhidayat.quran.reader.commonFontFamily
import io.zakkyhidayat.quran.reader.juzOpeningGlyph
import io.zakkyhidayat.quran.settings.AppSettings
import io.zakkyhidayat.quran.ui.JumpToAyahDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.zakkyhidayat.quran.AppViewModel
import io.zakkyhidayat.quran.data.Bookmark
import io.zakkyhidayat.quran.data.BookmarkKind
import io.zakkyhidayat.quran.reader.surahNameFontFamily
import io.zakkyhidayat.quran.ui.AppIcons
import io.zakkyhidayat.quran.ui.CenteredContent
import io.zakkyhidayat.quran.ui.segmentedItemColors
import io.zakkyhidayat.quran.ui.NumberBadge

private val TABS = listOf(
    R.string.tab_bookmark to AppIcons.Bookmark,
    R.string.tab_surah to AppIcons.MenuBook,
    R.string.tab_juz to AppIcons.GridView,
    R.string.tab_hizb to AppIcons.PieChart,
    R.string.tab_rub to AppIcons.Quarter,
    R.string.tab_manzil to AppIcons.CalendarViewWeek,
    R.string.tab_ruku to AppIcons.FormatListNumbered,
)
// Tab Sajdah disembunyikan sementara; tempatnya mungkin dipakai fitur surah/ayat lain dari QUL. SajdaList tetap ada.
private val ListPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndexScreen(vm: AppViewModel, onOpenReader: () -> Unit, onOpenSettings: () -> Unit, onOpenSurahInfo: (Int) -> Unit) {
    val onBack = onOpenReader // pilihan di daftar membuka layar baca
    // Bookmark di urutan pertama, tetapi yang dibuka pertama kali tetap Surah.
    var tab by rememberSaveable { mutableIntStateOf(1) }
    var showJump by remember { mutableStateOf(false) }
    val surahsForJump by vm.surahs.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.index_title)) },
                actions = {
                    IconButton(onClick = { showJump = true }) { Icon(AppIcons.FormatListNumbered, contentDescription = stringResource(R.string.jump_to_ayah)) }
                    IconButton(onClick = { vm.randomAyah(); onBack() }) { Icon(AppIcons.Shuffle, contentDescription = stringResource(R.string.random_ayah)) }
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings)) }
                },
            )
        },
    ) { padding ->
        if (showJump) {
            JumpToAyahDialog(
                surahs = surahsForJump,
                initial = null,
                onDismiss = { showJump = false },
                onJump = { surah, ayah -> showJump = false; vm.goToAyah(surah, ayah); onBack() },
            )
        }
        CenteredContent(Modifier.padding(padding)) {
            Column(Modifier.fillMaxSize()) {
                PrimaryScrollableTabRow(selectedTabIndex = tab, edgePadding = 8.dp, minTabWidth = 0.dp) {
                    TABS.forEachIndexed { i, (title, icon) ->
                        if (i == 0) {
                            // Bookmark cukup ikon; namanya tetap dibacakan pembaca layar.
                            // Latar tonal seukuran tab membedakan Bookmark (koleksi pribadi) dari tab daftar isi lainnya.
                            Tab(
                                selected = tab == i,
                                onClick = { tab = i },
                                modifier = Modifier.width(64.dp).background(MaterialTheme.colorScheme.secondaryContainer),
                                selectedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                unselectedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                icon = { Icon(icon, contentDescription = stringResource(title)) },
                            )
                        } else {
                            LeadingIconTab(
                                selected = tab == i,
                                onClick = { tab = i },
                                text = { Text(stringResource(title)) },
                                icon = { Icon(icon, contentDescription = null) },
                            )
                        }
                    }
                }
                // Fade-through M3 antar tab.
                val motion = MaterialTheme.motionScheme
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = { fadeIn(motion.defaultEffectsSpec()) togetherWith fadeOut(motion.fastEffectsSpec()) },
                    label = "indexTab",
                ) { current ->
                    when (current) {
                        0 -> BookmarkList(vm, onBack)
                        1 -> SurahList(vm, onBack, onOpenSurahInfo)
                        2 -> JuzList(vm, onBack)
                        3 -> HizbList(vm, onBack)
                        4 -> RubList(vm, onBack)
                        5 -> ManzilList(vm, onBack)
                        else -> RukuList(vm, onBack)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SurahList(vm: AppViewModel, onBack: () -> Unit, onOpenSurahInfo: (Int) -> Unit) {
    val surahs by vm.surahs.collectAsStateWithLifecycle()
    val font = remember { surahNameFontFamily(vm.getApplication()) }
    val items = remember(surahs) { surahs.values.toList() }
    val settings by vm.settingsRepository.settings.collectAsStateWithLifecycle(AppSettings())
    val lastSurah = surahs[settings.lastSurah]
    val glyphSize = MaterialTheme.typography.headlineSmall.fontSize
    // Di layar sempit (< 360dp) kaligrafi nama surah menyempitkan judul sampai terpotong di tengah kata.
    val showGlyph = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 360
    Column(Modifier.fillMaxSize()) {
        // Di luar LazyColumn supaya tidak tergulir keluar saat muncul setelah data dimuat.
        if (lastSurah != null && settings.lastAyah > 0) {
            Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)) {
            Card(
                onClick = { vm.goToAyah(lastSurah.id, settings.lastAyah); onBack() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(AppIcons.MenuBook, contentDescription = null)
                    Column(Modifier.padding(start = 16.dp)) {
                        Text(stringResource(R.string.continue_reading), style = MaterialTheme.typography.titleMedium)
                        Text("${lastSurah.nameLatin} ${lastSurah.id}:${settings.lastAyah}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            }
        }
    LazyColumn(Modifier.weight(1f), contentPadding = ListPadding, verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        itemsIndexed(items, key = { _, s -> s.id }) { index, s ->
            SegmentedListItem(
                onClick = { vm.clearSelection(); vm.goToPage(s.firstPage); onBack() },
                shapes = ListItemDefaults.segmentedShapes(index, items.size),
                colors = segmentedItemColors(),
                leadingContent = { NumberBadge(s.id) },
                supportingContent = { Text(stringResource(R.string.surah_summary, s.ayahCount, s.firstPage)) },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (showGlyph) Text(s.nameGlyph.toString(), style = TextStyle(fontFamily = font, fontSize = glyphSize, color = MaterialTheme.colorScheme.primary))
                        IconButton(onClick = { onOpenSurahInfo(s.id) }) { Icon(Icons.Default.Info, contentDescription = stringResource(R.string.surah_info_cd, s.nameLatin)) }
                    }
                },
            ) { Text(s.nameLatin) }
        }
    }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun JuzList(vm: AppViewModel, onBack: () -> Unit) {
    val juz by vm.juz.collectAsStateWithLifecycle()
    val commonFont = remember { commonFontFamily(vm.getApplication()) }
    val surahs by vm.surahs.collectAsStateWithLifecycle()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = ListPadding, verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        itemsIndexed(juz, key = { _, j -> j.id }) { index, j ->
            SegmentedListItem(
                onClick = { vm.clearSelection(); vm.goToPage(j.page); onBack() },
                shapes = ListItemDefaults.segmentedShapes(index, juz.size),
                colors = segmentedItemColors(),
                leadingContent = { NumberBadge(j.id) },
                supportingContent = { Text(stringResource(R.string.ref_place, surahs[j.surah]?.nameLatin.orEmpty(), j.surah, j.ayah, j.page)) },
                trailingContent = {
                    Text(juzOpeningGlyph(j.id), style = TextStyle(fontFamily = commonFont, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary))
                },
            ) { Text(stringResource(R.string.juz_n, j.id)) }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HizbList(vm: AppViewModel, onBack: () -> Unit) {
    val hizb by vm.hizb.collectAsStateWithLifecycle()
    val rub by vm.rub.collectAsStateWithLifecycle()
    val surahs by vm.surahs.collectAsStateWithLifecycle()
    var expanded by rememberSaveable { mutableIntStateOf(0) }
    // Baris hizb; hizb yang dibuka menampilkan tiga rub' di dalamnya (1/4, 1/2, 3/4).
    val rows = remember(hizb, rub, expanded) {
        buildList {
            hizb.forEach { h ->
                add(h to null as String?)
                if (h.id == expanded) {
                    val base = (h.id - 1) * 4
                    listOf("¼" to 1, "½" to 2, "¾" to 3).forEach { (label, offset) -> rub.getOrNull(base + offset)?.let { add(it to label) } }
                }
            }
        }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = ListPadding, verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        itemsIndexed(rows, key = { _, r -> if (r.second == null) "h${r.first.id}" else "r${r.first.id}" }) { index, (m, quarter) ->
            val place = stringResource(R.string.ref_place, surahs[m.surah]?.nameLatin.orEmpty(), m.surah, m.ayah, m.page)
            if (quarter == null) {
                SegmentedListItem(
                    onClick = { vm.goToAyah(m.surah, m.ayah); onBack() },
                    shapes = ListItemDefaults.segmentedShapes(index, rows.size),
                    colors = segmentedItemColors(),
                    leadingContent = { NumberBadge(m.id) },
                    supportingContent = { Text(stringResource(R.string.juz_place, (m.id - 1) / 2 + 1, place)) },
                    trailingContent = {
                        IconButton(onClick = { expanded = if (expanded == m.id) 0 else m.id }) {
                            Icon(
                                if (expanded == m.id) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (expanded == m.id) stringResource(R.string.hide_rub) else stringResource(R.string.show_rub),
                            )
                        }
                    },
                ) { Text(stringResource(R.string.hizb_n, m.id)) }
            } else {
                SegmentedListItem(
                    onClick = { vm.goToAyah(m.surah, m.ayah); onBack() },
                    shapes = ListItemDefaults.segmentedShapes(index, rows.size),
                    colors = segmentedItemColors(),
                    supportingContent = { Text(place) },
                    modifier = Modifier.padding(start = 24.dp),
                ) { Text(stringResource(R.string.rub_n, quarter)) }
            }
        }
    }
}

// Rub' (seperempat hizb): 240 bagian, masing-masing dengan juz dan hizb tempatnya.
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RubList(vm: AppViewModel, onBack: () -> Unit) {
    val rub by vm.rub.collectAsStateWithLifecycle()
    val surahs by vm.surahs.collectAsStateWithLifecycle()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = ListPadding, verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        itemsIndexed(rub, key = { _, m -> m.id }) { index, m ->
            val hizb = (m.id - 1) / 4 + 1
            SegmentedListItem(
                onClick = { vm.goToAyah(m.surah, m.ayah); onBack() },
                shapes = ListItemDefaults.segmentedShapes(index, rub.size),
                colors = segmentedItemColors(),
                leadingContent = { NumberBadge(m.id) },
                overlineContent = { Text(stringResource(R.string.juz_hizb, (hizb - 1) / 2 + 1, hizb)) },
                supportingContent = { Text(stringResource(R.string.ref_place, surahs[m.surah]?.nameLatin.orEmpty(), m.surah, m.ayah, m.page)) },
            ) { Text(stringResource(R.string.rub_n, "${(m.id - 1) % 4 + 1}/4")) }
        }
    }
}

// Ruku: 558 bagian; extra berisi nomor ruku di dalam surahnya.
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RukuList(vm: AppViewModel, onBack: () -> Unit) {
    val ruku by vm.ruku.collectAsStateWithLifecycle()
    val surahs by vm.surahs.collectAsStateWithLifecycle()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = ListPadding, verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        itemsIndexed(ruku, key = { _, m -> m.id }) { index, m ->
            SegmentedListItem(
                onClick = { vm.goToAyah(m.surah, m.ayah); onBack() },
                shapes = ListItemDefaults.segmentedShapes(index, ruku.size),
                colors = segmentedItemColors(),
                leadingContent = { NumberBadge(m.id) },
                supportingContent = { Text(stringResource(R.string.ref_place, surahs[m.surah]?.nameLatin.orEmpty(), m.surah, m.ayah, m.page)) },
            ) { Text(stringResource(R.string.ruku_in_surah, surahs[m.surah]?.nameLatin.orEmpty(), m.extra.toIntOrNull() ?: 0)) }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ManzilList(vm: AppViewModel, onBack: () -> Unit) {
    val manzil by vm.manzil.collectAsStateWithLifecycle()
    val surahs by vm.surahs.collectAsStateWithLifecycle()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = ListPadding, verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        itemsIndexed(manzil, key = { _, m -> m.id }) { index, m ->
            SegmentedListItem(
                onClick = { vm.goToAyah(m.surah, m.ayah); onBack() },
                shapes = ListItemDefaults.segmentedShapes(index, manzil.size),
                colors = segmentedItemColors(),
                leadingContent = { NumberBadge(m.id) },
                supportingContent = { Text(stringResource(R.string.ref_place, surahs[m.surah]?.nameLatin.orEmpty(), m.surah, m.ayah, m.page)) },
            ) { Text(stringResource(R.string.manzil_n, m.id)) }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SajdaList(vm: AppViewModel, onBack: () -> Unit) {
    val sajda by vm.sajda.collectAsStateWithLifecycle()
    val surahs by vm.surahs.collectAsStateWithLifecycle()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = ListPadding, verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        itemsIndexed(sajda, key = { _, m -> m.id }) { index, m ->
            SegmentedListItem(
                onClick = { vm.goToAyah(m.surah, m.ayah, openSheet = true); onBack() },
                shapes = ListItemDefaults.segmentedShapes(index, sajda.size),
                colors = segmentedItemColors(),
                leadingContent = { NumberBadge(m.id) },
                overlineContent = { Text(sajdaLabel(m.extra)) },
                supportingContent = { Text(stringResource(R.string.page_short, m.page)) },
            ) { Text("${surahs[m.surah]?.nameLatin.orEmpty()} ${m.surah}:${m.ayah}") }
        }
    }
}

@Composable
internal fun sajdaLabel(type: String): String = stringResource(if (type == "required") R.string.sajda_required else R.string.sajda_recommended)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BookmarkList(vm: AppViewModel, onBack: () -> Unit) {
    val bookmarks by vm.bookmarkStore.bookmarks.collectAsStateWithLifecycle()
    val surahs by vm.surahs.collectAsStateWithLifecycle()
    val pageMeta by vm.pageMeta.collectAsStateWithLifecycle()
    if (bookmarks.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_bookmarks), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = ListPadding, verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        itemsIndexed(bookmarks, key = { _, b -> b.id }) { index, b: Bookmark ->
            val title = if (b.kind == BookmarkKind.Ayah) "${surahs[b.surah]?.nameLatin.orEmpty()} ${b.surah}:${b.ayah}" else stringResource(R.string.page_n, b.page)
            val subtitle = if (b.kind == BookmarkKind.Ayah) {
                stringResource(R.string.bookmark_ayah_sub, b.page)
            } else {
                stringResource(R.string.bookmark_page_sub, surahs[pageMeta.getOrNull(b.page - 1)?.surah]?.nameLatin.orEmpty())
            }
            SegmentedListItem(
                onClick = {
                    if (b.kind == BookmarkKind.Ayah) vm.goToAyah(b.surah, b.ayah) else { vm.clearSelection(); vm.goToPage(b.page) }
                    onBack()
                },
                shapes = ListItemDefaults.segmentedShapes(index, bookmarks.size),
                colors = segmentedItemColors(),
                supportingContent = { Text(subtitle) },
                trailingContent = {
                    IconButton(onClick = { vm.deleteBookmark(b.id) }) { Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete)) }
                },
            ) { Text(title) }
        }
    }
}
