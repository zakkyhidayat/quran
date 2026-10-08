package io.zakkyhidayat.quran.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArabicTextTest {
    private fun sameForSearch(typed: String, mushaf: String) =
        assertEquals("$typed vs $mushaf", ArabicText.normalize(typed), ArabicText.normalize(mushaf))

    private fun found(typed: String, ayah: String) =
        assertTrue("$typed in $ayah", ArabicText.normalize(ayah).contains(ArabicText.normalize(typed)))

    @Test
    fun typedSpellingMatchesMushafSpelling() {
        sameForSearch("الرحمن", "ٱلرَّحۡمَٰنِ")
        sameForSearch("إبراهيم", "إِبۡرَٰهِيمَ") // alif biasa vs alif kecil
        sameForSearch("إبراهيم", "إِبۡرَٰهِـۧمَ") // ya kecil (Al-Baqarah)
        sameForSearch("الكتاب", "ٱلۡكِتَٰبُ")
        sameForSearch("الصلاة", "ٱلصَّلَوٰةَ") // waw yang dibaca alif
        sameForSearch("الزكاة", "ٱلزَّكَوٰةَ")
        sameForSearch("رحمة", "رَحۡمَة")
        sameForSearch("هدى", "هُدًى")
        sameForSearch("الأرض", "ٱلۡأَرۡضِ")
    }

    @Test
    fun prefixedWordsAreFound() {
        found("إبراهيم", "وَإِذِ ٱبۡتَلَىٰٓ إِبۡرَٰهِـۧمَ رَبُّهُۥ")
        found("الرحمن", "وَٱلرَّحۡمَٰنِ")
        found("الصلاة", "وَأَقِيمُواْ ٱلصَّلَوٰةَ وَءَاتُواْ ٱلزَّكَوٰةَ")
    }

    @Test
    fun detectsArabicScript() {
        assertTrue(ArabicText.containsArabic("بسم"))
        assertTrue(ArabicText.containsArabic("surah بقرة"))
        assertFalse(ArabicText.containsArabic("al-baqarah 2:255"))
    }
}
