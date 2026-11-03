package io.zakkyhidayat.quran.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TafsirPassageTest {
    private val info = TranslationInfo("ar-tahrir", "ar", "التحرير والتنوير", downloaded = true, kind = "tafsir")

    @Test
    fun keyEncodesSurahAndAyah() {
        assertEquals(1001, TafsirKey.of(1, 1))
        assertEquals(2286, TafsirKey.of(2, 286))
        assertEquals(114006, TafsirKey.of(114, 6))
        assertEquals(114, TafsirKey.surah(114006))
        assertEquals(6, TafsirKey.ayah(114006))
    }

    @Test
    fun passageCoversItsRangeOnly() {
        val passage = TafsirPassage(info, fromSurah = 2, fromAyah = 282, toSurah = 2, toAyah = 283, text = "x")
        assertFalse(passage.contains(2, 281))
        assertTrue(passage.contains(2, 282))
        assertTrue(passage.contains(2, 283))
        assertFalse(passage.contains(2, 284))
    }

    @Test
    fun singleAyahPassage() {
        val passage = TafsirPassage(info, 1, 1, 1, 1, "x")
        assertTrue(passage.contains(1, 1))
        assertFalse(passage.contains(1, 2))
    }
}
