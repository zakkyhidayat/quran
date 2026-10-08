package io.zakkyhidayat.quran.settings

import androidx.compose.ui.res.stringResource
import io.zakkyhidayat.quran.R
import android.os.Build
import io.zakkyhidayat.quran.data.Backup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.material3.RadioButton
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import io.zakkyhidayat.quran.AppLanguage
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
    val context = LocalContext.current
    val version = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    }
    val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) } },
            )
        },
    ) { padding ->
        CenteredContent(Modifier.padding(padding)) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                LanguageSection()

                SectionTitle(AppIcons.Palette, stringResource(R.string.appearance))

                AppearanceControls(vm, settings)

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

                SectionTitle(AppIcons.Translate, stringResource(R.string.translations))
                TranslationControls(vm, settings)

                SectionTitle(AppIcons.Alarm, stringResource(R.string.reminder))
                ReminderControls(vm, settings)

                SectionTitle(AppIcons.Backup, stringResource(R.string.backup))
                BackupControls(vm, settings)

                SectionTitle(AppIcons.Shield, stringResource(R.string.permissions))
                PermissionControls()

                SectionTitle(Icons.Default.Info, stringResource(R.string.about))
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
                                        false -> android.widget.Toast.makeText(context, latest, android.widget.Toast.LENGTH_SHORT).show()
                                        null -> android.widget.Toast.makeText(context, failedCheck, android.widget.Toast.LENGTH_LONG).show()
                                        true -> Unit // dialog pembaruan tampil
                                    }
                                }
                            },
                            trailing = { Icon(AppIcons.Download, contentDescription = null) },
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
                if (selected) Icon(Icons.Default.Check, contentDescription = stringResource(R.string.selected), modifier = Modifier.size(20.dp))
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
private fun Group(content: @Composable GroupScope.() -> Unit) {
    val scope = GroupScope().apply { content() }
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

@Composable
private fun SectionTitle(icon: ImageVector, text: String) {
    Row(Modifier.padding(start = 4.dp, top = 24.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun Labeled(label: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
        content()
    }
}

@Composable
internal fun translationLabel(tr: io.zakkyhidayat.quran.data.TranslationInfo): String {
    val language = when (tr.lang) {
        "id" -> stringResource(R.string.lang_name_id)
        "en" -> stringResource(R.string.lang_name_en)
        "ur" -> stringResource(R.string.lang_name_ur)
        "bn" -> stringResource(R.string.lang_name_bn)
        "tr" -> stringResource(R.string.lang_name_tr)
        "fa" -> stringResource(R.string.lang_name_fa)
        "ms" -> stringResource(R.string.lang_name_ms)
        "fr" -> stringResource(R.string.lang_name_fr)
        "ru" -> stringResource(R.string.lang_name_ru)
        "ar" -> stringResource(R.string.lang_name_ar)
        else -> tr.lang
    }
    return stringResource(R.string.translation_label, language, tr.name)
}

private fun openUrl(context: android.content.Context, url: String) {
    runCatching { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))) }
}

@Composable
internal fun LanguageSection(showTitle: Boolean = true) {
    val context = LocalContext.current
    var code by remember { mutableStateOf(AppLanguage.saved(context)) }
    var open by rememberSaveable { mutableStateOf(false) }
    // Nama bahasa ditulis dalam bahasanya sendiri agar mudah ditemukan apa pun bahasa yang sedang aktif.
    val systemLabel = stringResource(R.string.language_system)
    val options = listOf("" to systemLabel) + AppLanguage.supported.map { it to AppLanguage.endonyms.getValue(it) }
    if (showTitle) SectionTitle(AppIcons.Translate, stringResource(R.string.language))
    Group {
        item(
            title = options.first { it.first == code }.second,
            onClick = { open = true },
            leading = { Icon(AppIcons.Translate, contentDescription = null) },
        )
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(stringResource(R.string.language)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()).selectableGroup()) {
                    options.forEach { (value, label) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .selectable(selected = code == value, role = Role.RadioButton) {
                                    open = false
                                    if (code != value) {
                                        code = value
                                        AppLanguage.apply(context, value)
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = code == value, onClick = null)
                            Spacer(Modifier.width(16.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.close)) } },
        )
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
                        onClick = { scope.launch { repo.setThemeMode(mode) } },
                        shape = SegmentedButtonDefaults.itemShape(i, options.size),
                        icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(SegmentedButtonDefaults.IconSize)) },
                    ) { Text(label) }
                }
            }
        }

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

