package io.zakkyhidayat.quran.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TopicLocalizationTest {
    @Test
    fun `nama topik Arab dipakai pada UI Arab dan selain itu memakai fallback Inggris`() {
        val topic = Topic(1, "Day of Resurrection", "يوم القيامة", null, null, 1)
        assertEquals("يوم القيامة", topic.localizedName("ar"))
        assertEquals("Day of Resurrection", topic.localizedName("id"))
        assertEquals("Day of Resurrection", topic.copy(nameAr = null).localizedName("ar"))
    }
}
