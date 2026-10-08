package io.zakkyhidayat.quran.index

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
    "Surah" to AppIcons.MenuBook,
    "Juz" to AppIcons.GridView,
    "Hizb" to AppIcons.PieChart,
    "Manzil" to AppIcons.CalendarViewWeek,
    "Sajdah" to Icons.Default.KeyboardArrowDown,
    "Bookmark" to AppIcons.BookmarkBorder,
)
private val ListPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndexScreen(vm: AppViewModel, onBack: () -> Unit, onOpenSettings: () -> Unit, onOpenSurahInfo: (Int) -> Unit, embedded: Boolean = false) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showJump by remember { mutableStateOf(false) }
    val surahsForJump by vm.surahs.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daftar") },
                navigationIcon = {
                    if (!embedded) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali") }
                },
                actions = {
                    IconButton(onClick = { showJump = true }) { Icon(AppIcons.FormatListNumbered, contentDescription = "Lompat ke ayat") }
                    IconButton(onClick = { vm.randomAyah(); onBack() }) { Icon(AppIcons.Shuffle, contentDescription = "Ayat acak") }
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, contentDescription = "Pengaturan") }
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
                PrimaryScrollableTabRow(selectedTabIndex = tab, edgePadding = 8.dp) {
                    TABS.forEachIndexed { i, (title, icon) ->
                        LeadingIconTab(
                            selected = tab == i,
                            onClick = { tab = i },
                            text = { Text(title) },
                            icon = { Icon(icon, contentDescription = null) },
                        )
                    }
                }
                when (tab) {
                    0 -> SurahList(vm, onBack, onOpenSurahInfo)
                    1 -> JuzList(vm, onBack)
                    2 -> HizbList(vm, onBack)
                    3 -> ManzilList(vm, onBack)
                    4 -> SajdaList(vm, onBack)
                    else -> BookmarkList(vm, onBack)
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
    LazyColumn(Modifier.fillMaxSize(), contentPadding = ListPadding, verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        if (lastSurah != null && settings.lastAyah > 0) {
            item(key = "continue") {
                Card(
                    onClick = { vm.goToAyah(lastSurah.id, settings.lastAyah); onBack() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(AppIcons.MenuBook, contentDescription = null)
                        Column(Modifier.padding(start = 16.dp)) {
                            Text("Lanjutkan membaca", style = MaterialTheme.typography.titleMedium)
                            Text("${lastSurah.nameLatin} ${lastSurah.id}:${settings.lastAyah}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
        itemsIndexed(items, key = { _, s -> s.id }) { index, s ->
            SegmentedListItem(
                onClick = { vm.clearSelection(); vm.goToPage(s.firstPage); onBack() },
                shapes = ListItemDefaults.segmentedShapes(index, items.size),
                colors = segmentedItemColors(),
                leadingContent = { NumberBadge(s.id) },
                supportingContent = { Text("${s.ayahCount} ayat • Hal. ${s.firstPage}") },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(s.nameGlyph.toString(), style = TextStyle(fontFamily = font, fontSize = glyphSize, color = MaterialTheme.colorScheme.primary))
                        IconButton(onClick = { onOpenSurahInfo(s.id) }) { Icon(Icons.Default.Info, contentDescription = "Info surah ${s.nameLatin}") }
                    }
                },
            ) { Text(s.nameLatin) }
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
                supportingContent = { Text("${surahs[j.surah]?.nameLatin.orEmpty()} ${j.surah}:${j.ayah} • Hal. ${j.page}") },
                trailingContent = {
                    Text(juzOpeningGlyph(j.id), style = TextStyle(fontFamily = commonFont, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary))
                },
            ) { Text("Juz ${j.id}") }
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
            val place = "${surahs[m.surah]?.nameLatin.orEmpty()} ${m.surah}:${m.ayah} • Hal. ${m.page}"
            if (quarter == null) {
                SegmentedListItem(
                    onClick = { vm.goToAyah(m.surah, m.ayah); onBack() },
                    shapes = ListItemDefaults.segmentedShapes(index, rows.size),
                    colors = segmentedItemColors(),
                    leadingContent = { NumberBadge(m.id) },
                    supportingContent = { Text("Juz ${(m.id - 1) / 2 + 1} • $place") },
                    trailingContent = {
                        IconButton(onClick = { expanded = if (expanded == m.id) 0 else m.id }) {
                            Icon(
                                if (expanded == m.id) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (expanded == m.id) "Tutup rub'" else "Tampilkan rub'",
                            )
                        }
                    },
                ) { Text("Hizb ${m.id}") }
            } else {
                SegmentedListItem(
                    onClick = { vm.goToAyah(m.surah, m.ayah); onBack() },
                    shapes = ListItemDefaults.segmentedShapes(index, rows.size),
                    colors = segmentedItemColors(),
                    supportingContent = { Text(place) },
                    modifier = Modifier.padding(start = 24.dp),
                ) { Text("Rub' $quarter") }
            }
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
                supportingContent = { Text("${surahs[m.surah]?.nameLatin.orEmpty()} ${m.surah}:${m.ayah} • Hal. ${m.page}") },
            ) { Text("Manzil ${m.id}") }
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
                supportingContent = { Text("Hal. ${m.page}") },
            ) { Text("${surahs[m.surah]?.nameLatin.orEmpty()} ${m.surah}:${m.ayah}") }
        }
    }
}

internal fun sajdaLabel(type: String): String = if (type == "required") "Sajdah tilawah (wajib)" else "Sajdah tilawah (dianjurkan)"

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BookmarkList(vm: AppViewModel, onBack: () -> Unit) {
    val bookmarks by vm.bookmarkStore.bookmarks.collectAsStateWithLifecycle()
    val surahs by vm.surahs.collectAsStateWithLifecycle()
    val pageMeta by vm.pageMeta.collectAsStateWithLifecycle()
    if (bookmarks.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Belum ada bookmark", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = ListPadding, verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        itemsIndexed(bookmarks, key = { _, b -> b.id }) { index, b: Bookmark ->
            val title = if (b.kind == BookmarkKind.Ayah) "${surahs[b.surah]?.nameLatin.orEmpty()} ${b.surah}:${b.ayah}" else "Halaman ${b.page}"
            val subtitle = if (b.kind == BookmarkKind.Ayah) {
                "Ayat • Hal. ${b.page}"
            } else {
                "Halaman • ${surahs[pageMeta.getOrNull(b.page - 1)?.surah]?.nameLatin.orEmpty()}"
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
                    IconButton(onClick = { vm.deleteBookmark(b.id) }) { Icon(Icons.Default.Delete, contentDescription = "Hapus") }
                },
            ) { Text(title) }
        }
    }
}