/** Daftar terjemahan aktif (dengan tombol hapus) dan tombol tambah. Dipakai di Pengaturan dan onboarding. */
@Composable
internal fun TranslationControls(vm: AppViewModel, settings: AppSettings) {
    val repo = vm.settingsRepository
    val scope = rememberCoroutineScope()
    val translations by vm.translations.collectAsStateWithLifecycle()
    var showAddTranslation by rememberSaveable { mutableStateOf(false) }
    Column {
        val ordered = translations.map { it.id }
        val active = translations.filter { it.id in settings.translationIds }
        val inactive = translations.filter { it.id !in settings.translationIds }
        Group {
            active.forEach { tr ->
                item(
                    title = translationLabel(tr),
                    trailing = {
                        IconButton(
                            onClick = { scope.launch { repo.setTranslations(ordered.filter { it in settings.translationIds && it != tr.id }) } },
                        ) { Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_translation_cd, translationLabel(tr))) }
                    },
                )
            }
        }
        FilledTonalButton(onClick = { showAddTranslation = true }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text("  " + stringResource(R.string.add_translation))
        }
        if (showAddTranslation) {
            AddTranslationDialog(
                vm = vm,
                inactive = inactive,
                onActivate = { id ->
                    // Urutan mengikuti daftar terjemahan terbaru (paket yang baru diunduh sudah masuk).
                    val all = vm.translations.value.map { it.id }
                    scope.launch { repo.setTranslations(all.filter { it in settings.translationIds || it == id }) }
                    showAddTranslation = false
                },
                onDismiss = { showAddTranslation = false },
            )
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

/** Cadangan lokal: ekspor dan pulihkan bookmark serta pengaturan lewat pemilih berkas sistem. */
@Composable
private fun BackupControls(vm: AppViewModel, settings: AppSettings) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    fun toast(text: String) = android.widget.Toast.makeText(context, text, android.widget.Toast.LENGTH_LONG).show()
    val exportDone = stringResource(R.string.backup_export_done)
    val importDone = stringResource(R.string.backup_import_done)
    val failed = stringResource(R.string.backup_failed)
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            runCatching { Backup.export(context, uri, vm.settingsRepository, vm.bookmarkStore) }
                .onSuccess { toast(exportDone) }
                .onFailure { toast(failed.format(it.message.orEmpty())) }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching { Backup.import(context, uri, vm.settingsRepository, vm.bookmarkStore) }
                .onSuccess {
                    toast(importDone.format(it.bookmarksAdded))
                    // Terjemahan aktif dari cadangan diunduh ulang bila belum terpasang.
                    vm.restoreActiveTranslations()
                }
                .onFailure { toast(failed.format(it.message.orEmpty())) }
        }
    }
    // Cadangan otomatis: pilih berkas sekali, izin aksesnya disimpan permanen.
    val autoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
            vm.settingsRepository.setAutoBackup(uri.toString())
        }
    }
    val autoUri = settings.autoBackupUri
    val autoSubtitle = when {
        autoUri == null -> stringResource(R.string.auto_backup_off_sub)
        settings.autoBackupAt < 0 -> stringResource(R.string.auto_backup_error)
        else -> stringResource(
            R.string.auto_backup_on_sub,
            remember(autoUri) { displayName(context, autoUri) },
            if (settings.autoBackupAt == 0L) stringResource(R.string.auto_backup_pending)
            // Jam bila hari ini, tanggal bila lebih lama.
            else android.text.format.DateUtils.formatSameDayTime(
                settings.autoBackupAt, System.currentTimeMillis(), java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT,
            ).toString(),
        )
    }
    Group {
        item(
            title = stringResource(R.string.auto_backup),
            subtitle = autoSubtitle,
            onClick = {
                if (autoUri == null) {
                    autoLauncher.launch("quran-autobackup.json")
                } else {
                    runCatching {
                        context.contentResolver.releasePersistableUriPermission(
                            android.net.Uri.parse(autoUri),
                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                        )
                    }
                    scope.launch { vm.settingsRepository.setAutoBackup(null) }
                }
            },
            leading = { Icon(AppIcons.Backup, contentDescription = null) },
            trailing = { IconSwitch(autoUri != null) },
        )
        item(
            title = stringResource(R.string.backup_export),
            subtitle = stringResource(R.string.backup_export_sub),
            onClick = {
                val date = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.ROOT).format(java.util.Date())
                exportLauncher.launch("quran-backup-$date.json")
            },
            leading = { Icon(AppIcons.Upload, contentDescription = null) },
        )
        item(
            title = stringResource(R.string.backup_import),
            subtitle = stringResource(R.string.backup_import_sub),
            onClick = { importLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) },
            leading = { Icon(AppIcons.Download, contentDescription = null) },
        )
    }
}

