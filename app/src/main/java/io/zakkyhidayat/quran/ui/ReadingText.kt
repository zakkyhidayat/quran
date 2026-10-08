package io.zakkyhidayat.quran.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit

/** Skala teks baca yang diatur pengguna: Arab (selain halaman mushaf) dan terjemahan, 1f = ukuran bawaan. */
data class ReadingTextScale(val arabic: Float = 1f, val translation: Float = 1f)

val LocalReadingTextScale = staticCompositionLocalOf { ReadingTextScale() }

/** Gaya teks dengan ukuran dan tinggi baris dikalikan [scale]; tinggi baris mengikuti [lineHeightFactor] x ukuran. */
fun TextStyle.scaled(scale: Float, lineHeightFactor: Float): TextStyle {
    val size: TextUnit = fontSize * scale
    return copy(fontSize = size, lineHeight = size * lineHeightFactor)
}
