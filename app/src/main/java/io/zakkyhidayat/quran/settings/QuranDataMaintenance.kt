package io.zakkyhidayat.quran.settings

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.zakkyhidayat.quran.R
import io.zakkyhidayat.quran.data.QuranDataCheck
import io.zakkyhidayat.quran.data.QuranDataPart
import io.zakkyhidayat.quran.data.QuranDatabaseDownloadProgress
import io.zakkyhidayat.quran.data.QuranDatabaseInstaller
import io.zakkyhidayat.quran.ui.AppIcons
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun QuranDataMaintenance(installer: QuranDatabaseInstaller) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val progress by installer.progress.collectAsState()
    var dataCheck by remember { mutableStateOf<QuranDataCheck?>(null) }
    var checking by remember { mutableStateOf(true) }
    var checkFailed by remember { mutableStateOf(false) }
    var confirmation by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var complete by remember { mutableStateOf(false) }

    fun recheck() {
        scope.launch {
            checking = true
            checkFailed = false
            try {
                dataCheck = installer.checkForUpdate()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.w("QuranDatabase", "Gagal memeriksa versi data terbaru", e)
                checkFailed = true
            } finally {
                checking = false
            }
        }
    }
    LaunchedEffect(installer) { recheck() }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Status lebih dulu dan paling besar; penjelasan di bawahnya.
        DataStatusCard(checking, dataCheck, error = failed || checkFailed)
        if (downloading) {
            val current = progress
            if (current == null || current.total <= 0) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            } else {
                LinearProgressIndicator(
                    progress = { (current.downloaded.toFloat() / current.total).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(
                        R.string.onb_downloading,
                        Formatter.formatShortFileSize(context, current.downloaded),
                        Formatter.formatShortFileSize(context, current.total),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(quranPartLabel(current), style = MaterialTheme.typography.bodySmall)
            }
        }
        if (complete) {
            Text(stringResource(R.string.quran_data_refresh_success), color = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.quran_data_restart_required), style = MaterialTheme.typography.bodySmall)
        }
        if (dataCheck?.current == true && !checkFailed && !failed) {
            // Data sudah terbaru: tombol tetap aktif untuk memeriksa lagi, bukan label nonaktif.
            OutlinedButton(onClick = { recheck() }, enabled = !checking && !downloading, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.quran_data_check_again))
            }
        } else {
            Button(
                onClick = { if (checkFailed) recheck() else confirmation = true },
                enabled = !downloading && !checking && (checkFailed || dataCheck?.current == false),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(if (checkFailed || failed) R.string.retry else R.string.quran_data_refresh))
            }
        }
        Text(
            stringResource(R.string.quran_data_maintenance_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (confirmation) {
        AlertDialog(
            onDismissRequest = { confirmation = false },
            title = { Text(stringResource(R.string.quran_data_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.quran_data_confirm_body,
                        dataCheck?.downloadBytes?.let { Formatter.formatShortFileSize(context, it) } ?: "—",
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmation = false
                        scope.launch {
                            downloading = true
                            failed = false
                            complete = false
                            try {
                                installer.install()
                                dataCheck = installer.checkForUpdate()
                                complete = dataCheck?.current == true
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                android.util.Log.e("QuranDatabase", "Gagal memperbarui data mushaf", e)
                                failed = true
                            } finally {
                                downloading = false
                            }
                        }
                    },
                ) {
                    Text(stringResource(R.string.quran_data_refresh_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

/** "Database Qur’an, berkas 1 dari 2" untuk progres unduhan yang sedang berjalan. */
@Composable
internal fun quranPartLabel(p: QuranDatabaseDownloadProgress): String {
    val name = stringResource(if (p.part == QuranDataPart.Database) R.string.download_part_database else R.string.download_part_fonts)
    return stringResource(R.string.download_part_progress, name, p.index, p.count)
}

private class DataStatus(val container: Color, val content: Color, val icon: ImageVector, val text: String)

/** Kartu status data: sedang memeriksa, terbaru, ada pembaruan, atau gagal. */
@Composable
private fun DataStatusCard(checking: Boolean, check: QuranDataCheck?, error: Boolean) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val status = when {
        error -> DataStatus(scheme.errorContainer, scheme.onErrorContainer, AppIcons.Warning, stringResource(R.string.onb_download_error))
        check == null -> null
        check.current -> DataStatus(
            scheme.primaryContainer, scheme.onPrimaryContainer, AppIcons.CheckCircle,
            stringResource(R.string.quran_data_current, check.latestVersion),
        )
        else -> DataStatus(
            scheme.secondaryContainer, scheme.onSecondaryContainer, AppIcons.Download,
            stringResource(R.string.quran_data_update_available, check.latestVersion, Formatter.formatShortFileSize(context, check.downloadBytes)),
        )
    }
    if (status == null) {
        if (checking) {
            Surface(shape = MaterialTheme.shapes.extraLarge, color = scheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(24.dp))
            }
        }
        return
    }
    Surface(shape = MaterialTheme.shapes.extraLarge, color = status.container, contentColor = status.content, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(status.icon, contentDescription = null, modifier = Modifier.size(28.dp))
            Text(status.text, style = MaterialTheme.typography.titleMedium)
        }
    }
}
