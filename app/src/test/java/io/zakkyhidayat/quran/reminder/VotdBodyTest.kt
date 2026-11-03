package io.zakkyhidayat.quran.reminder

import org.junit.Assert.assertEquals
import org.junit.Test

class VotdBodyTest {
    @Test
    fun arabicOnlyWhenNoTranslation() {
        assertEquals("الحمد لله", votdBody("الحمد لله", ""))
        assertEquals("الحمد لله", votdBody("الحمد لله", "   "))
    }

    @Test
    fun arabicThenTranslationSeparatedByBlankLine() {
        assertEquals("الحمد لله\n\nAll praise is due to Allah", votdBody("الحمد لله", "All praise is due to Allah"))
    }
}
