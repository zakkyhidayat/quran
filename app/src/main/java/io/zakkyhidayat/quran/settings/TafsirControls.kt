package io.zakkyhidayat.quran.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.zakkyhidayat.quran.AppViewModel
import io.zakkyhidayat.quran.R
import io.zakkyhidayat.quran.data.CatalogPack
import io.zakkyhidayat.quran.ui.AppIcons
import io.zakkyhidayat.quran.ui.segmentedItemColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private sealed interface TafsirCatalogState {
    data object Loading : TafsirCatalogState
    data object Failed : TafsirCatalogState
    data class Loaded(val packs: List<CatalogPack>) : TafsirCatalogState
}

/**
 * Halaman Tambah tafsir (Pengaturan > Tafsir > Tambah): kolom cari dan katalog tafsir yang bisa diunduh per bahasa.
 * Unduhan dipilih otomatis hanya bila belum ada tafsir yang dipilih. Daftar terpasang ada di [TafsirDownloads].
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun TafsirAdd(vm: AppViewModel, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val locale = LocalConfiguration.current.locales[0]
    val installed by vm.tafsirs.collectAsStateWithLifecycle()
    var state by remember { mutableStateOf<TafsirCatalogState>(TafsirCatalogState.Loading) }
    var attempt by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var downloading by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableFloatStateOf(0f) }
    var downloadJob by remember { mutableStateOf<Job?>(null) }
    var failedLabel by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(attempt) {
        state = TafsirCatalogState.Loading
        state = try {
            TafsirCatalogState.Loaded(vm.catalog().filter { it.kind == "tafsir" })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.w("TafsirPacks", "Katalog tafsir gagal dimuat: $e")
            TafsirCatalogState.Failed
        }
    }

    val loadingCd = stringResource(R.string.translations_loading_cd)
    val installedIds = installed.map { it.id }.toSet()
    val downloadable = (state as? TafsirCatalogState.Loaded)?.packs.orEmpty().filter { it.id !in installedIds }
    val q = query.trim().lowercase(locale)
    val groups = remember(downloadable, query, locale) {
        val collator = java.text.Collator.getInstance(locale)
        downloadable
            .map { languageDisplayName(it.lang, it.langName, locale) to it }
            .filter { (language, pack) -> q.isEmpty() || listOf(language, pack.name, pack.source).any { it.lowercase(locale).contains(q) } }
            .groupBy({ it.first }, { it.second })
            .toSortedMap(collator)
            .map { (language, packs) -> language to packs.sortedWith { x, y -> collator.compare(x.name, y.name) } }
    }

    Column(modifier.imePadding()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(stringResource(R.string.tafsir_search_hint)) },
            singleLine = true,
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
        LazyColumn(Modifier.weight(1f)) {
            when (state) {
                TafsirCatalogState.Loading -> item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                        LoadingIndicator(Modifier.semantics { contentDescription = loadingCd })
                    }
                }
                TafsirCatalogState.Failed -> item(key = "failed") {
                    Column(
                        Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(stringResource(R.string.tafsir_catalog_error), style = MaterialTheme.typography.bodyMedium)
                        FilledTonalButton(onClick = { attempt++ }) { Text(stringResource(R.string.retry)) }
                    }
                }
                is TafsirCatalogState.Loaded -> {
                    if (groups.isEmpty()) {
                        if (query.isNotBlank()) {
                            item(key = "no-match") {
                                Text(stringResource(R.string.tafsir_no_match), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
                            }
                        }
                    } else {
                        item(key = "available-heading") { TafsirHeading(stringResource(R.string.translations_downloadable)) }
                        groups.forEach { (language, packs) ->
                            item(key = "lang-$language") {
                                Text(
                                    language,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 2.dp),
                                )
                            }
                            items(packs, key = { it.id }) { pack ->
                                val busy = downloading == pack.id
                                val start: () -> Unit = {
                                    downloadJob = scope.launch {
                                        downloading = pack.id
                                        progress = 0f
                                        failedLabel = null
                                        try {
                                            vm.installPack(pack) { done, total -> progress = if (total > 0) done.toFloat() / total else 0f }
                                            // Tafsir hanya satu: unduhan baru dipilih otomatis bila belum ada tafsir yang dipilih,
                                            // supaya tafsir yang sudah dipakai tidak diganti diam-diam.
                                            if (vm.settingsRepository.settings.first().selectedTafsir.isEmpty()) {
                                                vm.settingsRepository.setSelectedTafsir(pack.id)
                                            }
                                        } catch (e: CancellationException) {
                                            throw e
                                        } catch (e: Exception) {
                                            android.util.Log.w("TafsirPacks", "Gagal mengunduh ${pack.id}", e)
                                            failedLabel = pack.name
                                        } finally {
                                            downloading = null
                                        }
                                    }
                                }
                                ListItem(
                                    headlineContent = { Text(pack.name) },
                                    trailingContent = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (pack.bytes > 0 && !busy) {
                                                Text(
                                                    stringResource(R.string.translation_size_mb, String.format(locale, "%.1f", pack.bytes / 1_000_000.0)),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                            DownloadButton(
                                                downloading = busy,
                                                progress = progress,
                                                onStart = start,
                                                onCancel = { downloadJob?.cancel() },
                                                startDescription = stringResource(R.string.tafsir_download_cd, pack.name),
                                                cancelDescription = stringResource(R.string.cancel),
                                                enabled = downloading == null,
                                            )
                                        }
                                    },
                                    colors = tafsirItemColors(),
                                    modifier = Modifier.clickable(enabled = downloading == null, onClick = start),
                                )
                            }
                        }
                    }
                }
            }
            failedLabel?.let { name ->
                item(key = "failed-download") {
                    Text(
                        stringResource(R.string.translation_download_failed, name),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TafsirHeading(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun tafsirItemColors() = ListItemDefaults.colors(containerColor = Color.Transparent)

/**
 * Halaman Pengaturan > Tafsir: "Tanpa tafsir" dan tafsir terunduh, satu dipilih dengan radio. Geser baris menghapus
 * berkasnya dengan jeda Batalkan. Mengunduh tafsir baru ada di [TafsirAdd].
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun TafsirDownloads(vm: AppViewModel, settings: AppSettings, snackbar: SnackbarHostState, onAdd: () -> Unit) {
    val scope = rememberCoroutineScope()
    val installed by vm.tafsirs.collectAsStateWithLifecycle()
    val pending by vm.pendingDeletes.collectAsStateWithLifecycle()
    val updates by vm.packUpdates.collectAsStateWithLifecycle()
    val updater = rememberPackUpdater(vm, "TafsirPacks")
    LaunchedEffect(installed.map { it.id to it.version }) { vm.checkPackUpdates() }
    val shown = installed.filter { it.id !in pending }
    val count = 1 + shown.size
    val undo = stringResource(R.string.undo)
    val select: (String) -> Unit = { id -> scope.launch { vm.settingsRepository.setSelectedTafsir(id) } }
    Column {
        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
            SegmentedListItem(
                onClick = { select("") },
                shapes = ListItemDefaults.segmentedShapes(0, count),
                colors = segmentedItemColors(),
                trailingContent = { RadioButton(selected = settings.selectedTafsir.isEmpty(), onClick = null) },
            ) { Text(stringResource(R.string.tafsir_none)) }
            shown.forEachIndexed { index, tr ->
                key(tr.id) {
                    val deleted = stringResource(R.string.pack_deleted, tr.name)
                    val updateDescription = stringResource(R.string.update_tafsir_cd, tr.name)
                    DownloadedPackRow(
                        title = tr.name,
                        index = index + 1,
                        count = count,
                        deletable = true,
                        packId = tr.id,
                        updatePack = updates[tr.id],
                        updater = updater,
                        updateDescription = updateDescription,
                        onClick = { select(tr.id) },
                        onDelete = {
                            vm.scheduleDeletePack(tr.id)
                            showUndoDelete(scope, snackbar, vm, tr.id, deleted, undo)
                        },
                        control = { RadioButton(selected = settings.selectedTafsir == tr.id, onClick = null) },
                    )
                }
            }
        }
        PackListFooter(
            showSwipeHint = shown.isNotEmpty(),
            updateFailed = updater.failed,
            addTitle = stringResource(R.string.add_tafsir),
            onAdd = onAdd,
        )
    }
}
