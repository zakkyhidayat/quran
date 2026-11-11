package io.zakkyhidayat.quran.settings

import androidx.compose.material3.MaterialTheme
import io.zakkyhidayat.quran.ui.AppIcons
import androidx.compose.ui.Alignment
import io.zakkyhidayat.quran.data.CatalogPack
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.zakkyhidayat.quran.AppViewModel
import io.zakkyhidayat.quran.R
import kotlinx.coroutines.launch

/**
 * Nama bahasa untuk kode ISO 639 dalam bahasa tampilan [locale]. Kode yang tidak dikenal Locale (hasilnya kosong atau
 * sama dengan kodenya, mis. "mos") memakai [fallback] (nama Inggris dari katalog), lalu kodenya sendiri.
 */
internal fun languageDisplayName(code: String, fallback: String?, locale: java.util.Locale): String {
    val name = java.util.Locale.forLanguageTag(code).getDisplayLanguage(locale)
    return if (name.isBlank() || name.equals(code, ignoreCase = true)) fallback?.takeIf { it.isNotBlank() } ?: code else name
}

@Composable
internal fun translationLabel(tr: io.zakkyhidayat.quran.data.TranslationInfo): String {
    val language = when (tr.lang) {
        "id" -> stringResource(R.string.lang_name_id)
        "en" -> stringResource(R.string.lang_name_en)
        "ur" -> stringResource(R.string.lang_name_ur)
        "bn" -> stringResource(R.string.lang_name_bn)
        "tr" -> stringResource(R.string.lang_name_tr)
        "fa" -> stringResource(R.string.lang_name_fa)
        "ms" -> stringResource(R.string.lang_name_ms)
        "fr" -> stringResource(R.string.lang_name_fr)
        "ru" -> stringResource(R.string.lang_name_ru)
        "ar" -> stringResource(R.string.lang_name_ar)
        else -> languageDisplayName(tr.lang, tr.langName, LocalConfiguration.current.locales[0])
    }
    return stringResource(R.string.translation_label, language, tr.name)
}

/** Bahasa tema dan topik ayat: bahasa aplikasi, bahasa sistem, atau salah satu bahasa yang punya terjemahan. */
@Composable
internal fun ExtrasLanguageRow(vm: AppViewModel, settings: AppSettings) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    // Hanya Inggris dan Indonesia untuk sekarang. Nilai tersimpan "app" atau "system" tetap diterima dan dipetakan ke salah satunya.
    val resolved = io.zakkyhidayat.quran.AppLanguage.extrasLanguage(settings.extrasLanguage, context)
    Labeled(stringResource(R.string.extras_language)) {
        Group {
            listOf("en" to stringResource(R.string.lang_name_en), "id" to stringResource(R.string.lang_name_id)).forEach { (code, label) ->
                item(
                    title = label,
                    onClick = { scope.launch { vm.settingsRepository.setExtrasLanguage(code) } },
                    trailing = { RadioButton(selected = resolved == code, onClick = null) },
                )
            }
        }
        Text(
            stringResource(R.string.extras_language_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 6.dp),
        )
    }
}

/**
 * Daftar terjemahan aktif (dengan tombol hapus) dan katalog tambah yang selalu tampil di tempat.
 */
@Composable
internal fun TranslationControls(
    vm: AppViewModel,
    settings: AppSettings,
    modifier: Modifier = Modifier,
    fillAvailableHeight: Boolean = false,
) {
    val repo = vm.settingsRepository
    val scope = rememberCoroutineScope()
    val translations by vm.translations.collectAsStateWithLifecycle()
    val updates by vm.packUpdates.collectAsStateWithLifecycle()
    var updating by remember { mutableStateOf<String?>(null) }
    var updateProgress by remember { mutableFloatStateOf(0f) }
    var updateJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var updateFailed by remember { mutableStateOf<String?>(null) }
    // Cek versi katalog sekali saat daftar tampil (dan lagi bila daftar terpasang berubah); luring dilewati diam-diam.
    LaunchedEffect(translations.map { it.id to it.version }) { vm.checkPackUpdates() }
    Column(modifier) {
        val ordered = translations.map { it.id }
        val active = translations.filter { it.id in settings.translationIds }
        val inactive = translations.filter { it.id !in settings.translationIds }
        Group {
            active.forEach { tr ->
                val label = translationLabel(tr)
                item(
                    title = label,
                    subtitle = if (tr.id in updates) stringResource(R.string.translation_update_available) else null,
                    trailing = {
                        Row {
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
                                            } catch (e: kotlinx.coroutines.CancellationException) {
                                                throw e
                                            } catch (e: Exception) {
                                                android.util.Log.w("TranslationPacks", "Gagal memperbarui ${pack.id}", e)
                                                updateFailed = label
                                            } finally {
                                                updating = null
                                            }
                                        }
                                    },
                                    onCancel = { updateJob?.cancel() },
                                    startDescription = stringResource(R.string.update_translation_cd, label),
                                    cancelDescription = stringResource(R.string.cancel),
                                    enabled = updating == null,
                                    icon = AppIcons.Refresh,
                                )
                            }
                            IconButton(
                                onClick = { scope.launch { repo.setTranslations(ordered.filter { it in settings.translationIds && it != tr.id }) } },
                            ) { Icon(AppIcons.Delete, contentDescription = stringResource(R.string.delete_translation_cd, translationLabel(tr))) }
                        }
                    },
                )
            }
        }
        updateFailed?.let {
            Text(
                stringResource(R.string.translation_update_failed, it),
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Text(
            stringResource(R.string.add_translation),
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
        )
        val onActivate: (String) -> Unit = { id ->
            // Urutan mengikuti daftar terjemahan terbaru (paket yang baru diunduh sudah masuk).
            val all = vm.translations.value.map { it.id }
            scope.launch { repo.setTranslations(all.filter { it in settings.translationIds || it == id }) }
        }
        // Daftar lazy dibatasi agar bisa digulir di Pengaturan maupun onboarding.
        val catalogModifier = if (fillAvailableHeight) {
            Modifier.weight(1f).imePadding()
        } else {
            Modifier.heightIn(max = 400.dp).imePadding()
        }
        TranslationCatalogList(vm, inactive, onActivate, catalogModifier)
    }
}


