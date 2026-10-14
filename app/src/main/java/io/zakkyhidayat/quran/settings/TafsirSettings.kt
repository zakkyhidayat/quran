package io.zakkyhidayat.quran.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.zakkyhidayat.quran.AppViewModel
import io.zakkyhidayat.quran.R
import io.zakkyhidayat.quran.data.ContentPack
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private sealed interface TafsirCatalogState {
    data object Loading : TafsirCatalogState
    data object Failed : TafsirCatalogState
    data class Loaded(val packs: List<ContentPack>) : TafsirCatalogState
}

@Composable
private fun tafsirLabel(lang: String, name: String): String =
    stringResource(R.string.translation_label, languageDisplayName(lang, null, LocalConfiguration.current.locales[0]), name)

/**
 * Halaman Tafsir di Pengaturan, sejajar dengan halaman Terjemahan: daftar tafsir terpasang (hapus, perbarui bila katalog
 * punya versi lebih baru), lalu katalog tambah yang dikelompokkan per bahasa, bisa dicari (bahasa atau pengarang), dan
 * mengunduh dengan indikator progres. Tafsir tidak punya status aktif; terpasang berarti tersedia.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun TafsirControls(vm: AppViewModel, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val installed by vm.tafsirInstalled.collectAsStateWithLifecycle()
    val updates by vm.tafsirUpdates.collectAsStateWithLifecycle()
    var state by remember { mutableStateOf<TafsirCatalogState>(TafsirCatalogState.Loading) }
    var attempt by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableFloatStateOf(0f) }
    var failed by remember { mutableStateOf<String?>(null) }
    var failedWasUpdate by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val locale = LocalConfiguration.current.locales[0]

    LaunchedEffect(attempt) {
        state = TafsirCatalogState.Loading
        state = try {
            TafsirCatalogState.Loaded(vm.tafsirCatalog())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.w("TafsirPacks", "Katalog gagal dimuat: $e; sebab: ${e.cause}")
            TafsirCatalogState.Failed
        }
    }
    // Cek versi katalog sekali setelah katalog dan daftar terpasang siap; luring dilewati diam-diam.
    LaunchedEffect(installed.map { it.id to it.version }, state is TafsirCatalogState.Loaded) { vm.checkTafsirUpdates() }

    val installedIds = installed.map { it.id }.toSet()
    val downloadable = (state as? TafsirCatalogState.Loaded)?.packs.orEmpty().filter { it.id !in installedIds }
    // Kelompok per bahasa (nama bahasa mengikuti bahasa aplikasi), urut abjad; pencarian cocok ke bahasa, kode, nama, atau pengarang.
    val groups = remember(downloadable, query, locale) {
        groupPacksByLanguage(
            downloadable, query, locale,
            languageOf = { languageDisplayName(it.lang, null, locale) },
            searchTerms = { languageSearchTerms(it.lang, null, locale) + it.name + it.source },
            nameOf = { it.name },
        )
    }
    val loadingCd = stringResource(R.string.tafsir_loading_cd)

    // Satu daftar yang bisa digulir untuk semuanya (terpasang, kolom cari, katalog) dan ruang keyboard dipasang sekali di
    // sini; sebelumnya imePadding ganda menciutkan daftar hasil menjadi kosong saat keyboard terbuka.
    LazyColumn(modifier.imePadding(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (installed.isEmpty()) {
            item(key = "none-installed") {
                Text(stringResource(R.string.tafsir_none_installed), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(installed, key = { "installed-${it.id}" }) { pack ->
                val label = tafsirLabel(pack.lang, pack.name)
                Group {
                    item(
                        title = label,
                        subtitle = if (pack.id in updates) stringResource(R.string.translation_update_available) else pack.source.takeIf { it.isNotBlank() },
                        trailing = {
                            Row {
                                updates[pack.id]?.let { update ->
                                    if (busy == pack.id) {
                                        val cd = stringResource(R.string.tafsir_updating_cd, label)
                                        CircularProgressIndicator(Modifier.size(24.dp).semantics { contentDescription = cd })
                                    } else {
                                        IconButton(
                                            enabled = busy == null,
                                            onClick = {
                                                scope.launch {
                                                    busy = pack.id
                                                    failed = null
                                                    try {
                                                        vm.installTafsir(update)
                                                    } catch (e: CancellationException) {
                                                        throw e
                                                    } catch (e: Exception) {
                                                        android.util.Log.w("TafsirPacks", "Gagal memperbarui ${pack.id}", e)
                                                        failed = label
                                                        failedWasUpdate = true
                                                    } finally {
                                                        busy = null
                                                    }
                                                }
                                            },
                                        ) { Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.update_tafsir_cd, label)) }
                                    }
                                }
                                IconButton(onClick = { vm.deleteTafsir(pack.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_tafsir_cd, label))
                                }
                            }
                        },
                    )
                }
            }
        }
        item(key = "add-title") {
            Text(
                stringResource(R.string.add_tafsir),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
            )
        }
        item(key = "search") {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.tafsir_search_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )
        }
        when (state) {
            TafsirCatalogState.Loading -> item {
                Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                    LoadingIndicator(Modifier.semantics { contentDescription = loadingCd })
                }
            }
            TafsirCatalogState.Failed -> item {
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
                    item {
                        if (query.isNotBlank()) {
                            Text(stringResource(R.string.tafsir_no_match), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
                        } else if (installed.isEmpty()) {
                            // Katalog rilis belum memuat satu pun tafsir.
                            Text(stringResource(R.string.tafsir_none_available), style = MaterialTheme.typography.bodyMedium)
                        } else {
                            Text(stringResource(R.string.tafsir_all_added), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    languageGroupedPackItems(
                        groups,
                        key = { it.id }, name = { it.name }, source = { it.source.takeIf { s -> s.isNotBlank() } }, bytes = { it.bytes },
                        label = { tafsirLabel(it.lang, it.name) },
                        busyId = busy, progress = progress, locale = locale,
                        onClick = { pack, label ->
                            scope.launch {
                                busy = pack.id
                                progress = 0f
                                failed = null
                                try {
                                    vm.installTafsir(pack) { done, total -> progress = if (total > 0) done.toFloat() / total else 0f }
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    android.util.Log.w("TafsirPacks", "Gagal mengunduh ${pack.id}", e)
                                    failed = label
                                    failedWasUpdate = false
                                } finally {
                                    busy = null
                                }
                            }
                        },
                    )
                }
            }
        }
        failed?.let {
            item(key = "failed") {
                Text(
                    stringResource(if (failedWasUpdate) R.string.translation_update_failed else R.string.translation_download_failed, it),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
