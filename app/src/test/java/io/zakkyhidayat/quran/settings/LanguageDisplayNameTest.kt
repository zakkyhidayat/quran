package io.zakkyhidayat.quran.settings

import java.util.Locale
import org.junit.Test
import org.junit.Assert.assertEquals

class LanguageDisplayNameTest {
    @Test
    fun `kode 639-1 memakai nama dari Locale`() {
        assertEquals("French", languageDisplayName("fr", null, Locale.ENGLISH))
        assertEquals("Prancis", languageDisplayName("fr", null, Locale.forLanguageTag("id")))
    }

    @Test
    fun `kode tak dikenal memakai nama katalog lalu kodenya sendiri`() {
        assertEquals("Moore", languageDisplayName("xyz", "Moore", Locale.ENGLISH))
        assertEquals("zzz", languageDisplayName("zzz", null, Locale.ENGLISH))
        assertEquals("zzz", languageDisplayName("zzz", "", Locale.ENGLISH))
    }
}
