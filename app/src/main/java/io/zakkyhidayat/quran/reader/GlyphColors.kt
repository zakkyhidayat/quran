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

    // Satu warna rata untuk semua piksel dengan transparansi asli dipertahankan (baris alfa = identitas), jadi tepi huruf
    // tetap halus. Ambang luminans sebelumnya membuat huruf "diam" abu-abu dan piksel tepi berganti hitam/putih acak.
    // Penanda nomor ayat digambar terpisah dengan warna aslinya, jadi tidak perlu dipertahankan di sini.
    private fun monochrome(value: Float): ColorFilter {
        val row = floatArrayOf(0f, 0f, 0f, 0f, value)
        return ColorFilter.colorMatrix(ColorMatrix(row + row + row + floatArrayOf(0f, 0f, 0f, 1f, 0f)))
    }

    private val monochromeLight = monochrome(0f)
    private val monochromeDark = monochrome(242f)

    fun filter(tajweed: Boolean, dark: Boolean): ColorFilter? = when {
        tajweed && dark -> invertKeepHue
        tajweed -> null
        dark -> monochromeDark
        else -> monochromeLight
    }
}