/**
 * Halaman Pengaturan > Terjemahan: hanya yang sudah terunduh. Sakelar mengaktifkan atau menonaktifkan (urutan mengikuti
 * daftar), geser baris menghapus berkasnya dengan jeda Batalkan. Mengunduh terjemahan baru ada di [TranslationAdd].
 * Onboarding tetap memakai [TranslationControls], karena belum punya halaman Pengaturan.
 */
@Composable
internal fun TranslationDownloads(vm: AppViewModel, settings: AppSettings, snackbar: SnackbarHostState, onAdd: () -> Unit) {
    val repo = vm.settingsRepository
    val scope = rememberCoroutineScope()
    val translations by vm.translations.collectAsStateWithLifecycle()
    val pending by vm.pendingDeletes.collectAsStateWithLifecycle()
    val updates by vm.packUpdates.collectAsStateWithLifecycle()
    val updater = rememberPackUpdater(vm, "TranslationPacks")
    LaunchedEffect(translations.map { it.id to it.version }) { vm.checkPackUpdates() }
    val shown = translations.filter { it.id !in pending }
    val ordered = translations.map { it.id }
    val undo = stringResource(R.string.undo)
    Column {
        Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(androidx.compose.material3.ListItemDefaults.SegmentedGap)) {
            shown.forEachIndexed { index, tr ->
                androidx.compose.runtime.key(tr.id) {
                    val label = translationLabel(tr)
                    val active = tr.id in settings.translationIds
                    val deleted = stringResource(R.string.pack_deleted, label)
                    val updateDescription = stringResource(R.string.update_translation_cd, label)
                    DownloadedPackRow(
                        title = label,
                        index = index,
                        count = shown.size,
                        deletable = tr.downloaded,
                        packId = tr.id,
                        updatePack = updates[tr.id],
                        updater = updater,
                        updateDescription = updateDescription,
                        onClick = {
                            scope.launch {
                                val next = if (active) {
                                    ordered.filter { it in settings.translationIds && it != tr.id }
                                } else {
                                    ordered.filter { it in settings.translationIds || it == tr.id }
                                }
                                repo.setTranslations(next)
                            }
                        },
                        onDelete = {
                            vm.scheduleDeletePack(tr.id)
                            showUndoDelete(scope, snackbar, vm, tr.id, deleted, undo)
                        },
                        control = { IconSwitch(active) },
                    )
                }
            }
        }
        PackListFooter(
            showSwipeHint = shown.any { it.downloaded },
            updateFailed = updater.failed,
            addTitle = stringResource(R.string.add_translation),
            onAdd = onAdd,
        )
    }
}

/** Halaman Pengaturan > Terjemahan > Tambah: kolom cari dan katalog; yang diunduh langsung aktif. */
@Composable
internal fun TranslationAdd(vm: AppViewModel, settings: AppSettings, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val onActivate: (String) -> Unit = { id ->
        // Urutan mengikuti daftar terjemahan terbaru (paket yang baru diunduh sudah masuk).
        val all = vm.translations.value.map { it.id }
        scope.launch { vm.settingsRepository.setTranslations(all.filter { it in settings.translationIds || it == id }) }
    }
    TranslationCatalogList(vm, emptyList(), onActivate, modifier.imePadding())
}
