package io.zakkyhidayat.quran.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
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
    modifier: Modifier = Modifier,
) {
    var guideOpen by remember { mutableStateOf(false) }
    val guideTransition = remember { MutableTransitionState(false) }
    LaunchedEffect(guideOpen) { guideTransition.targetState = guideOpen }
    val tajweedLabel = stringResource(R.string.tajweed_title)
    val guideOffsetY = with(LocalDensity.current) { 56.dp.roundToPx() }
    val motion = MaterialTheme.motionScheme
    Box(
        Modifier
            .then(modifier)
            .height(48.dp),
    ) {
        Box(Modifier.align(Alignment.CenterStart)) {
            FilledTonalButton(
                onClick = { guideOpen = !guideOpen },
                shape = CircleShape,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
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
            if (guideTransition.currentState || guideTransition.targetState) {
                Popup(
                    alignment = Alignment.TopStart,
                    offset = IntOffset(0, guideOffsetY),
                    onDismissRequest = { guideOpen = false },
                    properties = PopupProperties(focusable = true),
                ) {
                    AnimatedVisibility(
                        visibleState = guideTransition,
                        enter = scaleIn(initialScale = 0.92f, animationSpec = motion.fastSpatialSpec()) +
                            fadeIn(animationSpec = motion.fastEffectsSpec()),
                        exit = scaleOut(targetScale = 0.92f, animationSpec = motion.fastSpatialSpec()) +
                            fadeOut(animationSpec = motion.fastEffectsSpec()),
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shadowElevation = 6.dp,
                            tonalElevation = 3.dp,
                            modifier = Modifier.width(minOf(LocalConfiguration.current.screenWidthDp.dp - 32.dp, 304.dp)),
                        ) {
                            Column(
                                Modifier
                                    .heightIn(max = 300.dp)
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
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
                                        style = MaterialTheme.typography.titleSmall,
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

/** Posisi baca untuk mode aktif dan semua pilihan yang tampil di menu counter. */
internal data class CounterValue(
    val mode: CounterMode,
    val ayah: AyahRef,
    val divisions: Map<CounterMode, DivisionProgress>,
)

internal fun CounterMode.kind(): MarkerKind = when (this) {
    CounterMode.Juz -> MarkerKind.Juz
    CounterMode.Hizb -> MarkerKind.Hizb
    CounterMode.Rub -> MarkerKind.Rub
    CounterMode.Manzil -> MarkerKind.Manzil
    else -> MarkerKind.Ruku
}

/** Tombol ringkas yang menampilkan hitungan aktif dan membuka pilihan jenis counter. */
@Composable
internal fun ReaderCounter(
    value: CounterValue,
    surah: Surah?,
    onModeSelect: (CounterMode) -> Unit,
) {
    val selectedProgress = value.divisions.getValue(value.mode)
    val label = counterModeLabel(value.mode)
    val n = selectedProgress.n
    val total = selectedProgress.total
    val descriptionLabel = if (value.mode == CounterMode.Surah) surah?.nameLatin.orEmpty() else label
    val description = stringResource(R.string.counter_cd, descriptionLabel, n, total)
    val textMeasurer = rememberTextMeasurer()
    val textStyle = MaterialTheme.typography.labelLarge
    val titleWidth = with(LocalDensity.current) {
        CounterMode.entries.maxOf { candidate ->
            textMeasurer.measure(
                AnnotatedString(counterModeLabel(candidate)),
                style = textStyle,
                maxLines = 1,
            ).size.width.toDp()
        }
    }
    val progressWidth = with(LocalDensity.current) {
        CounterMode.entries.maxOf { candidate ->
        val p = value.divisions.getValue(candidate)
            textMeasurer.measure(
                AnnotatedString("${p.n}/${p.total}"),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
            ).size.width.toDp()
        }
    }
    val progress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = (n.toFloat() / total.coerceAtLeast(1)).coerceIn(0f, 1f),
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "readerCounterProgress",
    )
    val progressTrackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.22f)
    val progressColor = MaterialTheme.colorScheme.onSecondaryContainer
    val counterButtonWidth = titleWidth + progressWidth + 58.dp
    var expanded by remember { mutableStateOf(false) }
    val counterTransition = remember { MutableTransitionState(false) }
    LaunchedEffect(expanded) { counterTransition.targetState = expanded }
    Box {
        Surface(
            onClick = { expanded = true },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier
                .height(48.dp)
                .semantics {
                    contentDescription = description
                    role = Role.Button
                },
        ) {
            Row(
                Modifier.width(counterButtonWidth).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Box(Modifier.size(16.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(16.dp)) {
                        drawCircle(
                            color = progressTrackColor,
                            style = Stroke(width = 2.dp.toPx()),
                        )
                        drawArc(
                            color = progressColor,
                            startAngle = -90f,
                            sweepAngle = progress * 360f,
                            useCenter = false,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                        )
                    }
                }
                Text(label, style = textStyle, maxLines = 1, modifier = Modifier.weight(1f), overflow = TextOverflow.Clip)
                Text(
                    "$n/$total",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.82f),
                    maxLines = 1,
                    modifier = Modifier.widthIn(min = progressWidth),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                )
                Icon(AppIcons.ExpandMore, contentDescription = stringResource(R.string.counter_switch), modifier = Modifier.size(18.dp))
            }
        }
        if (counterTransition.currentState || counterTransition.targetState) {
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(0, with(LocalDensity.current) { 52.dp.roundToPx() }),
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true),
            ) {
                AnimatedVisibility(
                    visibleState = counterTransition,
                    enter = scaleIn(initialScale = 0.92f, animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()) +
                        fadeIn(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()),
                    exit = scaleOut(targetScale = 0.92f, animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()) +
                        fadeOut(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()),
                ) {
                    Column(
                        Modifier
                            .width(counterButtonWidth)
                            .heightIn(max = LocalConfiguration.current.screenHeightDp.dp - 160.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        CounterMode.entries.forEach { mode ->
                            val selected = value.mode == mode
                            val title = counterModeLabel(mode)
                            val itemProgress = value.divisions.getValue(mode)
                            val progressFraction = (itemProgress.n.toFloat() / itemProgress.total.coerceAtLeast(1)).coerceIn(0f, 1f)
                            val itemTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.22f)
                            val itemProgressColor = if (selected) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                            val containerColor by animateColorAsState(
                                targetValue = if (selected) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                                animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
                                label = "counterItemColor",
                            )
                            Surface(
                                onClick = {
                                    expanded = false
                                    if (!selected) onModeSelect(mode)
                                },
                                shape = CircleShape,
                                color = containerColor,
                                contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                                tonalElevation = if (selected) 2.dp else 1.dp,
                                shadowElevation = 1.dp,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                                        Canvas(Modifier.size(24.dp)) {
                                            drawCircle(
                                                color = itemTrackColor,
                                                style = Stroke(width = 2.dp.toPx()),
                                            )
                                            drawArc(
                                                color = itemProgressColor,
                                                startAngle = -90f,
                                                sweepAngle = progressFraction * 360f,
                                                useCenter = false,
                                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                                            )
                                        }
                                        Text(
                                            itemProgress.n.toString(),
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                        )
                                    }
                                    Text(
                                        title,
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        "${itemProgress.n}/${itemProgress.total}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.78f)
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        modifier = Modifier.widthIn(min = 44.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun counterModeLabel(mode: CounterMode): String = when (mode) {
    CounterMode.Surah -> stringResource(R.string.ayah_label)
    CounterMode.Juz -> stringResource(R.string.tab_juz)
    CounterMode.Hizb -> stringResource(R.string.tab_hizb)
    CounterMode.Rub -> stringResource(R.string.tab_rub)
    CounterMode.Manzil -> stringResource(R.string.tab_manzil)
    CounterMode.Ruku -> stringResource(R.string.tab_ruku)
}
