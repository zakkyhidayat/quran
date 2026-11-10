package io.zakkyhidayat.quran.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.zakkyhidayat.quran.AppLanguage
import io.zakkyhidayat.quran.R
import io.zakkyhidayat.quran.ui.AppIcons

/** Pilihan bahasa: kode kosong berarti mengikuti sistem; nama tiap bahasa ditulis dalam bahasanya sendiri agar mudah ditemukan. */
@Composable
private fun languageOptions(): List<Pair<String, String>> {
    val system = stringResource(R.string.language_system)
    return remember(system) { listOf("" to system) + AppLanguage.supported.map { it to AppLanguage.endonyms.getValue(it) } }
}

/**
 * Nama untuk kode bahasa [code]. Kode di luar daftar (misalnya bahasa per-aplikasi yang diatur lewat sistem ke bahasa yang
 * belum didukung) tampil apa adanya, bukan membuat layar Pengaturan gagal.
 */
internal fun languageLabel(options: List<Pair<String, String>>, code: String): String =
    options.firstOrNull { it.first == code }?.second ?: code

/** Nama bahasa aplikasi yang sedang dipakai, untuk subjudul baris Bahasa di Pengaturan. */
@Composable
internal fun currentLanguageLabel(): String = languageLabel(languageOptions(), AppLanguage.saved(LocalContext.current))

/** Daftar radio bahasa; satu-satunya tempat logika memilih bahasa, dipakai halaman Pengaturan dan onboarding. */
@Composable
private fun LanguageRadioGroup(onSelected: () -> Unit = {}) {
    val context = LocalContext.current
    var code by remember { mutableStateOf(AppLanguage.saved(context)) }
    val options = languageOptions()
    Group {
        options.forEach { (value, label) ->
            item(
                title = label,
                onClick = {
                    if (code != value) {
                        code = value
                        AppLanguage.apply(context, value)
                    }
                    onSelected()
                },
                trailing = { RadioButton(selected = code == value, onClick = null) },
            )
        }
    }
}

/** Halaman Pengaturan > Bahasa: daftar radio satu halaman penuh, bukan dialog melayang. */
@Composable
internal fun LanguageControls() = LanguageRadioGroup()

/**
 * Bahasa di onboarding, yang belum punya halaman Pengaturan: satu baris yang membuka pilihan di tempat (bukan dialog).
 * Di Pengaturan bahasa adalah halaman sendiri ([LanguageControls]).
 */
@Composable
internal fun LanguageSection(showTitle: Boolean = true) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    if (showTitle) SectionTitle(AppIcons.Language, stringResource(R.string.language))
    Group {
        item(
            title = stringResource(R.string.language),
            subtitle = currentLanguageLabel(),
            onClick = { expanded = !expanded },
            leading = { Icon(AppIcons.Language, contentDescription = null) },
            trailing = { Icon(if (expanded) AppIcons.ExpandLess else AppIcons.ExpandMore, contentDescription = null) },
        )
    }
    if (expanded) {
        Spacer(Modifier.height(8.dp))
        LanguageRadioGroup(onSelected = { expanded = false })
    }
}
