package io.zakkyhidayat.quran.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchParsingTest {
    @Test
    fun ayahReferencesInCommonForms() {
        assertEquals(2 to 255, parseAyahReference("2:255"))
        assertEquals(2 to 255, parseAyahReference("2.255"))
        assertEquals(2 to 255, parseAyahReference("2 255"))
        assertEquals(114 to 6, parseAyahReference(" 114 : 6 "))
    }

    @Test
    fun otherTextIsNotAReference() {
        assertNull(parseAyahReference("255"))
        assertNull(parseAyahReference("al-baqarah"))
        assertNull(parseAyahReference("2:2555"))
        assertNull(parseAyahReference("2:"))
    }
}
