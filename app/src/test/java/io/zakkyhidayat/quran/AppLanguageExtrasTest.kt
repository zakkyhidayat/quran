package io.zakkyhidayat.quran

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageExtrasTest {
    @Test
    fun indonesianCodesMapToIndonesian() {
        assertEquals("id", AppLanguage.extrasFromCode("id"))
        assertEquals("id", AppLanguage.extrasFromCode("in"))
    }

    @Test
    fun everythingElseFallsBackToEnglish() {
        assertEquals("en", AppLanguage.extrasFromCode("en"))
        assertEquals("en", AppLanguage.extrasFromCode("ar"))
        assertEquals("en", AppLanguage.extrasFromCode("fr"))
        assertEquals("en", AppLanguage.extrasFromCode(""))
    }
}
