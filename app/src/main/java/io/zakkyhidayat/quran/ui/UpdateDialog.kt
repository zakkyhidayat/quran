package io.zakkyhidayat.quran.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.zakkyhidayat.quran.AppViewModel
import io.zakkyhidayat.quran.R
import io.zakkyhidayat.quran.data.Updater
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.Locale

/** Tawaran pembaruan dari GitHub Releases: unduh APK dengan progres, lalu buka pemasang sistem. */
@Composable
fun UpdateDialog(vm: AppViewModel) {
    val info by vm.update.collectAsStateWithLifecycle()
    val current = info ?: return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var progress by remember(current) { mutableFloatStateOf(-1f) }
    var failed by remember(current) { mutableStateOf(false) }
    val downloading = progress >= 0f

    AlertDialog(
        onDismissRequest = { if (!downloading) vm.dismissUpdate(skip = false) },
        title = { Text(stringResource(R.string.update_title, current.version)) },
        text = {
            Column {
                if (current.bytes > 0) {
                    Text(
                        stringResource(R.string.translation_size_mb, String.format(Locale.getDefault(), "%.1f", current.bytes / 1_000_000.0)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                if (current.notes.isNotBlank()) {
                    Text(
                        current.notes.trim(),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState()),
                    )
                }
                if (downloading) {
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                }
                if (failed) {
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.update_failed), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !downloading,
                onClick = {
                    if (!Updater.canInstall(context)) {
                        // Izin "pasang aplikasi tak dikenal" diberikan sekali di Pengaturan Android.
                        Updater.openInstallPermissionSettings(context)
                        return@TextButton
                    }
                    scope.launch {
                        failed = false
                        progress = 0f
                        try {
                            val apk = Updater.download(context, current) { progress = it }
                            Updater.install(context, apk)
                            vm.dismissUpdate(skip = false)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            android.util.Log.w("Updater", "Unduhan pembaruan gagal: $e")
                            failed = true
                        } finally {
                            progress = -1f
                        }
                    }
                },
            ) { Text(stringResource(R.string.update_install)) }
        },
        dismissButton = {
            Row {
                TextButton(enabled = !downloading, onClick = { vm.dismissUpdate(skip = true) }) { Text(stringResource(R.string.update_skip)) }
                TextButton(enabled = !downloading, onClick = { vm.dismissUpdate(skip = false) }) { Text(stringResource(R.string.update_later)) }
            }
        },
    )
}