// Nama berkas yang ditampilkan untuk URI SAF; cadangan ke ujung URI bila penyedia tidak memberi nama.
private fun displayName(context: android.content.Context, uri: String): String = runCatching {
    context.contentResolver.query(android.net.Uri.parse(uri), arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { if (it.moveToFirst()) it.getString(0) else null }
}.getOrNull() ?: uri.substringAfterLast('/')

/** Pengingat membaca harian: sakelar (meminta izin notifikasi di Android 13+), jam, dan satuan bagian. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderControls(vm: AppViewModel, settings: AppSettings) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = vm.settingsRepository
    var showTime by rememberSaveable { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) scope.launch { repo.setReminder(true) }
    }
    val timeText = remember(settings.reminderMinutes) {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, settings.reminderMinutes / 60)
            set(java.util.Calendar.MINUTE, settings.reminderMinutes % 60)
        }
        android.text.format.DateFormat.getTimeFormat(context).format(cal.time)
    }
    Group {
        item(
            title = stringResource(R.string.reminder_daily),
            subtitle = if (settings.reminderEnabled) stringResource(R.string.reminder_on_sub, timeText) else stringResource(R.string.reminder_off_sub),
            onClick = {
                when {
                    settings.reminderEnabled -> scope.launch { repo.setReminder(false) }
                    io.zakkyhidayat.quran.reminder.Reminder.canNotify(context) -> scope.launch { repo.setReminder(true) }
                    Build.VERSION.SDK_INT >= 33 -> permission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            leading = { Icon(AppIcons.Alarm, contentDescription = null) },
            trailing = { IconSwitch(settings.reminderEnabled) },
        )
        // Build debug saja: tampilkan pengingat sekarang untuk menguji isi dan ketukannya.
        if (settings.reminderEnabled && io.zakkyhidayat.quran.BuildConfig.DEBUG) {
            item(
                title = "Kirim pengingat uji",
                onClick = { scope.launch { io.zakkyhidayat.quran.reminder.Reminder.notify(context) } },
            )
        }
        if (settings.reminderEnabled) {
            item(
                title = stringResource(R.string.reminder_time),
                subtitle = timeText,
                onClick = { showTime = true },
                leading = { Icon(AppIcons.Schedule, contentDescription = null) },
            )
        }
    }
    if (settings.reminderEnabled) {
        Labeled(stringResource(R.string.reminder_unit)) {
            val options = listOf(
                ReminderUnit.Juz to stringResource(R.string.tab_juz),
                ReminderUnit.Hizb to stringResource(R.string.tab_hizb),
                ReminderUnit.Manzil to stringResource(R.string.tab_manzil),
            )
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                options.forEachIndexed { i, (unit, label) ->
                    SegmentedButton(
                        selected = settings.reminderUnit == unit,
                        onClick = { scope.launch { repo.setReminderUnit(unit) } },
                        shape = SegmentedButtonDefaults.itemShape(i, options.size),
                    ) { Text(label) }
                }
            }
        }
    }
    if (showTime) {
        val state = androidx.compose.material3.rememberTimePickerState(
            initialHour = settings.reminderMinutes / 60,
            initialMinute = settings.reminderMinutes % 60,
            is24Hour = android.text.format.DateFormat.is24HourFormat(context),
        )
        AlertDialog(
            onDismissRequest = { showTime = false },
            title = { Text(stringResource(R.string.reminder_time)) },
            text = { androidx.compose.material3.TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repo.setReminderMinutes(state.hour * 60 + state.minute) }
                    showTime = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

/**
 * Status izin yang dibutuhkan aplikasi: notifikasi (pengingat membaca) dan, di varian GitHub, memasang aplikasi
 * (pembaruan). Diperiksa ulang setiap kali layar kembali aktif, karena izin diubah di Pengaturan Android.
 */
@Composable
private fun PermissionControls() {
    val context = LocalContext.current
    var notifications by remember { mutableStateOf(io.zakkyhidayat.quran.reminder.Reminder.canNotify(context)) }
    var install by remember { mutableStateOf(io.zakkyhidayat.quran.data.Updater.canInstall(context)) }
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        notifications = io.zakkyhidayat.quran.reminder.Reminder.canNotify(context)
        install = io.zakkyhidayat.quran.data.Updater.canInstall(context)
        onPauseOrDispose { }
    }
    Group {
        item(
            title = stringResource(R.string.perm_notifications),
            subtitle = stringResource(R.string.perm_notifications_sub),
            onClick = {
                context.startActivity(
                    android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName),
                )
            },
            leading = { Icon(AppIcons.Alarm, contentDescription = null) },
            trailing = { PermissionStatus(notifications) },
        )
        if (io.zakkyhidayat.quran.BuildConfig.UPDATER_ENABLED) {
            item(
                title = stringResource(R.string.perm_install),
                subtitle = stringResource(R.string.perm_install_sub),
                onClick = { io.zakkyhidayat.quran.data.Updater.openInstallPermissionSettings(context) },
                leading = { Icon(AppIcons.Download, contentDescription = null) },
                trailing = { PermissionStatus(install) },
            )
        }
    }
}

@Composable
private fun PermissionStatus(granted: Boolean) {
    val color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (granted) Icons.Default.Check else AppIcons.Warning,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            stringResource(if (granted) R.string.perm_granted else R.string.perm_denied),
            style = MaterialTheme.typography.labelLarge,
            color = color,
        )
    }
}
