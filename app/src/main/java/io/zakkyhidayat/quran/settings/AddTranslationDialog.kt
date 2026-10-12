package io.zakkyhidayat.quran.settings

import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItem
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.zakkyhidayat.quran.AppViewModel
import io.zakkyhidayat.quran.R
import io.zakkyhidayat.quran.data.CatalogPack
import io.zakkyhidayat.quran.data.TranslationInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private sealed interface CatalogState {
    data object Loading : CatalogState
    data object Failed : CatalogState
    data class Loaded(val packs: List<CatalogPack>) : CatalogState
}

/** Dialog tambah terjemahan (Pengaturan): pembungkus tipis di sekitar [TranslationCatalogList]. */
@Composable
internal fun AddTranslationDialog(
    vm: AppViewModel,
    inactive: List<TranslationInfo>,
    onActivate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_translation)) },
        text = { TranslationCatalogList(vm, inactive, onActivate) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

/**
 * Isi tambah terjemahan tanpa kerangka dialog: terjemahan terpasang yang belum aktif (bundel atau unduhan), lalu paket
 * katalog yang belum terpasang, dikelompokkan per bahasa dan bisa dicari (bahasa atau penerjemah). Katalog diambil saat
 * komposabel masuk; gagal (misalnya luring) hanya menampilkan pesan dan tombol coba lagi. Tinggi dibatasi lewat [modifier]
 * (daftarnya LazyColumn, jadi tidak boleh tinggi tak terbatas di dalam kolom yang bisa digulir).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun TranslationCatalogList(
    vm: AppViewModel,
    inactive: List<TranslationInfo>,
    onActivate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val installedIds = vm.translations.collectAsStateWithLifecycle().value.map { it.id }.toSet()
    var state by remember { mutableStateOf<CatalogState>(CatalogState.Loading) }
    var attempt by remember { mutableIntStateOf(0) }
    var downloading by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableFloatStateOf(0f) }
    var failedLabel by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(attempt) {
        state = CatalogState.Loading
        state = try {
            CatalogState.Loaded(vm.catalog())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.w("TranslationPacks", "Katalog gagal dimuat: $e; sebab: ${e.cause}")
            CatalogState.Failed
        }
    }

    val loadingCd = stringResource(R.string.translations_loading_cd)
    val locale = LocalConfiguration.current.locales[0]
    var query by remember { mutableStateOf("") }
    val downloadable = (state as? CatalogState.Loaded)?.packs.orEmpty().filter { it.id !in installedIds }
    // Kelompok per bahasa (nama bahasa mengikuti bahasa aplikasi), urut abjad; pencarian cocok ke bahasa, kode, nama, atau penerjemah.
    val groups = remember(downloadable, query, locale) {
        val q = query.trim().lowercase(locale)
        val collator = java.text.Collator.getInstance(locale)
        downloadable
            .map { languageDisplayName(it.lang, it.langName, locale) to it }
            .filter { (language, pack) ->
                q.isEmpty() || listOf(language, pack.lang, pack.langName.orEmpty(), pack.name, pack.source).any { it.lowercase(locale).contains(q) }
            }
            .groupBy({ it.first }, { it.second })
            .toSortedMap(collator)
            .map { (language, packs) -> language to packs.sortedWith { x, y -> collator.compare(x.name, y.name) } }
    }
    val inactiveShown = remember(inactive, query) {
        val q = query.trim().lowercase()
        inactive.filter { q.isEmpty() || it.name.lowercase().contains(q) || it.lang.lowercase().contains(q) || it.langName.orEmpty().lowercase().contains(q) }
    }

    Column(modifier) {
        if (state is CatalogState.Loaded) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.translations_search_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )
        }
        LazyColumn(Modifier.weight(1f, fill = false)) {
            items(inactiveShown, key = { "inactive-${it.id}" }) { tr ->
                ListItem(
                    headlineContent = { Text(translationLabel(tr)) },
                    trailingContent = if (tr.downloaded) {
                        {
                            IconButton(onClick = { vm.deletePack(tr.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_downloaded_translation_cd, translationLabel(tr)))
                            }
                        }
                    } else {
                        null
                    },
                    colors = dialogItemColors(),
                    modifier = Modifier.clickable { onActivate(tr.id) },
                )
            }

            when (state) {
                CatalogState.Loading -> item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                        LoadingIndicator(Modifier.semantics { contentDescription = loadingCd })
                    }
                }
                CatalogState.Failed -> item {
                    Column(
                        Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(stringResource(R.string.translations_catalog_error), style = MaterialTheme.typography.bodyMedium)
                        FilledTonalButton(onClick = { attempt++ }) { Text(stringResource(R.string.retry)) }
                    }
                }
                is CatalogState.Loaded -> {
                    if (groups.isEmpty()) {
                        item {
                            // Tanpa hasil: katalog kosong dan tak ada yang tersisa, atau pencarian tak cocok.
                            if (query.isNotBlank()) {
                                Text(stringResource(R.string.translations_no_match), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
                            } else if (inactive.isEmpty()) {
                                Text(stringResource(R.string.translations_all_added), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    } else {
                        item {
                            Text(
                                stringResource(R.string.translations_downloadable),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                            )
                        }
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
                                val label = translationLabel(pack.info)
                                val busy = downloading == pack.id
                                ListItem(
                                    headlineContent = { Text(pack.name) },
                                    trailingContent = {
                                        if (busy) {
                                            val cd = stringResource(R.string.translation_downloading_cd, label)
                                            CircularProgressIndicator(
                                                progress = { progress },
                                                modifier = Modifier.size(24.dp).semantics { contentDescription = cd },
                                            )
                                        } else if (pack.bytes > 0) {
                                            Text(
                                                stringResource(R.string.translation_size_mb, String.format(locale, "%.1f", pack.bytes / 1_000_000.0)),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    },
                                    colors = dialogItemColors(),
                                    modifier = Modifier.clickable(enabled = downloading == null) {
                                        scope.launch {
                                            downloading = pack.id
                                            progress = 0f
                                            failedLabel = null
                                            try {
                                                vm.installPack(pack) { done, total -> progress = if (total > 0) done.toFloat() / total else 0f }
                                                onActivate(pack.id)
                                            } catch (e: CancellationException) {
                                                throw e
                                            } catch (e: Exception) {
                                                android.util.Log.w("TranslationPacks", "Gagal mengunduh ${pack.id}", e)
                                                failedLabel = label
                                            } finally {
                                                downloading = null
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }

            failedLabel?.let {
                item {
                    Text(
                        stringResource(R.string.translation_download_failed, it),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

// Baris daftar: latar mengikuti wadah (dialog atau halaman), bukan permukaan daftar.
@Composable
private fun dialogItemColors() = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
