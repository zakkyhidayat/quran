package io.zakkyhidayat.quran.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.zakkyhidayat.quran.AppViewModel
import io.zakkyhidayat.quran.ui.CenteredContent
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
                SectionTitle("Tampilan")
                Labeled("Tema") {
                    val options = listOf(ThemeMode.System to "Sistem", ThemeMode.Light to "Terang", ThemeMode.Dark to "Gelap")
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        options.forEachIndexed { i, (mode, label) ->
                            SegmentedButton(
                                selected = settings.themeMode == mode,
                                onClick = { scope.launch { repo.setThemeMode(mode) } },
                                shape = SegmentedButtonDefaults.itemShape(i, options.size),
                            ) { Text(label) }
                        }
                    }
                }
                Labeled("Warna aplikasi") {
                    val options = listOf(ColorMode.Dynamic to "Dinamis", ColorMode.Original to "Asli")
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        options.forEachIndexed { i, (mode, label) ->
                            SegmentedButton(
                                selected = settings.colorMode == mode,
                                onClick = { scope.launch { repo.setColorMode(mode) } },
                                enabled = mode == ColorMode.Original || dynamicSupported,
                                shape = SegmentedButtonDefaults.itemShape(i, options.size),
                            ) { Text(label) }
                        }
                    }
                    if (!dynamicSupported) {
                        Text(
                            "Warna dinamis butuh Android 12 atau lebih baru.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
                Group {
                    item(
                        title = "AMOLED",
                        subtitle = "Latar hitam pekat di mode gelap",
                        onClick = { scope.launch { repo.setAmoled(!settings.amoled) } },
                        trailing = { Switch(checked = settings.amoled, onCheckedChange = null) },
                    )
                    item(
                        title = "Warna tajwid",
                        subtitle = "Tampilkan huruf berwarna sesuai hukum tajwid",
                        onClick = { scope.launch { repo.setTajweed(!settings.tajweed) } },
                        trailing = { Switch(checked = settings.tajweed, onCheckedChange = null) },
                    )
                }

                SectionTitle("Terjemahan")
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

                SectionTitle("Tentang")
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
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 8.dp))
}

@Composable
private fun Labeled(label: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
        content()
    }
}
