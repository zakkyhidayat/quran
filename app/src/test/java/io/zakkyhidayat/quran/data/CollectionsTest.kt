package io.zakkyhidayat.quran.data

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectionsTest {
    @Test
    fun `rujukan ayat, rentang, dan seluruh surah`() {
        assertEquals(
            listOf(AyahRange(2, 285, 286), AyahRange(2, 255, 255), AyahRange(112, 1, 0)),
            AyahRange.parseList("2:285-286, 2:255,112"),
        )
        assertTrue(AyahRange(67, 1, 0).wholeSurah)
        assertEquals("2:285-286", AyahRange(2, 285, 286).toString())
        assertEquals("2:255", AyahRange(2, 255, 255).toString())
    }

    @Test
    fun `rujukan tidak sah dilewati`() {
        assertEquals(emptyList<AyahRange>(), AyahRange.parseList("0:1,115,2:0,2:5-3,abc,"))
    }

    @Test
    fun `ayat hari ini maksimal 4 ayat, tanpa surah utuh, dan stabil per hari`() {
        val pool = Collections.dailyCandidates(AyahRange.parseList("2:255,93:1-11,2:285-286,112,2:255,94:5-8"))
        assertEquals(setOf(AyahRange(2, 255, 255), AyahRange(2, 285, 286), AyahRange(94, 5, 8)), pool.toSet())
        assertEquals(3, pool.size)
        val day = LocalDate.of(2026, 10, 9)
        assertEquals(Collections.pickForDay(pool, day), Collections.pickForDay(pool, day))
        assertNull(Collections.pickForDay(emptyList(), day))
    }

    @Test
    fun `waktu bacaan sunnah`() {
        val thursdayEvening = LocalDateTime.of(2026, 10, 8, 19, 0)
        val fridayNoon = LocalDateTime.of(2026, 10, 9, 12, 0)
        val fridayEvening = LocalDateTime.of(2026, 10, 9, 19, 0)
        val saturdayMorning = LocalDateTime.of(2026, 10, 10, 9, 0)
        assertTrue(SunnahTime.Friday.isNow(thursdayEvening))
        assertTrue(SunnahTime.Friday.isNow(fridayNoon))
        assertFalse(SunnahTime.Friday.isNow(fridayEvening))
        assertTrue(SunnahTime.Night.isNow(fridayEvening))
        assertTrue(SunnahTime.Night.isNow(LocalDateTime.of(2026, 10, 10, 2, 0)))
        assertFalse(SunnahTime.Night.isNow(saturdayMorning))
        assertEquals(SunnahReading.KahfFriday, SunnahReading.ordered(fridayNoon).first())
        assertEquals(SunnahReading.MulkSajdah, SunnahReading.ordered(fridayEvening).first())
    }
}
