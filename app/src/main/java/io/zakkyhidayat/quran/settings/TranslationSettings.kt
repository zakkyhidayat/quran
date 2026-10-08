package io.zakkyhidayat.quran.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.zakkyhidayat.quran.AppViewModel
import io.zakkyhidayat.quran.R
import kotlinx.coroutines.launch

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
        else -> tr.lang
    }
    return stringResource(R.string.translation_label, language, tr.name)
}

/** Daftar terjemahan aktif (dengan tombol hapus) dan tombol tambah. Dipakai di Pengaturan dan onboarding. */
@Composable
internal fun TranslationControls(vm: AppViewModel, settings: AppSettings) {
    val repo = vm.settingsRepository
    val scope = rememberCoroutineScope()
    val translations by vm.translations.collectAsStateWithLifecycle()
    var showAddTranslation by rememberSaveable { mutableStateOf(false) }
    Column {
        val ordered = translations.map { it.id }
        val active = translations.filter { it.id in settings.translationIds }
        val inactive = translations.filter { it.id !in settings.translationIds }
        Group {
            active.forEach { tr ->
                item(
                    title = translationLabel(tr),
                    trailing = {
                        IconButton(
                            onClick = { scope.launch { repo.setTranslations(ordered.filter { it in settings.translationIds && it != tr.id }) } },
                        ) { Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_translation_cd, translationLabel(tr))) }
                    },
                )
            }
        }
        FilledTonalButton(onClick = { showAddTranslation = true }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.add_translation))
        }
        if (showAddTranslation) {
            AddTranslationDialog(
                vm = vm,
                inactive = inactive,
                onActivate = { id ->
                    // Urutan mengikuti daftar terjemahan terbaru (paket yang baru diunduh sudah masuk).
                    val all = vm.translations.value.map { it.id }
                    scope.launch { repo.setTranslations(all.filter { it in settings.translationIds || it == id }) }
                    showAddTranslation = false
                },
                onDismiss = { showAddTranslation = false },
            )
        }
    }
}
