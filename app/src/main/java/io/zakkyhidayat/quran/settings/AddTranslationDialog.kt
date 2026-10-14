package io.zakkyhidayat.quran.settings

import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItem
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.runtime.Stable
import kotlinx.coroutines.CoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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

internal sealed interface CatalogState {
    data object Loading : CatalogState
    data object Failed : CatalogState
    data class Loaded(val packs: List<CatalogPack>) : CatalogState
}

/** Keadaan katalog unduhan (dimuat, dicari, diunduh) yang dipakai bersama oleh daftar mandiri dan halaman Pengaturan. */
@Stable
internal class TranslationCatalog(val scope: CoroutineScope, val locale: java.util.Locale) {
    var state by mutableStateOf<CatalogState>(CatalogState.Loading)
    var attempt by mutableIntStateOf(0)
    var downloading by mutableStateOf<String?>(null)
    var progress by mutableFloatStateOf(0f)
    var failedLabel by mutableStateOf<String?>(null)
    var query by mutableStateOf("")
    var installedIds: Set<String> = emptySet()
    // State supaya daftar lazy tersusun ulang saat hasil pencarian berubah.
    var groups by mutableStateOf<List<Pair<String, List<CatalogPack>>>>(emptyList())
    var inactiveShown by mutableStateOf<List<TranslationInfo>>(emptyList())
}

/**
 * Memuat katalog (sekali per percobaan) dan menghitung kelompok per bahasa (nama bahasa mengikuti bahasa aplikasi, urut
 * abjad; pencarian cocok ke nama bahasa dalam beberapa bahasa, kode, nama, atau penerjemah) serta terjemahan terpasang yang
 * belum aktif. Gagal (misalnya luring) hanya mengubah status menjadi [CatalogState.Failed].
 */
@Composable
internal fun rememberTranslationCatalog(vm: AppViewModel, inactive: List<TranslationInfo>): TranslationCatalog {
    val locale = LocalConfiguration.current.locales[0]
    val scope = rememberCoroutineScope()
    val catalog = remember { TranslationCatalog(scope, locale) }
    catalog.installedIds = vm.translations.collectAsStateWithLifecycle().value.map { it.id }.toSet()

    LaunchedEffect(catalog.attempt) {
        catalog.state = CatalogState.Loading
        catalog.state = try {
            CatalogState.Loaded(vm.catalog())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.w("TranslationPacks", "Katalog gagal dimuat: $e; sebab: ${e.cause}")
            CatalogState.Failed
        }
    }

    val query = catalog.query
    val downloadable = (catalog.state as? CatalogState.Loaded)?.packs.orEmpty().filter { it.id !in catalog.installedIds }
    catalog.groups = remember(downloadable, query, locale) {
        groupPacksByLanguage(
            downloadable, query, locale,
            languageOf = { languageDisplayName(it.lang, it.langName, locale) },
            searchTerms = { languageSearchTerms(it.lang, it.langName, locale) + it.name + it.source },
            nameOf = { it.name },
        )
    }
    catalog.inactiveShown = remember(inactive, query, locale) {
        val q = query.trim().lowercase(locale)
        inactive.filter { tr -> q.isEmpty() || (languageSearchTerms(tr.lang, tr.langName, locale) + tr.name).any { it.lowercase(locale).contains(q) } }
    }
    return catalog
}

@Composable
internal fun TranslationSearchField(catalog: TranslationCatalog, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = catalog.query,
        onValueChange = { catalog.query = it },
        placeholder = { Text(stringResource(R.string.translations_search_hint)) },
        singleLine = true,
        modifier = modifier.fillMaxWidth().padding(bottom = 8.dp),
    )
}

/**
 * Isi tambah terjemahan tanpa kerangka dialog, dengan kolom cari tetap di atas daftar yang bisa digulir: terjemahan terpasang
 * yang belum aktif (bundel atau unduhan), lalu paket katalog yang belum terpasang per bahasa. Tinggi dibatasi lewat [modifier]
 * (daftarnya LazyColumn, jadi tidak boleh tinggi tak terbatas di dalam kolom yang bisa digulir). Dipakai onboarding; halaman
 * Pengaturan memakai [translationCatalogItems] di dalam satu daftar bersama bagian lainnya.
 */
@Composable
internal fun TranslationCatalogList(
    vm: AppViewModel,
    inactive: List<TranslationInfo>,
    onActivate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val catalog = rememberTranslationCatalog(vm, inactive)
    Column(modifier) {
        TranslationSearchField(catalog)
        LazyColumn(Modifier.weight(1f)) { translationCatalogItems(vm, catalog, inactive, onActivate) }
    }
}

/** Baris-baris katalog: terjemahan terpasang tak aktif, status muat, kelompok bahasa dengan paket unduhan, dan galat unduh. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun LazyListScope.translationCatalogItems(
    vm: AppViewModel,
    catalog: TranslationCatalog,
    inactive: List<TranslationInfo>,
    onActivate: (String) -> Unit,
) {
    val scope = catalog.scope
    val locale = catalog.locale
    val groups = catalog.groups
    items(catalog.inactiveShown, key = { "inactive-${it.id}" }) { tr ->
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

    when (catalog.state) {
        CatalogState.Loading -> item(key = "catalog-loading") {
            val loadingCd = stringResource(R.string.translations_loading_cd)
            Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                LoadingIndicator(Modifier.semantics { contentDescription = loadingCd })
            }
        }
        CatalogState.Failed -> item(key = "catalog-failed") {
            Column(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.translations_catalog_error), style = MaterialTheme.typography.bodyMedium)
                FilledTonalButton(onClick = { catalog.attempt++ }) { Text(stringResource(R.string.retry)) }
            }
        }
        is CatalogState.Loaded -> {
            if (groups.isEmpty()) {
                item(key = "catalog-empty") {
                    // Tanpa hasil: katalog kosong dan tak ada yang tersisa, atau pencarian tak cocok.
                    if (catalog.query.isNotBlank()) {
                        Text(stringResource(R.string.translations_no_match), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
                    } else if (inactive.isEmpty()) {
                        Text(stringResource(R.string.translations_all_added), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                languageGroupedPackItems(
                    groups,
                    key = { it.id }, name = { it.name }, source = { null }, bytes = { it.bytes },
                    label = { translationLabel(it.info) },
                    busyId = catalog.downloading, progress = catalog.progress, locale = locale,
                    onClick = { pack, label ->
                        scope.launch {
                            catalog.downloading = pack.id
                            catalog.progress = 0f
                            catalog.failedLabel = null
                            try {
                                vm.installPack(pack) { done, total -> catalog.progress = if (total > 0) done.toFloat() / total else 0f }
                                onActivate(pack.id)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                android.util.Log.w("TranslationPacks", "Gagal mengunduh ${pack.id}", e)
                                catalog.failedLabel = label
                            } finally {
                                catalog.downloading = null
                            }
                        }
                    },
                )
            }
        }
    }

    catalog.failedLabel?.let {
        item(key = "download-failed") {
            Text(
                stringResource(R.string.translation_download_failed, it),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

// Baris daftar: latar mengikuti wadah (dialog atau halaman), bukan permukaan daftar.
@Composable
private fun dialogItemColors() = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
