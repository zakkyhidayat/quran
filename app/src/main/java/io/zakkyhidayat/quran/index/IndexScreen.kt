package io.zakkyhidayat.quran.index

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.zakkyhidayat.quran.AppViewModel
import io.zakkyhidayat.quran.data.Bookmark
import io.zakkyhidayat.quran.data.BookmarkKind
import io.zakkyhidayat.quran.reader.surahNameFontFamily
import io.zakkyhidayat.quran.ui.AppIcons

private val TABS = listOf("Surah", "Juz", "Bookmark")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndexScreen(vm: AppViewModel, onBack: () -> Unit, onOpenSettings: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daftar") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali") } },
                actions = {
                    IconButton(onClick = { vm.randomAyah(); onBack() }) { Icon(AppIcons.Shuffle, contentDescription = "Ayat acak") }
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, contentDescription = "Pengaturan") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            PrimaryTabRow(selectedTabIndex = tab) {
                TABS.forEachIndexed { i, title -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(title) }) }
            }
            when (tab) {
                0 -> SurahList(vm, onBack)
                1 -> JuzList(vm, onBack)
                else -> BookmarkList(vm, onBack)
            }
        }
    }
}

@Composable
private fun SurahList(vm: AppViewModel, onBack: () -> Unit) {
    val surahs by vm.surahs.collectAsStateWithLifecycle()
    val font = remember { surahNameFontFamily(vm.getApplication()) }
    LazyColumn(Modifier.fillMaxSize()) {
        items(surahs.values.toList(), key = { it.id }) { s ->
            ListItem(
                modifier = Modifier.clickable { vm.clearSelection(); vm.goToPage(s.firstPage); onBack() },
                leadingContent = { NumberBadge(s.id) },
                headlineContent = { Text(s.nameLatin) },
                supportingContent = { Text("${s.ayahCount} ayat • Hal. ${s.firstPage}") },
                trailingContent = {
                    Text(s.nameGlyph.toString(), style = TextStyle(fontFamily = font, fontSize = 26.sp, color = MaterialTheme.colorScheme.primary))
                },
            )
        }
    }
}

@Composable
private fun JuzList(vm: AppViewModel, onBack: () -> Unit) {
    val juz by vm.juz.collectAsStateWithLifecycle()
    val surahs by vm.surahs.collectAsStateWithLifecycle()
    LazyColumn(Modifier.fillMaxSize()) {
        items(juz, key = { it.id }) { j ->
            ListItem(
                modifier = Modifier.clickable { vm.clearSelection(); vm.goToPage(j.page); onBack() },
                leadingContent = { NumberBadge(j.id) },
                headlineContent = { Text("Juz ${j.id}") },
                supportingContent = { Text("${surahs[j.surah]?.nameLatin.orEmpty()} ${j.surah}:${j.ayah} • Hal. ${j.page}") },
            )
        }
    }
}

@Composable
private fun BookmarkList(vm: AppViewModel, onBack: () -> Unit) {
    val bookmarks by vm.bookmarkStore.bookmarks.collectAsStateWithLifecycle()
    val surahs by vm.surahs.collectAsStateWithLifecycle()
    val pageMeta by vm.pageMeta.collectAsStateWithLifecycle()
    if (bookmarks.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Belum ada bookmark", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(bookmarks, key = { it.id }) { b: Bookmark ->
            val title = if (b.kind == BookmarkKind.Ayah) "${surahs[b.surah]?.nameLatin.orEmpty()} ${b.surah}:${b.ayah}" else "Halaman ${b.page}"
            val subtitle = if (b.kind == BookmarkKind.Ayah) {
                "Ayat • Hal. ${b.page}"
            } else {
                "Halaman • ${surahs[pageMeta.getOrNull(b.page - 1)?.surah]?.nameLatin.orEmpty()}"
            }
            ListItem(
                modifier = Modifier.clickable {
                    if (b.kind == BookmarkKind.Ayah) vm.goToAyah(b.surah, b.ayah) else { vm.clearSelection(); vm.goToPage(b.page) }
                    onBack()
                },
                headlineContent = { Text(title) },
                supportingContent = { Text(subtitle) },
                trailingContent = {
                    IconButton(onClick = { vm.deleteBookmark(b.id) }) { Icon(Icons.Default.Delete, contentDescription = "Hapus") }
                },
            )
        }
    }
}

@Composable
private fun NumberBadge(number: Int) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text("$number", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}
