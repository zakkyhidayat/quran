package io.zakkyhidayat.quran.reader

import android.content.ClipData
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import io.zakkyhidayat.quran.data.AyahDetail
import io.zakkyhidayat.quran.data.Surah
import io.zakkyhidayat.quran.ui.AppIcons
import kotlinx.coroutines.launch

private val SUP = Regex("<sup>(\\d+)</sup>")

@Composable
fun AyahSheetContent(
    detail: AyahDetail,
    surah: Surah?,
    bookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val title = "${surah?.nameLatin ?: ""} ${detail.surah}:${detail.ayah}"
    val plainText = remember(detail) { shareText(detail, title) }

    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text("Halaman ${detail.page}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onPrevious) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Ayat sebelumnya") }
            IconButton(onClick = onNext) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Ayat berikutnya") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalIconToggleButton(checked = bookmarked, onCheckedChange = { onToggleBookmark() }) {
                Icon(
                    if (bookmarked) AppIcons.Bookmark else AppIcons.BookmarkBorder,
                    contentDescription = if (bookmarked) "Hapus bookmark" else "Bookmark ayat",
                )
            }
            FilledTonalIconButton(onClick = {
                scope.launch { clipboard.setClipEntry(ClipData.newPlainText("Ayat", plainText).toClipEntry()) }
            }) { Icon(AppIcons.ContentCopy, contentDescription = "Salin") }
            FilledTonalIconButton(onClick = {
                val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, plainText) }
                context.startActivity(Intent.createChooser(send, null))
            }) { Icon(Icons.Default.Share, contentDescription = "Bagikan") }
        }

        Spacer(Modifier.height(8.dp))
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            val arabicStyle = MaterialTheme.typography.headlineMedium
            Text(
                text = detail.arabic,
                style = arabicStyle.copy(fontFamily = remember { arabicFontFamily(context) }, lineHeight = arabicStyle.fontSize * 1.9f),
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        detail.translations.forEach { tr ->
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(tr.info.name, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    val body = MaterialTheme.typography.bodyLarge
                    Text(
                        text = translationText(tr.text, MaterialTheme.colorScheme.primary, MaterialTheme.typography.labelSmall.fontSize),
                        style = body.copy(lineHeight = body.fontSize * 1.6f),
                    )
                    if (tr.footnotes.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            tr.footnotes.forEach { note ->
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
                                            append(if (note.label != null) "${note.label}. " else "• ")
                                        }
                                        append(note.text)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

internal fun translationText(raw: String, markerColor: Color, markerSize: TextUnit): AnnotatedString = buildAnnotatedString {
    var last = 0
    for (m in SUP.findAll(raw)) {
        append(raw.substring(last, m.range.first))
        withStyle(SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = markerSize, color = markerColor, fontWeight = FontWeight.Bold)) {
            append(m.groupValues[1])
        }
        last = m.range.last + 1
    }
    append(raw.substring(last))
}

private fun shareText(detail: AyahDetail, title: String): String = buildString {
    append(detail.arabic)
    detail.translations.forEach { tr ->
        append("\n\n").append(SUP.replace(tr.text, ""))
        append("\n— ").append(tr.info.name)
    }
    append("\n\n(").append(title.trim()).append(')')
}
