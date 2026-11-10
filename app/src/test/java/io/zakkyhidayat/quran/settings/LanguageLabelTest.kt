package io.zakkyhidayat.quran.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageLabelTest {
    private val options = listOf("" to "System", "en" to "English", "id" to "Bahasa Indonesia")

    @Test
    fun knownCodeUsesItsName() {
        assertEquals("Bahasa Indonesia", languageLabel(options, "id"))
        assertEquals("System", languageLabel(options, ""))
    }

    @Test
    fun unsupportedCodeIsShownAsIsInsteadOfFailing() {
        assertEquals("pt", languageLabel(options, "pt"))
    }
}
