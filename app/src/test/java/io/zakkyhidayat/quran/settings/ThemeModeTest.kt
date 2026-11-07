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

    @Test
    fun threeStepsReturnToStartAndVisitEveryMode() {
        for (systemDark in listOf(false, true)) {
            for (start in ThemeMode.entries) {
                val visited = generateSequence(start) { it.next(systemDark) }.take(3).toSet()
                assertEquals("$start, systemDark=$systemDark", ThemeMode.entries.toSet(), visited)
                assertEquals(start, start.next(systemDark).next(systemDark).next(systemDark))
            }
        }
    }
}
