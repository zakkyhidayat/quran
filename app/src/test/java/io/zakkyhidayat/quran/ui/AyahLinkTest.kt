package io.zakkyhidayat.quran.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AyahLinkTest {
    @Test
    fun surahInfoLinksOpenTheFirstAyahOfTheRange() {
        assertEquals(2 to 255, parseAyahLink("/2/255"))
        assertEquals(2 to 67, parseAyahLink("/2/67-73"))
        assertEquals(36 to 1, parseAyahLink("/36"))
    }

    @Test
    fun invalidLinksAreIgnored() {
        assertNull(parseAyahLink("/115/1"))
        assertNull(parseAyahLink("https://example.com"))
        assertNull(parseAyahLink("/0"))
    }
}
