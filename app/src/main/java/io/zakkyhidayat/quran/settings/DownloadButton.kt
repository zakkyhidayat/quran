package io.zakkyhidayat.quran.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.zakkyhidayat.quran.ui.AppIcons

/**
 * Tombol unduh satu-ikon ala Google Translate: ikon unduh saat diam; saat berjalan berubah menjadi lingkaran progres
 * dengan kotak henti di tengah, dan ketukan membatalkan. [progress] di bawah atau sama dengan 0 berarti belum diketahui
 * (lingkaran berputar).
 */
@Composable
internal fun DownloadButton(
    downloading: Boolean,
    progress: Float,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    startDescription: String,
    cancelDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector = AppIcons.Download,
) {
    IconButton(
        onClick = if (downloading) onCancel else onStart,
        enabled = enabled || downloading,
        modifier = modifier.semantics { contentDescription = if (downloading) cancelDescription else startDescription },
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (downloading) {
                if (progress > 0f) {
                    CircularProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                } else {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                }
                Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(1.5.dp)))
            } else {
                Icon(icon, contentDescription = null)
            }
        }
    }
}
