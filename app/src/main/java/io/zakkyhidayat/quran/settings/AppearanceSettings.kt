package io.zakkyhidayat.quran.settings

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.zakkyhidayat.quran.AppViewModel
import io.zakkyhidayat.quran.R
import io.zakkyhidayat.quran.ui.AppIcons
import io.zakkyhidayat.quran.ui.scaled
import io.zakkyhidayat.quran.ui.theme.originalColorScheme
import kotlinx.coroutines.launch

@Composable
private fun PaletteCard(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean,
    scheme: ColorScheme?,
    caption: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.semantics { role = Role.RadioButton; this.selected = selected },
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
            contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
        ),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                if (selected) Icon(Icons.Default.Check, contentDescription = stringResource(R.string.selected), modifier = Modifier.size(20.dp))
            }
            if (scheme != null) {
                PaletteSwatches(scheme)
            } else {
                Spacer(Modifier.height(32.dp))
            }
            // Warna isi kartu (onSecondaryContainer saat terpilih) agar kontras keterangan tetap cukup.
            Text(caption, style = MaterialTheme.typography.bodySmall, color = androidx.compose.material3.LocalContentColor.current.copy(alpha = 0.8f))
        }
    }
}

// Pratinjau palet: primary, secondary, tertiary, wadah utama, dan permukaan.
@Composable
private fun PaletteSwatches(scheme: ColorScheme) {
    val colors = listOf(scheme.primary, scheme.secondary, scheme.tertiary, scheme.primaryContainer, scheme.surfaceContainerHighest)
    Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
        colors.forEach { color ->
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = color,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {}
        }
    }
}

/** Ukuran teks Arab dan terjemahan (daftar ayat dan lembar ayat), masing-masing 80-160%, dengan pratinjau. */
@Composable
internal fun TextSizeControls(vm: AppViewModel, settings: AppSettings) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val arabicFont = remember { io.zakkyhidayat.quran.reader.arabicFontFamily(context) }
    TextSizeSlider(
        label = stringResource(R.string.arabic_text_size),
        percent = settings.arabicTextPercent,
        onChange = { scope.launch { vm.settingsRepository.setArabicTextPercent(it) } },
    ) { scale ->
        androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
            Text(
                "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ",
                style = MaterialTheme.typography.headlineSmall.scaled(scale, 1.9f).copy(fontFamily = arabicFont),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    TextSizeSlider(
        label = stringResource(R.string.translation_text_size),
        percent = settings.translationTextPercent,
        onChange = { scope.launch { vm.settingsRepository.setTranslationTextPercent(it) } },
    ) { scale ->
        Text(stringResource(R.string.translation_text_sample), style = MaterialTheme.typography.bodyLarge.scaled(scale, 1.5f))
    }
}

@Composable
private fun TextSizeSlider(label: String, percent: Int, onChange: (Int) -> Unit, preview: @Composable (Float) -> Unit) {
    // Nilai sementara selama digeser (pratinjau langsung); disimpan saat jari dilepas.
    var value by remember(percent) { androidx.compose.runtime.mutableFloatStateOf(percent.toFloat()) }
    Labeled("$label · ${value.toInt()}%") {
        androidx.compose.material3.Slider(
            value = value,
            onValueChange = { value = it },
            onValueChangeFinished = { onChange(value.toInt()) },
            valueRange = 80f..160f,
            steps = 7,
        )
        preview(value / 100f)
    }
}

/** Tema, palet warna, dan kontras. Dipakai di Pengaturan dan onboarding. */
@Composable
internal fun AppearanceControls(vm: AppViewModel, settings: AppSettings) {
    val repo = vm.settingsRepository
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val dynamicPreview = if (dynamicSupported) (if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)) else null
    val originalPreview = originalColorScheme(dark, settings.contrast)
    Column {
        ThemeModeRow(vm, settings)

        Labeled(stringResource(R.string.color_palette)) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PaletteCard(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    title = stringResource(R.string.palette_dynamic),
                    icon = AppIcons.AutoAwesome,
                    selected = settings.colorMode == ColorMode.Dynamic && dynamicSupported,
                    enabled = dynamicSupported,
                    scheme = dynamicPreview,
                    caption = if (dynamicSupported) stringResource(R.string.from_wallpaper) else stringResource(R.string.needs_android12),
                    onClick = { scope.launch { repo.setColorMode(ColorMode.Dynamic) } },
                )
                PaletteCard(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    title = stringResource(R.string.palette_original),
                    icon = AppIcons.Palette,
                    selected = settings.colorMode == ColorMode.Original || !dynamicSupported,
                    enabled = true,
                    scheme = originalPreview,
                    caption = stringResource(R.string.classic_teal),
                    onClick = { scope.launch { repo.setColorMode(ColorMode.Original) } },
                )
            }
        }

        Labeled(stringResource(R.string.contrast)) {
            val options = listOf(ContrastLevel.Standard to stringResource(R.string.contrast_standard), ContrastLevel.Medium to stringResource(R.string.contrast_medium), ContrastLevel.High to stringResource(R.string.contrast_high))
            val usesDynamic = settings.colorMode == ColorMode.Dynamic && dynamicSupported
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                options.forEachIndexed { i, (level, label) ->
                    SegmentedButton(
                        selected = settings.contrast == level,
                        onClick = { scope.launch { repo.setContrast(level) } },
                        enabled = !usesDynamic,
                        shape = SegmentedButtonDefaults.itemShape(i, options.size),
                    ) { Text(label) }
                }
            }
            if (usesDynamic) {
                Text(
                    stringResource(R.string.contrast_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/** Pilihan tema: ikuti sistem, terang, gelap. Juga satu-satunya pengaturan tampilan di varian lite. */
@Composable
internal fun ThemeModeRow(vm: AppViewModel, settings: AppSettings) {
    val scope = rememberCoroutineScope()
    Labeled(stringResource(R.string.theme)) {
        val options = listOf(
            Triple(ThemeMode.System, stringResource(R.string.theme_system), AppIcons.BrightnessAuto),
            Triple(ThemeMode.Light, stringResource(R.string.theme_light), AppIcons.LightMode),
            Triple(ThemeMode.Dark, stringResource(R.string.theme_dark), AppIcons.DarkMode),
        )
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, (mode, label, icon) ->
                SegmentedButton(
                    selected = settings.themeMode == mode,
                    onClick = { scope.launch { vm.settingsRepository.setThemeMode(mode) } },
                    shape = SegmentedButtonDefaults.itemShape(i, options.size),
                    icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(SegmentedButtonDefaults.IconSize)) },
                ) { Text(label) }
            }
        }
    }
}

/** Sakelar warna tajwid sebagai satu baris berkelompok. */
@Composable
internal fun TajweedToggle(vm: AppViewModel, settings: AppSettings) {
    val scope = rememberCoroutineScope()
    Group {
        item(
            title = stringResource(R.string.tajweed_title),
            subtitle = stringResource(R.string.tajweed_sub),
            onClick = { scope.launch { vm.settingsRepository.setTajweed(!settings.tajweed) } },
            leading = { Icon(AppIcons.FormatColorText, contentDescription = null) },
            trailing = { IconSwitch(settings.tajweed) },
        )
    }
}
