package io.zakkyhidayat.quran.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DivisionMathTest {
    private val counts = listOf(7, 286, 200)

    @Test
    fun ordinalCountsAyahsOfEarlierSurahs() {
        assertEquals(1, DivisionMath.ordinal(counts, 1, 1))
        assertEquals(8, DivisionMath.ordinal(counts, 2, 1))
        assertEquals(493, DivisionMath.ordinal(counts, 3, 200))
    }

    @Test
    fun progressWithinDivision() {
        val starts = listOf(1, 149, 260) // tiga bagian dari total 493 ayat
        assertEquals(DivisionProgress(1, 1, 148), DivisionMath.progress(starts, 493, 1))
        assertEquals(DivisionProgress(1, 148, 148), DivisionMath.progress(starts, 493, 148))
        assertEquals(DivisionProgress(2, 1, 111), DivisionMath.progress(starts, 493, 149))
        assertEquals(DivisionProgress(3, 234, 234), DivisionMath.progress(starts, 493, 493))
    }

    @Test
    fun emptyStartsFallBackToWholeRange() {
        assertEquals(DivisionProgress(1, 5, 10), DivisionMath.progress(emptyList(), 10, 5))
    }
}
