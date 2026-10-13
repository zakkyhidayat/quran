package io.zakkyhidayat.quran.reader

import java.nio.ByteBuffer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class PalettedFontsTest {
    @Test
    fun darkTajweedPaletteKeepsAyahMedallionColor() {
        val palette = GlyphPalette.DarkTajweed

        assertEquals(GlyphPalette.DarkPlain.index, palette.colorSourcePalette)
        assertEquals(listOf(10 to 10), palette.copyColors)
    }

    @Test
    fun copiesSelectedSlotFromAnotherPalette() {
        val font = ByteBuffer.allocate(400)
        font.putShort(4, 1)
        val tableRecord = 12
        font.putInt(tableRecord, 0x4350414C)
        val cpalOffset = 28
        font.putInt(tableRecord + 8, cpalOffset)
        font.putShort(cpalOffset + 2, 16)
        font.putShort(cpalOffset + 4, 5)
        font.putInt(cpalOffset + 8, 22)
        for (palette in 0 until 5) {
            font.putShort(cpalOffset + 12 + palette * 2, (palette * 16).toShort())
        }
        val colorsOffset = cpalOffset + 22
        val sourceColorOffset = colorsOffset + 4 * (4 * 16 + 10)
        val expectedColor = byteArrayOf(11, 22, 33, 44)
        expectedColor.forEachIndexed { index, byte -> font.put(sourceColorOffset + index, byte) }
        val originalOtherColor = byteArrayOf(1, 2, 3, 4)
        originalOtherColor.forEachIndexed { index, byte -> font.put(colorsOffset + 4 * (1 * 16 + 9) + index, byte) }

        val patched = PalettedFonts.withPalette(font.array(), palette = 1, copyColors = listOf(10 to 10), colorSourcePalette = 4)

        assertArrayEquals(
            expectedColor,
            ByteArray(4) { patched[colorsOffset + 4 * (1 * 16 + 10) + it] },
        )
        assertArrayEquals(
            originalOtherColor,
            ByteArray(4) { patched[colorsOffset + 4 * (1 * 16 + 9) + it] },
        )
        assertEquals(16, ByteBuffer.wrap(patched).getShort(cpalOffset + 12 + 2).toInt())
    }
}
