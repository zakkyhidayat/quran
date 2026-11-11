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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
 * Halaman tafsir di Pengaturan, tampilannya sama dengan halaman terjemahan: kolom cari, tafsir terpasang (dipilih dengan
 * radio dan bisa dihapus), lalu katalog tafsir yang bisa diunduh per bahasa. "Tanpa tafsir" selalu di atas.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun TafsirControls(vm: AppViewModel, settings: AppSettings, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val locale = LocalConfiguration.current.locales[0]
    val installed by vm.tafsirs.collectAsStateWithLifecycle()
    var state by remember { mutableStateOf<TafsirCatalogState>(TafsirCatalogState.Loading) }
    var attempt by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<Pair<String, String>?>(null) }
    var downloading by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableFloatStateOf(0f) }
    var downloadJob by remember { mutableStateOf<Job?>(null) }
    var failedLabel by remember { mutableStateOf<String?>(null) }
    // Pembaruan paket terpasang, seperti Terjemahan: tombol perbarui di baris yang punya versi baru.
    val updates by vm.packUpdates.collectAsStateWithLifecycle()
    var updating by remember { mutableStateOf<String?>(null) }
    var updateProgress by remember { mutableFloatStateOf(0f) }
    var updateJob by remember { mutableStateOf<Job?>(null) }
    var updateFailed by remember { mutableStateOf<String?>(null) }

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

    // Cek versi katalog sekali saat daftar tampil (dan lagi bila daftar terpasang berubah), seperti Terjemahan.
    LaunchedEffect(installed.map { it.id to it.version }) { vm.checkPackUpdates() }
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
        pendingDelete?.let { (id, name) ->
            ConfirmDeletePack(
                name = name,
                onConfirm = {
                    pendingDelete = null
                    if (settings.selectedTafsir == id) scope.launch { vm.settingsRepository.setSelectedTafsir("") }
                    vm.deletePack(id)
                },
                onDismiss = { pendingDelete = null },
            )
        }
        // Seperti Terjemahan: pilihan terpasang dalam satu kelompok bersegmen, lalu bagian "Tambah" dengan kolom cari
        // dan daftar yang bisa diunduh.
        // Tanpa onClick, baris ini memakai ListItem biasa seperti Terjemahan, sehingga tombol di kanannya sejajar
        // dengan tombol unduh di daftar bawah. Pemilihan lewat radionya.
        Group {
            item(
                title = stringResource(R.string.tafsir_none),
                trailing = {
                    RadioButton(
                        selected = settings.selectedTafsir.isEmpty(),
                        onClick = { scope.launch { vm.settingsRepository.setSelectedTafsir("") } },
                    )
                },
            )
            installed.forEach { tr ->
                item(
                    title = tr.name,
                    subtitle = if (tr.id in updates) stringResource(R.string.translation_update_available) else null,
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            updates[tr.id]?.let { pack ->
                                val busy = updating == tr.id
                                DownloadButton(
                                    downloading = busy,
                                    progress = updateProgress,
                                    onStart = {
                                        updateJob = scope.launch {
                                            updating = tr.id
                                            updateProgress = 0f
                                            updateFailed = null
                                            try {
                                                vm.updatePack(pack) { done, total -> updateProgress = if (total > 0) done.toFloat() / total else 0f }
                                            } catch (e: CancellationException) {
                                                throw e
                                            } catch (e: Exception) {
                                                android.util.Log.w("TafsirPacks", "Gagal memperbarui ${pack.id}", e)
                                                updateFailed = tr.name
                                            } finally {
                                                updating = null
                                            }
                                        }
                                    },
                                    onCancel = { updateJob?.cancel() },
                                    startDescription = stringResource(R.string.update_tafsir_cd, tr.name),
                                    cancelDescription = stringResource(R.string.cancel),
                                    enabled = updating == null,
                                    icon = AppIcons.Refresh,
                                )
                            }
                            RadioButton(
                                selected = settings.selectedTafsir == tr.id,
                                onClick = { scope.launch { vm.settingsRepository.setSelectedTafsir(tr.id) } },
                            )
                            IconButton(onClick = { pendingDelete = tr.id to tr.name }) {
                                Icon(AppIcons.Delete, contentDescription = stringResource(R.string.delete_downloaded_translation_cd, tr.name))
                            }
                        }
                    },
                )
            }
        }
        Text(
            stringResource(R.string.add_tafsir),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
        )
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
