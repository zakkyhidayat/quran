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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.draw.clip
import io.zakkyhidayat.quran.data.AyahRef
import io.zakkyhidayat.quran.data.DivisionProgress
import io.zakkyhidayat.quran.data.MarkerKind
import io.zakkyhidayat.quran.data.Surah
import io.zakkyhidayat.quran.settings.CounterMode
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

/** Isi penghitung: untuk [CounterMode.Surah] cukup ayat dan jumlah ayat surah; selain itu [division] dari repositori. */
internal data class CounterValue(val mode: CounterMode, val ayah: AyahRef, val division: DivisionProgress?)

internal fun CounterMode.kind(): MarkerKind = when (this) {
    CounterMode.Juz -> MarkerKind.Juz
    CounterMode.Hizb -> MarkerKind.Hizb
    CounterMode.Rub -> MarkerKind.Rub
    CounterMode.Manzil -> MarkerKind.Manzil
    else -> MarkerKind.Ruku
}

/** Tombol teks ringkas di bilah atas: "12/286" atau "Juz 3 · 45/148"; diketuk untuk berganti jenis. */
@Composable
internal fun ReaderCounter(value: CounterValue, surah: Surah?, onSwitch: () -> Unit) {
    val d = value.division
    val label = when (value.mode) {
        CounterMode.Surah -> null
        CounterMode.Juz -> stringResource(R.string.juz_n, d?.index ?: 0)
        CounterMode.Hizb -> stringResource(R.string.hizb_n, d?.index ?: 0)
        CounterMode.Rub -> stringResource(R.string.rub_n, (d?.index ?: 0).toString())
        CounterMode.Manzil -> stringResource(R.string.manzil_n, d?.index ?: 0)
        CounterMode.Ruku -> stringResource(R.string.counter_ruku, d?.index ?: 0)
    }
    val n = d?.n ?: value.ayah.ayah
    val total = d?.total ?: surah?.ayahCount ?: 0
    val text = if (label == null) "$n/$total" else "$label · $n/$total"
    val description = stringResource(R.string.counter_cd, label ?: surah?.nameLatin.orEmpty(), n, total)
    Box(
        Modifier
            .heightIn(min = 48.dp)
            .clip(MaterialTheme.shapes.large)
            .clickable(onClickLabel = stringResource(R.string.counter_switch), role = Role.Button, onClick = onSwitch)
            .padding(horizontal = 8.dp)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1, color = MaterialTheme.colorScheme.primary)
    }
}
