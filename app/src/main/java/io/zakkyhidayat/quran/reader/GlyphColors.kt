package io.zakkyhidayat.quran.reader

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

// Font V4 berwarna memakai palet 0 (teks hitam). Android tidak bisa memilih palet CPAL,
// jadi mode gelap dan mode tanpa tajwid dibuat dengan filter warna di atas hasil gambar glyph.
internal object GlyphColors {
    // invert + hue-rotate 180: kecerahan dibalik, rona dipertahankan. Baris = -H, offset 255.
    private val invertKeepHue = ColorFilter.colorMatrix(
        ColorMatrix(
            floatArrayOf(
                0.574f, -1.430f, -0.144f, 0f, 255f,
                -0.426f, -0.430f, -0.144f, 0f, 255f,
                -0.426f, -1.430f, 0.856f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f,
            ),
        ),
    )

    // Luminans di bawah ~0,65 jadi hitam, di atas ~0,85 jadi putih: huruf berwarna tajwid menjadi hitam,
    // isi ornamen ayat yang pucat tetap terang.
    private const val GAIN = 5f
    private const val THRESHOLD = 0.65f
    private const val R = 0.213f
    private const val G = 0.715f
    private const val B = 0.072f

    private fun monochrome(inverted: Boolean): ColorFilter {
        val s = if (inverted) -1f else 1f
        val offset = if (inverted) 255f + GAIN * THRESHOLD * 255f else -GAIN * THRESHOLD * 255f
        val row = floatArrayOf(s * GAIN * R, s * GAIN * G, s * GAIN * B, 0f, offset)
        return ColorFilter.colorMatrix(
            ColorMatrix(row + row + row + floatArrayOf(0f, 0f, 0f, 1f, 0f)),
        )
    }

    private val monochromeLight = monochrome(inverted = false)
    private val monochromeDark = monochrome(inverted = true)

    fun filter(tajweed: Boolean, dark: Boolean): ColorFilter? = when {
        tajweed && dark -> invertKeepHue
        tajweed -> null
        dark -> monochromeDark
        else -> monochromeLight
    }
}
