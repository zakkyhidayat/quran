package io.zakkyhidayat.quran.reminder

import org.junit.Assert.assertEquals
import org.junit.Test

class DailyPortionMathTest {
    // Tiga bagian dalam 10 ayat: 1-3, 4-7, 8-10.
    private val starts = listOf(1, 4, 8)

    @Test fun midPortionStaysInPortion() = assertEquals(DailyPortion(2, 4, 7), DailyPortionMath.next(starts, 10, 5))

    @Test fun lastAyahOfPortionAdvances() = assertEquals(DailyPortion(2, 4, 7), DailyPortionMath.next(starts, 10, 3))

    @Test fun lastPortionWrapsToFirst() = assertEquals(DailyPortion(1, 1, 3), DailyPortionMath.next(starts, 10, 10))

    @Test fun fromOrdinalMapsToSurahAyah() {
        val counts = listOf(7, 286, 200)
        assertEquals(1 to 1, DailyPortionMath.fromOrdinal(counts, 1))
        assertEquals(1 to 7, DailyPortionMath.fromOrdinal(counts, 7))
        assertEquals(2 to 1, DailyPortionMath.fromOrdinal(counts, 8))
        assertEquals(3 to 200, DailyPortionMath.fromOrdinal(counts, 493))
    }
}
