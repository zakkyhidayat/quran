package io.zakkyhidayat.quran.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ArabicWordsTest {
    @Test fun dropsTrailingAyahNumber() {
        assertEquals(listOf("قُلۡ", "هُوَ"), arabicWords("قُلۡ هُوَ ١"))
    }

    @Test fun attachesLeadingSignToNextWord() {
        assertEquals(listOf("۞ إِنَّ", "ٱللَّهَ"), arabicWords("۞ إِنَّ ٱللَّهَ ٢٦"))
    }
}
