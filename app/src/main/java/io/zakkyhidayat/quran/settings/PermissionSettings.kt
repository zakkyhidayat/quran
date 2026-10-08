package io.zakkyhidayat.quran.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.zakkyhidayat.quran.R
import io.zakkyhidayat.quran.ui.AppIcons

// Pengingat membaca disembunyikan sementara sampai fiturnya dimatangkan; kodenya tetap ada (reminder/Reminder.kt).
private const val REMINDER_VISIBLE = false

/**
 * Status izin yang dibutuhkan aplikasi: notifikasi (pengingat membaca) dan, di varian GitHub, memasang aplikasi
 * (pembaruan). Diperiksa ulang setiap kali layar kembali aktif, karena izin diubah di Pengaturan Android.
 */
@Composable
internal fun PermissionControls() {
    val context = LocalContext.current
    var notifications by remember { mutableStateOf(io.zakkyhidayat.quran.reminder.Reminder.canNotify(context)) }
    var install by remember { mutableStateOf(io.zakkyhidayat.quran.data.Updater.canInstall(context)) }
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        notifications = io.zakkyhidayat.quran.reminder.Reminder.canNotify(context)
        install = io.zakkyhidayat.quran.data.Updater.canInstall(context)
        onPauseOrDispose { }
    }
    Group {
        if (REMINDER_VISIBLE) item(
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
