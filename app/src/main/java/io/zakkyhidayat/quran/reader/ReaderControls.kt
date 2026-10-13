package io.zakkyhidayat.quran.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.window.PopupProperties
import io.zakkyhidayat.quran.R
import io.zakkyhidayat.quran.data.AyahRef
import io.zakkyhidayat.quran.data.DivisionProgress
import io.zakkyhidayat.quran.data.MarkerKind
import io.zakkyhidayat.quran.data.Surah
import io.zakkyhidayat.quran.settings.CounterMode
import io.zakkyhidayat.quran.settings.ReadingMode
import io.zakkyhidayat.quran.ui.AppIcons

@Composable
internal fun TajweedReaderControls(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    mode: ReadingMode,
    onModeSelect: (ReadingMode) -> Unit,
    showReadingModes: Boolean,
    modifier: Modifier = Modifier,
) {
    var guideOpen by remember { mutableStateOf(false) }
    val tajweedLabel = stringResource(R.string.tajweed_title)
    val guideOffsetY = with(LocalDensity.current) { -56.dp.roundToPx() }
    Box(
        Modifier
            .then(modifier)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(48.dp),
    ) {
        Box(
            Modifier.align(Alignment.BottomStart),
        ) {
            Button(
                onClick = { guideOpen = !guideOpen },
                shape = androidx.compose.foundation.shape.CircleShape,
                contentPadding = PaddingValues(start = 16.dp, end = 12.dp),
                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
            ) {
                Text(stringResource(R.string.tajweed_reader_button), maxLines = 1)
                Spacer(Modifier.size(1.dp))
                Icon(
                    if (guideOpen) AppIcons.ExpandLess else AppIcons.ExpandMore,
                    contentDescription = stringResource(R.string.tajweed_guide_open),
                    modifier = Modifier.size(18.dp),
                )
            }
            if (guideOpen) {
                Popup(
                    alignment = Alignment.BottomStart,
                    offset = IntOffset(0, guideOffsetY),
                    onDismissRequest = { guideOpen = false },
                    properties = PopupProperties(focusable = true),
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shadowElevation = 8.dp,
                        tonalElevation = 2.dp,
                        modifier = Modifier.width(LocalConfiguration.current.screenWidthDp.dp - 32.dp),
                    ) {
                        Column(
                            Modifier
                                .heightIn(max = 300.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TajweedLegendItem(Color(0xFF999999), R.string.tajweed_legend_idgham, Modifier.weight(1f))
                                TajweedLegendItem(Color(0xFFFF9FCB), R.string.tajweed_legend_madd_two, Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TajweedLegendItem(Color(0xFFFF842E), R.string.tajweed_legend_madd_optional, Modifier.weight(1f))
                                TajweedLegendItem(Color(0xFFFF4F8B), R.string.tajweed_legend_madd_four_five, Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TajweedLegendItem(Color(0xFFE00000), R.string.tajweed_legend_madd_six, Modifier.weight(1f))
                                TajweedLegendItem(Color(0xFF20B95A), R.string.tajweed_legend_ikhfa, Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TajweedLegendItem(Color(0xFF00CDEB), R.string.tajweed_legend_qalqalah, Modifier.weight(1f))
                                TajweedLegendItem(Color(0xFF3986D8), R.string.tajweed_legend_tafkhim, Modifier.weight(1f))
                            }
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    stringResource(R.string.tajweed_title),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                                Switch(
                                    checked = enabled,
                                    onCheckedChange = onEnabledChange,
                                    modifier = Modifier.semantics { contentDescription = tajweedLabel },
                                )
                            }
                        }
                    }
                }
            }
        }
        if (showReadingModes) {
            ReadingModeBar(
                mode,
                onModeSelect,
                compact = true,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun TajweedLegendItem(color: Color, label: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Surface(Modifier.size(12.dp), shape = androidx.compose.foundation.shape.CircleShape, color = color) {}
        Text(stringResource(label), style = MaterialTheme.typography.labelMedium)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ReadingModeBar(
    mode: ReadingMode,
    onSelect: (ReadingMode) -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val options = listOf(
        Triple(ReadingMode.Mushaf, R.string.mode_mushaf, AppIcons.MenuBook),
        Triple(ReadingMode.AyahTranslation, R.string.mode_ayah_translation, AppIcons.Translate),
        Triple(ReadingMode.Translation, R.string.mode_translation, AppIcons.TextLines),
    )
    // Button group tersambung: bentuk tombol berubah (morph) dan label muncul saat dipilih.
    Row(
        modifier
            .then(if (compact) Modifier.wrapContentWidth() else Modifier.fillMaxWidth().padding(vertical = 8.dp))
            .selectableGroup(),
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
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .semantics {
                        role = Role.RadioButton
                        if (compact || !checked) contentDescription = name
                    },
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
                if (!compact) {
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
