package io.zakkyhidayat.quran.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdaterTest {
    @Test
    fun newerVersionsAreDetected() {
        assertTrue(Updater.isNewer("0.3.0", "0.2.0"))
        assertTrue(Updater.isNewer("1.0.0", "0.9.9"))
        assertTrue(Updater.isNewer("0.2.10", "0.2.9")) // angka dibandingkan sebagai bilangan, bukan teks
        assertTrue(Updater.isNewer("0.3", "0.2.9")) // bagian yang hilang dianggap 0
    }

    @Test
    fun sameOrOlderVersionsAreNotOffered() {
        assertFalse(Updater.isNewer("0.2.0", "0.2.0"))
        assertFalse(Updater.isNewer("0.1.9", "0.2.0"))
        assertFalse(Updater.isNewer("0.2.0-beta", "0.2.0")) // sufiks pra-rilis diabaikan: sama dengan 0.2.0
    }
}
