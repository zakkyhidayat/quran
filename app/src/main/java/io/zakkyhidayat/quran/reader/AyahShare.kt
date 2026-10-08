package io.zakkyhidayat.quran.reader

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.toClipEntry
import io.zakkyhidayat.quran.data.AyahDetail

internal val SUP = Regex("<sup>(\\d+)</sup>")

/** Teks salin/bagikan satu ayat: Arab, tiap terjemahan (tanpa penanda catatan kaki), lalu judul. Dipakai lembar ayat dan daftar ayat. */
internal fun ayahShareText(detail: AyahDetail, title: String): String = buildString {
    append(detail.arabic)
    detail.translations.forEach { tr ->
        append("\n\n").append(SUP.replace(tr.text, ""))
        append("\n— ").append(tr.info.name)
    }
    append("\n\n(").append(title.trim()).append(')')
}

internal suspend fun copyAyahText(clipboard: Clipboard, text: String) {
    clipboard.setClipEntry(ClipData.newPlainText("Ayah", text).toClipEntry())
}

internal fun shareAyahText(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
    context.startActivity(Intent.createChooser(send, null))
}
