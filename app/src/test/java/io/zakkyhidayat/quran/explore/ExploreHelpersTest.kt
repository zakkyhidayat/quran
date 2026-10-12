package io.zakkyhidayat.quran.explore

import io.zakkyhidayat.quran.data.phraseWords
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExploreHelpersTest {
    @Test fun rangeFormat() {
        assertEquals("2:6–7", formatAyahRange(2, 6, 7))
        assertEquals("2:255", formatAyahRange(2, 255, 255))
    }

    @Test fun highlightsWordRangeInJoinedText() {
        val words = listOf("aa", "bbb", "c", "dd")
        // kata 2..3 = "bbb c" -> mulai di indeks 3, panjang 5
        assertEquals(listOf(3 until 8), highlightCharRanges(words, listOf(2..3)))
    }

    @Test fun mergesAdjacentAndClampsRanges() {
        val words = listOf("aa", "bbb", "c")
        assertEquals(listOf(0 until 6), highlightCharRanges(words, listOf(0..1, 2..2)))
        assertEquals(listOf(7 until 8), highlightCharRanges(words, listOf(3..9)))
        assertEquals(emptyList<IntRange>(), highlightCharRanges(words, listOf(5..9)))
        assertEquals(emptyList<IntRange>(), highlightCharRanges(emptyList(), listOf(1..2)))
    }

    @Test fun separateWordsStaySeparate() {
        assertEquals(listOf(0 until 2, 7 until 8), highlightCharRanges(listOf("aa", "bbb", "c"), listOf(1..1, 3..3)))
    }

    @Test fun ayahQuery() {
        assertTrue(matchesAyahQuery("2:255", 2, 255, "Al-Baqarah"))
        assertFalse(matchesAyahQuery("2:255", 2, 254, "Al-Baqarah"))
        assertTrue(matchesAyahQuery("baq", 2, 1, "Al-Baqarah"))
        assertTrue(matchesAyahQuery("2", 2, 9, "Al-Baqarah"))
        assertFalse(matchesAyahQuery("2", 3, 9, "Ali 'Imran"))
        assertTrue(matchesAyahQuery("  ", 3, 9, "Ali 'Imran"))
    }

    @Test fun rootQuery() {
        assertTrue(matchesRootQuery("دعو", "د ع و", "dEw"))
        assertTrue(matchesRootQuery("د ع", "د ع و", "dEw"))
        assertTrue(matchesRootQuery("DEW", "د ع و", "dEw"))
        assertFalse(matchesRootQuery("رجل", "د ع و", "dEw"))
    }

    @Test fun phraseCutsWords() {
        val w = listOf("a", "b", "c", "d")
        assertEquals("b c", phraseWords(w, 2, 3))
        assertEquals("c d", phraseWords(w, 3, 9))
        assertEquals("", phraseWords(emptyList(), 1, 2))
    }
}
