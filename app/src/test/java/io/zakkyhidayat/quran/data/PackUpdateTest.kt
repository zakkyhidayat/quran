package io.zakkyhidayat.quran.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackUpdateTest {
    @Test fun newerCatalogVersionOffersUpdate() = assertTrue(isPackUpdateAvailable(1, 2))

    @Test fun sameOrOlderCatalogVersionDoesNot() {
        assertFalse(isPackUpdateAvailable(2, 2))
        assertFalse(isPackUpdateAvailable(3, 2))
    }
}
