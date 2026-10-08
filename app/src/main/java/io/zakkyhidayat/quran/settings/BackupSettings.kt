package io.zakkyhidayat.quran.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.zakkyhidayat.quran.AppViewModel
import io.zakkyhidayat.quran.R
import io.zakkyhidayat.quran.data.Backup
import io.zakkyhidayat.quran.ui.AppIcons
import kotlinx.coroutines.launch

/** Cadangan lokal: ekspor dan pulihkan bookmark serta pengaturan lewat pemilih berkas sistem. */
@Composable
internal fun BackupControls(vm: AppViewModel, settings: AppSettings, snackbar: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    fun toast(text: String) { scope.launch { snackbar.showSnackbar(text) } }
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
