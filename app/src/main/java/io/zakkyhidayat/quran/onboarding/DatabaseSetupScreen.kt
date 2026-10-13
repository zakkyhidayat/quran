package io.zakkyhidayat.quran.onboarding

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.zakkyhidayat.quran.R
import io.zakkyhidayat.quran.data.QuranDataCheck
import io.zakkyhidayat.quran.data.QuranDatabaseInstaller
import io.zakkyhidayat.quran.settings.LanguageSection
import io.zakkyhidayat.quran.ui.CenteredContent
import io.zakkyhidayat.quran.ui.AppIcons
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun DatabaseSetupScreen(installer: QuranDatabaseInstaller, onInstalled: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val progress by installer.progress.collectAsStateWithLifecycle()
    var dataCheck by remember { mutableStateOf<QuranDataCheck?>(null) }
    var loadingCatalog by remember { mutableStateOf(true) }
    var downloading by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(installer) {
        loadingCatalog = true
        try {
            dataCheck = installer.checkForUpdate()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.w("QuranDatabase", "Gagal memuat informasi database", e)
            failed = true
        } finally {
            loadingCatalog = false
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        CenteredContent {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    stringResource(R.string.onb_step, 1, 4),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(88.dp),
                ) {
                    Icon(
                        AppIcons.Download,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(22.dp),
                    )
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    stringResource(R.string.onb_welcome_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.onb_welcome_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
                LanguageSection(showTitle = false)
                Spacer(Modifier.height(24.dp))
                Text(
                    stringResource(R.string.onb_download_title),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.onb_download_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                dataCheck?.let { check ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (check.current) stringResource(R.string.quran_data_current, check.latestVersion)
                        else stringResource(
                            R.string.onb_download_size,
                            Formatter.formatShortFileSize(context, check.downloadBytes),
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (downloading) {
                    Spacer(Modifier.height(20.dp))
                    val current = progress
                    if (current == null || current.total <= 0) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    } else {
                        LinearProgressIndicator(
                            progress = { (current.downloaded.toFloat() / current.total).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(
                                R.string.onb_downloading,
                                Formatter.formatShortFileSize(context, current.downloaded),
                                Formatter.formatShortFileSize(context, current.total),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                } else if (failed) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.onb_download_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(24.dp))
                Button(
                    enabled = !loadingCatalog && !downloading,
                    onClick = {
                        scope.launch {
                            downloading = true
                            failed = false
                            try {
                                installer.install()
                                onInstalled()
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                android.util.Log.e("QuranDatabase", "Gagal mengunduh database", e)
                                failed = true
                            } finally {
                                downloading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (downloading) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.size(12.dp))
                    }
                    Text(stringResource(R.string.onb_download_start))
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.onb_download_full_features),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
