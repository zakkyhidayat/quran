package io.zakkyhidayat.quran.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.semantics.stateDescription
import io.zakkyhidayat.quran.ui.FADE_IN_MS
import io.zakkyhidayat.quran.ui.FADE_OUT_MS
import io.zakkyhidayat.quran.ui.STEP_MS
import io.zakkyhidayat.quran.ui.StepEasing
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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

/**
 * Daftar terjemahan aktif (dengan tombol hapus) dan tombol tambah. Dipakai di Pengaturan dan onboarding.
 * [inlineCatalog] = katalog mengembang di tempat (onboarding); jika tidak, tampil sebagai dialog (Pengaturan).
 */
@Composable
internal fun TranslationControls(vm: AppViewModel, settings: AppSettings, inlineCatalog: Boolean = false) {
    val repo = vm.settingsRepository
    val scope = rememberCoroutineScope()
    val translations by vm.translations.collectAsStateWithLifecycle()
    var showAddTranslation by rememberSaveable { mutableStateOf(false) }
    val updates by vm.packUpdates.collectAsStateWithLifecycle()
    var updating by remember { mutableStateOf<String?>(null) }
    var updateFailed by remember { mutableStateOf<String?>(null) }
    // Cek versi katalog sekali saat daftar tampil (dan lagi bila daftar terpasang berubah); luring dilewati diam-diam.
    LaunchedEffect(translations.map { it.id to it.version }) { vm.checkPackUpdates() }
    Column {
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
                                if (updating == tr.id) {
                                    val cd = stringResource(R.string.translation_updating_cd, label)
                                    CircularProgressIndicator(Modifier.size(24.dp).semantics { contentDescription = cd })
                                } else {
                                    IconButton(
                                        enabled = updating == null,
                                        onClick = {
                                            scope.launch {
                                                updating = tr.id
                                                updateFailed = null
                                                try {
                                                    vm.updatePack(pack)
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
                                    ) { Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.update_translation_cd, label)) }
                                }
                            }
                            IconButton(
                                onClick = { scope.launch { repo.setTranslations(ordered.filter { it in settings.translationIds && it != tr.id }) } },
                            ) { Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_translation_cd, translationLabel(tr))) }
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
        val expanded = inlineCatalog && showAddTranslation
        val stateText = stringResource(if (expanded) R.string.state_expanded else R.string.state_collapsed)
        FilledTonalButton(
            onClick = { showAddTranslation = !showAddTranslation },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).then(
                // Mode inline: tombol berperan sebagai pengendali kembang/ciut, keadaannya dibacakan TalkBack.
                if (inlineCatalog) Modifier.semantics { stateDescription = stateText } else Modifier,
            ),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.add_translation))
        }
        val onActivate: (String) -> Unit = { id ->
            // Urutan mengikuti daftar terjemahan terbaru (paket yang baru diunduh sudah masuk).
            val all = vm.translations.value.map { it.id }
            scope.launch { repo.setTranslations(all.filter { it in settings.translationIds || it == id }) }
            showAddTranslation = false
        }
        if (inlineCatalog) {
            AnimatedVisibility(
                visible = showAddTranslation,
                enter = expandVertically(tween(STEP_MS, easing = StepEasing)) + fadeIn(tween(FADE_IN_MS, delayMillis = FADE_OUT_MS, easing = LinearEasing)),
                exit = shrinkVertically(tween(STEP_MS, easing = StepEasing)) + fadeOut(tween(FADE_OUT_MS, easing = LinearEasing)),
            ) {
                // Tinggi dibatasi karena langkah onboarding sudah verticalScroll; imePadding agar kolom cari tetap terlihat.
                TranslationCatalogList(vm, inactive, onActivate, Modifier.heightIn(max = 400.dp).imePadding().padding(top = 8.dp))
            }
        } else if (showAddTranslation) {
            AddTranslationDialog(vm = vm, inactive = inactive, onActivate = onActivate, onDismiss = { showAddTranslation = false })
        }
    }
}
