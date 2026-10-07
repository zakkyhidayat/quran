package io.zakkyhidayat.quran.settings

import android.os.Build
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
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
                    Text("Warna dinamis butuh Android 12 atau lebih baru.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            ListItem(
                headlineContent = { Text("AMOLED") },
                supportingContent = { Text("Latar hitam pekat di mode gelap") },
                trailingContent = { Switch(checked = settings.amoled, onCheckedChange = { scope.launch { repo.setAmoled(it) } }) },
            )
            ListItem(
                headlineContent = { Text("Warna tajwid") },
                supportingContent = { Text("Tampilkan huruf berwarna sesuai hukum tajwid") },
                trailingContent = { Switch(checked = settings.tajweed, onCheckedChange = { scope.launch { repo.setTajweed(it) } }) },
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionTitle("Terjemahan")
            listOf("id" to "Bahasa Indonesia", "en" to "English").forEach { (lang, title) ->
                Text(title, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                translations.filter { it.lang == lang }.forEach { tr ->
                    val checked = tr.id in settings.translationIds
                    ListItem(
                        modifier = Modifier.clickable { scope.launch { repo.setTranslations(toggled(translations.map { it.id }, settings.translationIds, tr.id)) } },
                        headlineContent = { Text(tr.name) },
                        leadingContent = { Checkbox(checked = checked, onCheckedChange = null) },
                    )
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionTitle("Tentang")
            ListItem(headlineContent = { Text("Versi") }, supportingContent = { Text(version) })
            ListItem(
                headlineContent = { Text("Sumber data") },
                supportingContent = { Text("Quranic Universal Library (Tarteel): layout KFGQPC V4, font tajwid, dan terjemahan.") },
            )
        }
    }
}

private fun toggled(order: List<String>, current: List<String>, id: String): List<String> {
    val next = if (id in current) current - id else current + id
    return order.filter { it in next }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
}

@Composable
private fun Labeled(label: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 6.dp))
        content()
    }
}
