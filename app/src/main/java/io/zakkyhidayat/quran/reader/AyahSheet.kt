package io.zakkyhidayat.quran.reader

import io.zakkyhidayat.quran.ui.scaled
import io.zakkyhidayat.quran.ui.LocalReadingTextScale
import io.zakkyhidayat.quran.ui.InfoLabel
import androidx.compose.ui.res.stringResource
import io.zakkyhidayat.quran.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import io.zakkyhidayat.quran.data.AyahExtras
import androidx.compose.material3.TextButton
import io.zakkyhidayat.quran.data.Surah
import io.zakkyhidayat.quran.ui.AppIcons
import kotlinx.coroutines.launch

@Composable
fun AyahSheetContent(
    detail: AyahDetail,
    surah: Surah?,
    bookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    showTransliteration: Boolean = true,
    extras: AyahExtras? = null,
    onOpenAyah: (Int, Int) -> Unit = { _, _ -> },
    surahNames: Map<Int, Surah> = emptyMap(),
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val title = "${surah?.nameLatin ?: ""} ${detail.surah}:${detail.ayah}"
    val plainText = remember(detail) { ayahShareText(detail, title) }

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
                val info = detail.info
                Text(
                    stringResource(R.string.ayah_meta, detail.page, info.juz, info.hizb, info.rubInHizb, info.manzil, info.ruku),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onPrevious) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.previous_ayah)) }
            IconButton(onClick = onNext) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.next_ayah)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalIconToggleButton(checked = bookmarked, onCheckedChange = { onToggleBookmark() }) {
                Icon(
                    if (bookmarked) AppIcons.Bookmark else AppIcons.BookmarkBorder,
                    contentDescription = if (bookmarked) stringResource(R.string.remove_bookmark) else stringResource(R.string.bookmark_ayah),
                )
            }
            FilledTonalIconButton(onClick = {
                scope.launch { copyAyahText(clipboard, plainText) }
            }) { Icon(AppIcons.ContentCopy, contentDescription = stringResource(R.string.copy)) }
            FilledTonalIconButton(onClick = {
                shareAyahText(context, plainText)
            }) { Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share)) }
        }

        detail.info.sajda?.let { type ->
            Spacer(Modifier.height(4.dp))
            InfoLabel(stringResource(if (type == "required") R.string.sajda_ayah_required else R.string.sajda_ayah_recommended))
        }
        Spacer(Modifier.height(8.dp))
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            val arabicStyle = MaterialTheme.typography.headlineMedium.scaled(LocalReadingTextScale.current.arabic, 1.9f)
            Text(
                text = detail.arabic,
                style = arabicStyle.copy(fontFamily = remember { arabicFontFamily(context) }),
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        val romanized = detail.transliteration
        if (showTransliteration && !romanized.isNullOrBlank()) {
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.transliteration), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    val body = MaterialTheme.typography.bodyLarge.scaled(LocalReadingTextScale.current.translation, 1.6f)
                    Text(romanized, style = body.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
                }
            }
        }
        if (detail.translations.isEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.no_translation_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    val body = MaterialTheme.typography.bodyLarge.scaled(LocalReadingTextScale.current.translation, 1.6f)
                    Text(
                        text = translationText(tr.text, MaterialTheme.colorScheme.primary, MaterialTheme.typography.labelSmall.fontSize),
                        style = body,
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
        // Bagian tematik (data QUL opsional): tampil hanya bila ada isinya.
        if (extras != null && !extras.isEmpty) ExtrasSections(extras, surahNames, onOpenAyah)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ExtrasSections(extras: AyahExtras, surahs: Map<Int, Surah>, onOpenAyah: (Int, Int) -> Unit) {
    fun name(surah: Int, ayah: Int) = "${surahs[surah]?.nameLatin.orEmpty()} $surah:$ayah"
    if (extras.themes.isNotEmpty()) {
        ExtrasCard(stringResource(R.string.ayah_theme_title)) {
            extras.themes.forEach { t ->
                Text(t.theme, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${t.surah}:${t.ayahFrom}-${t.ayahTo}" + if (t.keywords.isNullOrBlank()) "" else " · ${t.keywords}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    if (extras.topics.isNotEmpty()) {
        ExtrasCard(stringResource(R.string.ayah_topics_title)) {
            Text(extras.topics.joinToString(" · ") { it.name }, style = MaterialTheme.typography.bodyMedium)
        }
    }
    if (extras.similar.isNotEmpty()) {
        ExtrasCard(stringResource(R.string.similar_ayahs_title)) {
            extras.similar.forEach { s ->
                TextButton(onClick = { onOpenAyah(s.surah, s.ayah) }) {
                    Text(stringResource(R.string.similar_ayah_item, name(s.surah, s.ayah), s.score))
                }
            }
        }
    }
    if (extras.mutashabihat.isNotEmpty()) {
        ExtrasCard(stringResource(R.string.mutashabihat_title)) {
            extras.mutashabihat.forEach { m ->
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(m.phrase, style = MaterialTheme.typography.titleLarge, modifier = Modifier.fillMaxWidth())
                }
                Text(
                    stringResource(R.string.mutashabihat_count, m.totalAyahs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                m.others.take(12).forEach { o ->
                    TextButton(onClick = { onOpenAyah(o.surah, o.ayah) }) { Text(name(o.surah, o.ayah)) }
                }
            }
        }
    }
}

@Composable
private fun ExtrasCard(title: String, content: @Composable () -> Unit) {
    Spacer(Modifier.height(16.dp))
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            content()
        }
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
