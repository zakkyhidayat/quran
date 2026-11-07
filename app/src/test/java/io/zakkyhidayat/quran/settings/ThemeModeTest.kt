package io.zakkyhidayat.quran.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {
    @Test
    fun cycleStartsWithAVisibleChangeOnLightSystem() {
        assertEquals(ThemeMode.Dark, ThemeMode.System.next(systemDark = false))
        assertEquals(ThemeMode.Light, ThemeMode.Dark.next(systemDark = false))
        assertEquals(ThemeMode.System, ThemeMode.Light.next(systemDark = false))
    }

    @Test
    fun cycleStartsWithAVisibleChangeOnDarkSystem() {
        assertEquals(ThemeMode.Light, ThemeMode.System.next(systemDark = true))
        assertEquals(ThemeMode.Dark, ThemeMode.Light.next(systemDark = true))
        assertEquals(ThemeMode.System, ThemeMode.Dark.next(systemDark = true))
    }
}
