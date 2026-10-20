package io.zakkyhidayat.quran.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.zakkyhidayat.quran.R
import io.zakkyhidayat.quran.ui.AppIcons
import java.text.Collator
import java.util.Locale

// Katalog unduhan terjemahan dan tafsir memakai tampilan yang sama: kelompok per bahasa, baris paket dengan ukuran dan
// tombol unduh. Dipisahkan di sini supaya perbaikan di satu tempat tidak perlu disalin ke yang lain.

/**
 * Kelompok per bahasa (nama bahasa mengikuti bahasa aplikasi, urut abjad; paket di tiap kelompok urut nama), disaring oleh
 * [query]: cocok bila salah satu [searchTerms], nama bahasa, atau nama paket memuatnya.
 */
internal fun <T> groupPacksByLanguage(
    packs: List<T>,
    query: String,
    locale: Locale,
    languageOf: (T) -> String,
    searchTerms: (T) -> List<String>,
    nameOf: (T) -> String,
): List<Pair<String, List<T>>> {
    val q = query.trim().lowercase(locale)
    val collator = Collator.getInstance(locale)
    return packs
        .map { languageOf(it) to it }
        .filter { (language, pack) -> q.isEmpty() || (searchTerms(pack) + language).any { it.lowercase(locale).contains(q) } }
        .groupBy({ it.first }, { it.second })
        .toSortedMap(collator)
        .map { (language, list) -> language to list.sortedWith { x, y -> collator.compare(nameOf(x), nameOf(y)) } }
}

/**
 * Judul "tersedia untuk diunduh", lalu tiap bahasa dengan baris paketnya: nama, [source] (penulis, boleh kosong), ukuran, dan
 * tombol unduh atau lingkaran progres untuk paket [busyId]. Mengetuk baris memanggil [onClick] dengan paket dan [label]-nya
 * (dihitung di komposisi karena memakai string resource); baris tidak bisa diketuk selama ada unduhan lain ([busyId] bukan null).
 */
internal fun <T> LazyListScope.languageGroupedPackItems(
    groups: List<Pair<String, List<T>>>,
    key: (T) -> String,
    name: (T) -> String,
    source: (T) -> String?,
    bytes: (T) -> Long,
    label: @Composable (T) -> String,
    busyId: String?,
    progress: Float,
    locale: Locale,
    onClick: (T, String) -> Unit,
) {
    item(key = "catalog-title") {
        Text(
            stringResource(R.string.translations_downloadable),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
        )
    }
    groups.forEach { (language, packs) ->
        item(key = "lang-$language") {
            Text(
                language,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 2.dp),
            )
        }
        items(packs, key = key) { pack ->
            val packLabel = label(pack)
            val downloading = busyId == key(pack)
            ListItem(
                headlineContent = { Text(name(pack)) },
                supportingContent = source(pack)?.let { { Text(it) } },
                trailingContent = {
                    if (downloading) {
                        val cd = stringResource(R.string.translation_downloading_cd, packLabel)
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.size(24.dp).semantics { contentDescription = cd },
                        )
                    } else {
                        val downloadCd = stringResource(R.string.translation_download_cd, packLabel)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (bytes(pack) > 0) {
                                Text(
                                    stringResource(R.string.translation_size_mb, String.format(locale, "%.1f", bytes(pack) / 1_000_000.0)),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(40.dp).semantics { contentDescription = downloadCd },
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(AppIcons.Download, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                            }
                        }
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable(enabled = busyId == null) { onClick(pack, packLabel) },
            )
        }
    }
}
