package io.zakkyhidayat.quran.reader

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.zakkyhidayat.quran.AppViewModel
import io.zakkyhidayat.quran.R
import io.zakkyhidayat.quran.data.TafsirEntry
import io.zakkyhidayat.quran.ui.HtmlContent
import io.zakkyhidayat.quran.ui.htmlTextLength

/** Tafsir yang teksnya ditulis kanan-ke-kiri. */
private val RTL_LANGUAGES = setOf("ar", "fa", "ur", "ps", "ug", "sd", "ku", "he")

/** Teks tafsir lebih panjang dari ini (huruf) dipotong dengan tombol "tampilkan selengkapnya"; entri terpanjang 170 ribu huruf. */
private const val PREVIEW_CHARS = 3_000

/**
 * Kartu tafsir di lembar ayat: pilih tafsir terpasang (bila lebih dari satu), lalu isi tafsir untuk ayat itu. Tanpa tafsir
 * terpasang, kartu mengajak mengunduh. Tafsir tidak punya status aktif; terpasang berarti tersedia (lihat halaman Tafsir).
 */
@Composable
internal fun TafsirCard(vm: AppViewModel, surah: Int, ayah: Int, onOpenTafsirSettings: () -> Unit) {
    val installed by vm.tafsirInstalled.collectAsStateWithLifecycle()
    val choice by vm.tafsirChoice.collectAsStateWithLifecycle()
    val selected = installed.firstOrNull { it.id == choice } ?: installed.firstOrNull()

    Spacer(Modifier.height(16.dp))
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(16.dp)) {
            if (selected == null) {
                Text(stringResource(R.string.tafsir), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.tafsir_sheet_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onOpenTafsirSettings) { Text(stringResource(R.string.add_tafsir)) }
                return@Column
            }

            // (sudah dimuat, isi): bedakan "sedang memuat" dari "tidak ada tafsir untuk ayat ini".
            val result by produceState(false to (null as TafsirEntry?), selected.id, surah, ayah) {
                value = true to vm.mushaf.tafsirFor(selected.id, surah, ayah)
            }
            val (loaded, entry) = result

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.tafsir), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                if (entry?.isGroup == true) {
                    Text(
                        " · ${entry.rangeLabel()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (installed.size > 1) {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    installed.forEach { pack ->
                        FilterChip(
                            selected = pack.id == selected.id,
                            onClick = { vm.tafsirChoice.value = pack.id },
                            label = { Text(pack.name) },
                        )
                    }
                }
            } else {
                Text(selected.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))

            when {
                !loaded -> Unit
                entry == null -> Text(
                    stringResource(R.string.tafsir_none_for_ayah),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> {
                    var expanded by rememberSaveable(selected.id, entry.startSurah, entry.startAyah) { mutableStateOf(false) }
                    val long = remember(entry.html) { htmlTextLength(entry.html) > PREVIEW_CHARS }
                    val direction = if (selected.lang in RTL_LANGUAGES) LayoutDirection.Rtl else LayoutDirection.Ltr
                    CompositionLocalProvider(LocalLayoutDirection provides direction) {
                        HtmlContent(entry.html, Modifier.fillMaxWidth(), maxChars = if (expanded) Int.MAX_VALUE else PREVIEW_CHARS) { }
                    }
                    if (long) {
                        TextButton(onClick = { expanded = !expanded }) {
                            Text(stringResource(if (expanded) R.string.tafsir_show_less else R.string.tafsir_show_more))
                        }
                    }
                }
            }
        }
    }
}
