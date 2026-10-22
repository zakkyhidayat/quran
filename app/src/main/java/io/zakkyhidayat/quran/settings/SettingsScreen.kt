package io.zakkyhidayat.quran.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.zakkyhidayat.quran.AppViewModel
import io.zakkyhidayat.quran.R
import io.zakkyhidayat.quran.data.Backup
import io.zakkyhidayat.quran.data.QuranDatabaseInstaller
import io.zakkyhidayat.quran.ui.AppIcons
import io.zakkyhidayat.quran.ui.CenteredContent
import kotlinx.coroutines.launch

/** Halaman Pengaturan: utama (daftar kategori) dan satu halaman per kategori. */
enum class SettingsPage(val route: String) {
    Main("settings"),
    Appearance("settings/appearance"),
    Translations("settings/translations"),
    Backup("settings/backup"),
    Reminder("settings/reminder"),
    QuranData("settings/quran-data"),
    About("settings/about"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsMainSheet(
    settings: AppSettings,
    onDismiss: () -> Unit,
    onOpen: (SettingsPage) -> Unit,
) {
    // Dialog melayang di tengah dengan margin 16dp, bukan sheet dari bawah.
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        androidx.compose.material3.Surface(
            shape = RoundedCornerShape(28.dp),
            color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier.padding(16.dp).widthIn(max = 560.dp).fillMaxWidth(),
        ) {
        Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).padding(top = 12.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.settings), style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                }
            }
            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
            ) {
                SettingsMain(settings, onOpen)
            }
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    vm: AppViewModel,
    settings: AppSettings,
    page: SettingsPage = SettingsPage.Main,
    onBack: () -> Unit,
    onOpen: (SettingsPage) -> Unit = {},
) {
    val repo = vm.settingsRepository
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val quranDataInstaller = remember(context) { QuranDatabaseInstaller(context) }
    val version = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    }
    val snackbar = remember { SnackbarHostState() }
    val title = stringResource(
        when (page) {
            SettingsPage.Main -> R.string.settings
            SettingsPage.Appearance -> R.string.appearance
            SettingsPage.Translations -> R.string.translations
            SettingsPage.Backup -> R.string.backup
            SettingsPage.Reminder -> R.string.reminder
            SettingsPage.QuranData -> R.string.quran_data_title
            SettingsPage.About -> R.string.about
        },
    )
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) } },
            )
        },
    ) { padding ->
        CenteredContent(Modifier.padding(padding)) {
            if (page == SettingsPage.Translations) {
                Column(
                    Modifier.fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp, bottom = 24.dp),
                ) {
                    Group {
                        item(
                            title = stringResource(R.string.translit_title),
                            subtitle = stringResource(R.string.translit_sub),
                            onClick = { scope.launch { repo.setShowTransliteration(!settings.showTransliteration) } },
                            leading = { Icon(AppIcons.Translate, contentDescription = null) },
                            trailing = { IconSwitch(settings.showTransliteration) },
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    TranslationControls(vm, settings, Modifier.weight(1f), fillAvailableHeight = true)
                }
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)) {
                    item(key = page.route) {
                        Column {
                            when (page) {
                            SettingsPage.Main -> SettingsMain(settings, onOpen)
                            SettingsPage.Appearance -> {

                                AppearanceControls(vm, settings)
                                TextSizeControls(vm, settings, showTranslation = true)

                                Spacer(Modifier.height(8.dp))
                                Group {
                                    item(
                                        title = stringResource(R.string.amoled),
                                        subtitle = stringResource(R.string.amoled_sub),
                                        onClick = { scope.launch { repo.setAmoled(!settings.amoled) } },
                                        leading = { Icon(AppIcons.DarkMode, contentDescription = null) },
                                        trailing = { IconSwitch(settings.amoled) },
                                    )
                                    item(
                                        title = stringResource(R.string.tajweed_title),
                                        subtitle = stringResource(R.string.tajweed_sub),
                                        onClick = { scope.launch { repo.setTajweed(!settings.tajweed) } },
                                        leading = { Icon(AppIcons.FormatColorText, contentDescription = null) },
                                        trailing = { IconSwitch(settings.tajweed) },
                                    )
                                    item(
                                        title = stringResource(R.string.keep_screen_on_title),
                                        subtitle = stringResource(R.string.keep_screen_on_sub),
                                        onClick = { scope.launch { repo.setKeepScreenOn(!settings.keepScreenOn) } },
                                        leading = { Icon(AppIcons.LightMode, contentDescription = null) },
                                        trailing = { IconSwitch(settings.keepScreenOn) },
                                    )
                                }

                            }
                            SettingsPage.Translations -> Unit
                            SettingsPage.Backup -> BackupControls(vm, settings, snackbar)
                            SettingsPage.Reminder -> ReminderControls(vm, settings)
                            SettingsPage.QuranData -> QuranDataMaintenance(quranDataInstaller)
                            SettingsPage.About -> {
                                Group {
                                    item(title = stringResource(R.string.version), subtitle = version)
                                    item(
                                        title = stringResource(R.string.data_source),
                                        subtitle = stringResource(R.string.data_source_sub),
                                    )
                                    item(
                                        title = stringResource(R.string.font),
                                        subtitle = stringResource(R.string.font_sub),
                                    )
                                    item(
                                        title = stringResource(R.string.credits),
                                        subtitle = stringResource(R.string.credits_sub),
                                        onClick = { openUrl(context, "https://qul.tarteel.ai/credits") },
                                        trailing = { Icon(AppIcons.OpenInNew, contentDescription = null) },
                                    )
                                    item(
                                        title = stringResource(R.string.support),
                                        subtitle = stringResource(R.string.support_sub),
                                        onClick = { openUrl(context, "https://ko-fi.com/zakkyhidayat") },
                                        leading = { Icon(AppIcons.Favorite, contentDescription = null) },
                                        trailing = { Icon(AppIcons.OpenInNew, contentDescription = null) },
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
}

/** Halaman utama: bahasa, lalu kategori dengan ringkasan isinya. */
@Composable
private fun SettingsMain(settings: AppSettings, onOpen: (SettingsPage) -> Unit) {
    LanguageSection(showTitle = false)
    Spacer(Modifier.height(16.dp))
    val theme = stringResource(
        when (settings.themeMode) {
            ThemeMode.System -> R.string.theme_system
            ThemeMode.Light -> R.string.theme_light
            ThemeMode.Dark -> R.string.theme_dark
        },
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SettingsCategory(stringResource(R.string.appearance), stringResource(R.string.settings_appearance_sub, theme), AppIcons.Palette) { onOpen(SettingsPage.Appearance) }
        SettingsCategory(stringResource(R.string.translations), stringResource(R.string.settings_translations_sub, settings.translationIds.size), AppIcons.Translate) { onOpen(SettingsPage.Translations) }
        SettingsCategory(
            stringResource(R.string.backup),
            stringResource(if (settings.autoBackupUri != null) R.string.settings_backup_on else R.string.settings_backup_sub),
            AppIcons.Backup,
        ) { onOpen(SettingsPage.Backup) }
        SettingsCategory(
            stringResource(R.string.reminder),
            if (settings.reminderEnabled) stringResource(R.string.reminder_on_sub, reminderTimeText(LocalContext.current, settings.reminderMinutes))
            else stringResource(R.string.reminder_off_sub),
            AppIcons.Alarm,
        ) { onOpen(SettingsPage.Reminder) }
        SettingsCategory(stringResource(R.string.quran_data_title), stringResource(R.string.quran_data_sub), AppIcons.Download) {
            onOpen(SettingsPage.QuranData)
        }
        SettingsCategory(stringResource(R.string.about), stringResource(R.string.settings_about_sub), Icons.Default.Info) {
            onOpen(SettingsPage.About)
        }
    }
}

@Composable
private fun SettingsCategory(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Group {
        item(
            title = title,
            subtitle = subtitle,
            onClick = onClick,
            leading = { Icon(icon, contentDescription = null) },
            trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
        )
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    runCatching { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))) }
}
