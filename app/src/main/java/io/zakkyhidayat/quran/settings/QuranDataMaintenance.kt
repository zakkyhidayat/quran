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
import io.zakkyhidayat.quran.data.QuranDataCheck
import io.zakkyhidayat.quran.data.QuranDatabaseInstaller
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

    LaunchedEffect(installer) {
        checking = true
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

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.quran_data_maintenance_body))
        if (checking) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        dataCheck?.let { check ->
            Text(
                if (check.current) stringResource(R.string.quran_data_current, check.latestVersion)
                else stringResource(
                    R.string.quran_data_update_available,
                    check.latestVersion,
                    Formatter.formatShortFileSize(context, check.downloadBytes),
                ),
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
        if (failed || checkFailed) {
            Text(stringResource(R.string.onb_download_error), color = MaterialTheme.colorScheme.error)
        }
        if (complete) {
            Text(stringResource(R.string.quran_data_refresh_success), color = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.quran_data_restart_required), style = MaterialTheme.typography.bodySmall)
        }
        Button(
            onClick = {
                if (checkFailed) {
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
                } else {
                    confirmation = true
                }
            },
            enabled = !downloading && !checking && (checkFailed || dataCheck?.current == false),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(
                    when {
                        checkFailed || failed -> R.string.retry
                        dataCheck?.current == true -> R.string.quran_data_current_button
                        else -> R.string.quran_data_refresh
                    },
                ),
            )
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
