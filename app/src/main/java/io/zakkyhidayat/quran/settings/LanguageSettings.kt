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

/**
 * Bahasa aplikasi. Pilihan terbuka di tempat (satu kelompok di bawah barisnya), bukan dialog melayang di atas Pengaturan,
 * supaya halaman tidak tertutup dan pemakaian di onboarding sama.
 */
@Composable
internal fun LanguageSection(showTitle: Boolean = true) {
    val context = LocalContext.current
    var code by remember { mutableStateOf(AppLanguage.saved(context)) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    // Nama bahasa ditulis dalam bahasanya sendiri agar mudah ditemukan apa pun bahasa yang sedang aktif.
    val systemLabel = stringResource(R.string.language_system)
    val options = listOf("" to systemLabel) + AppLanguage.supported.map { it to AppLanguage.endonyms.getValue(it) }
    if (showTitle) SectionTitle(AppIcons.Translate, stringResource(R.string.language))
    Group {
        item(
            title = stringResource(R.string.language),
            subtitle = options.first { it.first == code }.second,
            onClick = { expanded = !expanded },
            leading = { Icon(AppIcons.Translate, contentDescription = null) },
            trailing = { Icon(if (expanded) AppIcons.ExpandLess else AppIcons.ExpandMore, contentDescription = null) },
        )
    }
    if (expanded) {
        Spacer(Modifier.height(8.dp))
        Group {
            options.forEach { (value, label) ->
                item(
                    title = label,
                    onClick = {
                        expanded = false
                        if (code != value) {
                            code = value
                            AppLanguage.apply(context, value)
                        }
                    },
                    trailing = { RadioButton(selected = code == value, onClick = null) },
                )
            }
        }
    }
}
