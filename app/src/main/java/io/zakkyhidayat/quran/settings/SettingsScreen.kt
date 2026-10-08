package io.zakkyhidayat.quran.settings

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.zakkyhidayat.quran.AppViewModel
import io.zakkyhidayat.quran.ui.AppIcons
import io.zakkyhidayat.quran.ui.CenteredContent
import io.zakkyhidayat.quran.ui.theme.originalColorScheme
import io.zakkyhidayat.quran.ui.segmentedItemColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(vm: AppViewModel, settings: AppSettings, onBack: () -> Unit) {
    val repo = vm.settingsRepository
    val scope = rememberCoroutineScope()
    val translations by vm.translations.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val version = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    }
    val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val dynamicPreview = if (dynamicSupported) (if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)) else null
    val originalPreview = originalColorScheme(dark, settings.contrast)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali") } },
            )
        },
    ) { padding ->
        CenteredContent(Modifier.padding(padding)) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                SectionTitle(AppIcons.Palette, "Tampilan")

                Labeled("Tema") {
                    val options = listOf(
                        Triple(ThemeMode.System, "Sistem", AppIcons.BrightnessAuto),
                        Triple(ThemeMode.Light, "Terang", AppIcons.LightMode),
                        Triple(ThemeMode.Dark, "Gelap", AppIcons.DarkMode),
                    )
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        options.forEachIndexed { i, (mode, label, icon) ->
                            SegmentedButton(
                                selected = settings.themeMode == mode,
                                onClick = { scope.launch { repo.setThemeMode(mode) } },
                                shape = SegmentedButtonDefaults.itemShape(i, options.size),
                                icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(SegmentedButtonDefaults.IconSize)) },
                            ) { Text(label) }
                        }
                    }
                }

                Labeled("Palet warna") {
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PaletteCard(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            title = "Dinamis",
                            icon = AppIcons.AutoAwesome,
                            selected = settings.colorMode == ColorMode.Dynamic && dynamicSupported,
                            enabled = dynamicSupported,
                            scheme = dynamicPreview,
                            caption = if (dynamicSupported) "Dari wallpaper" else "Butuh Android 12+",
                            onClick = { scope.launch { repo.setColorMode(ColorMode.Dynamic) } },
                        )
                        PaletteCard(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            title = "Asli",
                            icon = AppIcons.Palette,
                            selected = settings.colorMode == ColorMode.Original || !dynamicSupported,
                            enabled = true,
                            scheme = originalPreview,
                            caption = "Teal klasik",
                            onClick = { scope.launch { repo.setColorMode(ColorMode.Original) } },
                        )
                    }
                }

                Labeled("Kontras warna", icon = AppIcons.Contrast) {
                    val options = listOf(ContrastLevel.Standard to "Standar", ContrastLevel.Medium to "Sedang", ContrastLevel.High to "Tinggi")
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
                            "Kontras berlaku untuk palet Asli.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Group {
                    item(
                        title = "AMOLED",
                        subtitle = "Latar hitam pekat di mode gelap",
                        onClick = { scope.launch { repo.setAmoled(!settings.amoled) } },
                        leading = { Icon(AppIcons.DarkMode, contentDescription = null) },
                        trailing = { IconSwitch(settings.amoled) },
                    )
                    item(
                        title = "Warna tajwid",
                        subtitle = "Tampilkan huruf berwarna sesuai hukum tajwid",
                        onClick = { scope.launch { repo.setTajweed(!settings.tajweed) } },
                        leading = { Icon(AppIcons.FormatColorText, contentDescription = null) },
                        trailing = { IconSwitch(settings.tajweed) },
                    )
                }

                SectionTitle(AppIcons.Translate, "Terjemahan")
                listOf("id" to "Bahasa Indonesia", "en" to "English").forEach { (lang, title) ->
                    val group = translations.filter { it.lang == lang }
                    if (group.isEmpty()) return@forEach
                    Text(title, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp))
                    Group {
                        group.forEach { tr ->
                            val checked = tr.id in settings.translationIds
                            item(
                                title = tr.name,
                                onClick = { scope.launch { repo.setTranslations(toggled(translations.map { it.id }, settings.translationIds, tr.id)) } },
                                leading = { Checkbox(checked = checked, onCheckedChange = null) },
                            )
                        }
                    }
                }

                SectionTitle(Icons.Default.Info, "Tentang")
                Group {
                    item(title = "Versi", subtitle = version)
                    item(
                        title = "Sumber data",
                        subtitle = "Quranic Universal Library (Tarteel): layout KFGQPC V4, font tajwid, dan terjemahan.",
                    )
                }
            }
        }
    }
}

// Switch M3 dengan ikon centang pada ibu jari saat aktif.
@Composable
private fun IconSwitch(checked: Boolean) {
    Switch(
        checked = checked,
        onCheckedChange = null,
        thumbContent = if (checked) {
            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
        } else {
            null
        },
    )
}

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
                if (selected) Icon(Icons.Default.Check, contentDescription = "Dipilih", modifier = Modifier.size(20.dp))
            }
            if (scheme != null) {
                PaletteSwatches(scheme)
            } else {
                Spacer(Modifier.height(32.dp))
            }
            Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

private class GroupItem(
    val title: String,
    val subtitle: String?,
    val onClick: () -> Unit,
    val leading: (@Composable () -> Unit)?,
    val trailing: (@Composable () -> Unit)?,
)

private class GroupScope {
    val items = mutableListOf<GroupItem>()

    fun item(
        title: String,
        subtitle: String? = null,
        onClick: () -> Unit = {},
        leading: (@Composable () -> Unit)? = null,
        trailing: (@Composable () -> Unit)? = null,
    ) {
        items += GroupItem(title, subtitle, onClick, leading, trailing)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Group(content: GroupScope.() -> Unit) {
    val scope = GroupScope().apply(content)
    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        scope.items.forEachIndexed { index, item ->
            SegmentedListItem(
                onClick = item.onClick,
                shapes = ListItemDefaults.segmentedShapes(index, scope.items.size),
                colors = segmentedItemColors(),
                leadingContent = item.leading,
                trailingContent = item.trailing,
                supportingContent = item.subtitle?.let { sub -> { Text(sub) } },
            ) { Text(item.title) }
        }
    }
}

private fun toggled(order: List<String>, current: List<String>, id: String): List<String> {
    val next = if (id in current) current - id else current + id
    return order.filter { it in next }
}

@Composable
private fun SectionTitle(icon: ImageVector, text: String) {
    Row(Modifier.padding(start = 4.dp, top = 24.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun Labeled(label: String, icon: ImageVector? = null, content: @Composable () -> Unit) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Row(Modifier.padding(start = 4.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
        content()
    }
}
