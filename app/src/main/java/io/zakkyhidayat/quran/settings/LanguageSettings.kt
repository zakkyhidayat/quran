package io.zakkyhidayat.quran.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import io.zakkyhidayat.quran.AppLanguage
import io.zakkyhidayat.quran.R
import io.zakkyhidayat.quran.ui.AppIcons

@Composable
internal fun LanguageSection(showTitle: Boolean = true) {
    val context = LocalContext.current
    var code by remember { mutableStateOf(AppLanguage.saved(context)) }
    var open by rememberSaveable { mutableStateOf(false) }
    // Nama bahasa ditulis dalam bahasanya sendiri agar mudah ditemukan apa pun bahasa yang sedang aktif.
    val systemLabel = stringResource(R.string.language_system)
    val options = listOf("" to systemLabel) + AppLanguage.supported.map { it to AppLanguage.endonyms.getValue(it) }
    if (showTitle) SectionTitle(AppIcons.Translate, stringResource(R.string.language))
    Group {
        item(
            title = stringResource(R.string.language),
            subtitle = options.first { it.first == code }.second,
            onClick = { open = true },
            leading = { Icon(AppIcons.Translate, contentDescription = null) },
        )
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(stringResource(R.string.language)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()).selectableGroup()) {
                    options.forEach { (value, label) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .selectable(selected = code == value, role = Role.RadioButton) {
                                    open = false
                                    if (code != value) {
                                        code = value
                                        AppLanguage.apply(context, value)
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = code == value, onClick = null)
                            Spacer(Modifier.width(16.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.close)) } },
        )
    }
}
