package io.zakkyhidayat.quran.ui

import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Panduan M3 untuk jendela lebar: batasi lebar isi dan tengahkan, jangan melebarkan baris teks.
val ContentMaxWidth: Dp = 840.dp

@Composable
fun CenteredContent(
    modifier: Modifier = Modifier,
    maxWidth: Dp = ContentMaxWidth,
    content: @Composable () -> Unit,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = maxWidth).fillMaxWidth()) { content() }
    }
}

// Bentuk cookie dari poligon M3 Expressive: mahal dibuat, jadi dibuat sekali lalu dipakai semua lencana.
private var cookieShape: androidx.compose.ui.graphics.Shape? = null

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun rememberCookieShape(): androidx.compose.ui.graphics.Shape =
    cookieShape ?: MaterialShapes.Cookie9Sided.toShape().also { cookieShape = it }

@Composable
fun NumberBadge(number: Int, modifier: Modifier = Modifier) {
    // Kotak berlatar bentuk cookie (bukan Surface): dipakai di setiap baris daftar, jadi dibuat seringan mungkin.
    Box(
        modifier.size(48.dp).background(MaterialTheme.colorScheme.secondaryContainer, rememberCookieShape()),
        contentAlignment = Alignment.Center,
    ) {
        Text("$number", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

// Wadah item bersegmen memakai surfaceContainer supaya terpisah dari latar (tonal, tanpa bayangan).
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun segmentedItemColors(): ListItemColors =
    ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)

/**
 * Label informasi kecil (misalnya "7 ayat", "Makkiyah"). Chip M3 selalu interaktif; untuk keterangan yang tidak bisa
 * diketuk dipakai permukaan tonal dengan bentuk dan tipografi chip, tanpa riak dan tanpa peran tombol.
 */
@Composable
fun InfoLabel(text: String, modifier: Modifier = Modifier, leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    ) {
        androidx.compose.foundation.layout.Row(
            Modifier.heightIn(min = 32.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}
