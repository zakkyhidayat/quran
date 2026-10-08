package io.zakkyhidayat.quran.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.zakkyhidayat.quran.AppViewModel
import io.zakkyhidayat.quran.R
import io.zakkyhidayat.quran.data.Backup
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
    Permissions("settings/permissions"),
    About("settings/about"),
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
            SettingsPage.Permissions -> R.string.permissions
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
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)) {
                item(key = page.route) {
                    Column {
                        when (page) {
                            SettingsPage.Main -> SettingsMain(settings, onOpen)
                            SettingsPage.Appearance -> {

                                AppearanceControls(vm, settings)
                                                TextSizeControls(vm, settings)

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
                                        title = stringResource(R.string.translit_title),
                                        subtitle = stringResource(R.string.translit_sub),
                                        onClick = { scope.launch { repo.setShowTransliteration(!settings.showTransliteration) } },
                                        leading = { Icon(AppIcons.Translate, contentDescription = null) },
                                        trailing = { IconSwitch(settings.showTransliteration) },
                                    )
                                    item(
                                        title = stringResource(R.string.tajweed_title),
                                        subtitle = stringResource(R.string.tajweed_sub),
                                        onClick = { scope.launch { repo.setTajweed(!settings.tajweed) } },
                                        leading = { Icon(AppIcons.FormatColorText, contentDescription = null) },
                                        trailing = { IconSwitch(settings.tajweed) },
                                    )
                                }

                            }
                            SettingsPage.Translations -> TranslationControls(vm, settings)
                            SettingsPage.Backup -> BackupControls(vm, settings, snackbar)
                            SettingsPage.Reminder -> ReminderControls(vm, settings)
                            SettingsPage.Permissions -> PermissionControls()
                            SettingsPage.About -> {
                                Group {
                                    if (io.zakkyhidayat.quran.BuildConfig.UPDATER_ENABLED) {
                                        val latest = stringResource(R.string.update_latest)
                                        val failedCheck = stringResource(R.string.update_check_failed)
                                        item(
                                            title = stringResource(R.string.version),
                                            subtitle = stringResource(R.string.update_check_sub, version),
                                            onClick = {
                                                vm.checkForUpdate(manual = true) { found ->
                                                    when (found) {
                                                        false -> scope.launch { snackbar.showSnackbar(latest) }
                                                        null -> scope.launch { snackbar.showSnackbar(failedCheck) }
                                                        true -> Unit // dialog pembaruan tampil
                                                    }
                                                }
                                            },
                                            trailing = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                        )
                                    } else {
                                        item(title = stringResource(R.string.version), subtitle = version)
                                    }
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
    Group {
        item(
            title = stringResource(R.string.appearance),
            subtitle = stringResource(R.string.settings_appearance_sub, theme),
            onClick = { onOpen(SettingsPage.Appearance) },
            leading = { Icon(AppIcons.Palette, contentDescription = null) },
            trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
        )
        item(
            title = stringResource(R.string.translations),
            subtitle = stringResource(R.string.settings_translations_sub, settings.translationIds.size),
            onClick = { onOpen(SettingsPage.Translations) },
            leading = { Icon(AppIcons.Translate, contentDescription = null) },
            trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
        )
        item(
            title = stringResource(R.string.backup),
            subtitle = stringResource(if (settings.autoBackupUri != null) R.string.settings_backup_on else R.string.settings_backup_sub),
            onClick = { onOpen(SettingsPage.Backup) },
            leading = { Icon(AppIcons.Backup, contentDescription = null) },
            trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
        )
        item(
            title = stringResource(R.string.reminder),
            subtitle = if (settings.reminderEnabled) {
                stringResource(R.string.reminder_on_sub, reminderTimeText(LocalContext.current, settings.reminderMinutes))
            } else {
                stringResource(R.string.reminder_off_sub)
            },
            onClick = { onOpen(SettingsPage.Reminder) },
            leading = { Icon(AppIcons.Alarm, contentDescription = null) },
            trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
        )
        item(
            title = stringResource(R.string.permissions),
            subtitle = stringResource(R.string.settings_permissions_sub),
            onClick = { onOpen(SettingsPage.Permissions) },
            leading = { Icon(AppIcons.Shield, contentDescription = null) },
            trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
        )
        item(
            title = stringResource(R.string.about),
            subtitle = stringResource(R.string.settings_about_sub),
            onClick = { onOpen(SettingsPage.About) },
            leading = { Icon(Icons.Default.Info, contentDescription = null) },
            trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
        )
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    runCatching { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))) }
}
