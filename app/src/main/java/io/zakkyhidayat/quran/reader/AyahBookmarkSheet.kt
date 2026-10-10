package io.zakkyhidayat.quran.reader

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.zakkyhidayat.quran.R
import io.zakkyhidayat.quran.data.AyahDetail
import io.zakkyhidayat.quran.data.Surah
import io.zakkyhidayat.quran.ui.AppIcons
import io.zakkyhidayat.quran.ui.InfoLabel

/**
 * Sheet ayat varian lite: posisi ayat dan satu tombol bookmark, tanpa terjemahan atau aksi lain. Teks Arab tidak
 * ditampilkan ulang karena ayatnya sudah tersorot di halaman di belakang sheet.
 */
@Composable
fun AyahBookmarkSheet(
    detail: AyahDetail,
    surah: Surah?,
    bookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${surah?.nameLatin.orEmpty()} ${detail.surah}:${detail.ayah}", style = MaterialTheme.typography.titleLarge)
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
        detail.info.sajda?.let { type ->
            Spacer(Modifier.height(4.dp))
            InfoLabel(stringResource(if (type == "required") R.string.sajda_ayah_required else R.string.sajda_ayah_recommended))
        }
        Spacer(Modifier.height(12.dp))
        FilledTonalButton(onClick = onToggleBookmark, modifier = Modifier.fillMaxWidth()) {
            Icon(
                if (bookmarked) AppIcons.Bookmark else AppIcons.BookmarkBorder,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(stringResource(if (bookmarked) R.string.remove_bookmark else R.string.bookmark_ayah))
        }
    }
}
