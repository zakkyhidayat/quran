package io.zakkyhidayat.quran.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.zakkyhidayat.quran.R
import io.zakkyhidayat.quran.settings.ReadingMode
import io.zakkyhidayat.quran.ui.AppIcons

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ReadingModeBar(mode: ReadingMode, onSelect: (ReadingMode) -> Unit) {
    val options = listOf(
        Triple(ReadingMode.Mushaf, R.string.mode_mushaf, AppIcons.MenuBook),
        Triple(ReadingMode.AyahTranslation, R.string.mode_ayah_translation, AppIcons.Translate),
        Triple(ReadingMode.Translation, R.string.mode_translation, AppIcons.Notes),
    )
    // Button group tersambung: bentuk tombol berubah (morph) dan label muncul saat dipilih.
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween, Alignment.CenterHorizontally),
    ) {
        options.forEachIndexed { index, (value, label, icon) ->
            val checked = mode == value
            val name = stringResource(label)
            FilledTonalToggleButton(
                checked = checked,
                onCheckedChange = { if (!checked) onSelect(value) },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                modifier = Modifier.semantics {
                    role = Role.RadioButton
                    if (!checked) contentDescription = name
                },
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(ToggleButtonDefaults.IconSize))
                AnimatedVisibility(
                    visible = checked,
                    enter = expandHorizontally(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()),
                    exit = shrinkHorizontally(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
                ) {
                    Row {
                        Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                        Text(name, maxLines = 1)
                    }
                }
            }
        }
    }
}

// Kartu petunjuk gerakan untuk pengguna baru.
@Composable
internal fun GestureHint(onDismiss: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp, // mengambang di atas teks mushaf
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.hint_title), style = MaterialTheme.typography.titleSmall)
                listOf(R.string.hint_swipe, R.string.hint_tap_ayah, R.string.hint_tap_surah).forEach {
                    Text("• " + stringResource(it), style = MaterialTheme.typography.bodyMedium)
                }
            }
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.hint_ok)) }
        }
    }
}
