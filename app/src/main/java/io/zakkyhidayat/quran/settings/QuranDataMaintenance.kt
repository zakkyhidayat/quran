package io.zakkyhidayat.quran.settings

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.zakkyhidayat.quran.R
import io.zakkyhidayat.quran.data.QuranDatabaseInstaller
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun QuranDataMaintenance(installer: QuranDatabaseInstaller) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val progress by installer.progress.collectAsState()
    var downloadSize by remember { mutableStateOf<Long?>(null) }
    var confirmation by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var complete by remember { mutableStateOf(false) }

    LaunchedEffect(installer) {
        try {
            downloadSize = installer.downloadSize()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.w("QuranDatabase", "Gagal memuat ukuran data terbaru", e)
            failed = true
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.quran_data_maintenance_body))
        downloadSize?.let {
            Text(
                stringResource(R.string.onb_download_size, Formatter.formatShortFileSize(context, it)),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
            )
        }
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
            }
        }
        if (failed) {
            Text(stringResource(R.string.onb_download_error), color = MaterialTheme.colorScheme.error)
        }
        if (complete) {
            Text(stringResource(R.string.quran_data_refresh_success), color = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.quran_data_restart_required), style = MaterialTheme.typography.bodySmall)
        }
        Button(
            onClick = { confirmation = true },
            enabled = !downloading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(if (failed) R.string.retry else R.string.quran_data_refresh))
        }
    }

    if (confirmation) {
        AlertDialog(
            onDismissRequest = { confirmation = false },
            title = { Text(stringResource(R.string.quran_data_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.quran_data_confirm_body,
                        downloadSize?.let { Formatter.formatShortFileSize(context, it) } ?: "—",
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
                                complete = true
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
