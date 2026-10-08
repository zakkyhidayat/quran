package io.zakkyhidayat.quran.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import java.util.Locale

private sealed interface CatalogState {
    data object Loading : CatalogState
    data object Failed : CatalogState
    data class Loaded(val packs: List<CatalogPack>) : CatalogState
}

/**
 * Dialog tambah terjemahan: terjemahan terpasang yang belum aktif (bundel atau unduhan), lalu paket katalog yang belum
 * terpasang. Katalog diambil saat dialog dibuka; gagal (misalnya luring) hanya menampilkan pesan dan tombol coba lagi.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun AddTranslationDialog(
    vm: AppViewModel,
    inactive: List<TranslationInfo>,
    onActivate: (String) -> Unit,
    onDismiss: () -> Unit,
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
    val downloadable = (state as? CatalogState.Loaded)?.packs.orEmpty().filter { it.id !in installedIds }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_translation)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                inactive.forEach { tr ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { onActivate(tr.id) }, modifier = Modifier.weight(1f)) {
                            Text(translationLabel(tr), modifier = Modifier.fillMaxWidth())
                        }
                        if (tr.downloaded) {
                            IconButton(onClick = { vm.deletePack(tr.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_downloaded_translation_cd, translationLabel(tr)))
                            }
                        }
                    }
                }

                when (state) {
                    CatalogState.Loading -> Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                        LoadingIndicator(Modifier.semantics { contentDescription = loadingCd })
                    }
                    CatalogState.Failed -> Column(
                        Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(stringResource(R.string.translations_catalog_error), style = MaterialTheme.typography.bodyMedium)
                        FilledTonalButton(onClick = { attempt++ }) { Text(stringResource(R.string.retry)) }
                    }
                    is CatalogState.Loaded -> {
                        if (downloadable.isNotEmpty()) {
                            Text(
                                stringResource(R.string.translations_downloadable),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 4.dp),
                            )
                        } else if (inactive.isEmpty()) {
                            Text(stringResource(R.string.translations_all_added), style = MaterialTheme.typography.bodyMedium)
                        }
                        downloadable.forEach { pack ->
                            val label = translationLabel(pack.info)
                            val busy = downloading == pack.id
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(
                                    enabled = downloading == null,
                                    onClick = {
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
                                    modifier = Modifier.weight(1f),
                                ) { Text(label, modifier = Modifier.fillMaxWidth()) }
                                if (busy) {
                                    val cd = stringResource(R.string.translation_downloading_cd, label)
                                    CircularProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier.padding(horizontal = 12.dp).size(24.dp).semantics { contentDescription = cd },
                                    )
                                } else if (pack.bytes > 0) {
                                    Text(
                                        stringResource(R.string.translation_size_mb, String.format(Locale.getDefault(), "%.1f", pack.bytes / 1_000_000.0)),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                failedLabel?.let {
                    Text(
                        stringResource(R.string.translation_download_failed, it),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}
