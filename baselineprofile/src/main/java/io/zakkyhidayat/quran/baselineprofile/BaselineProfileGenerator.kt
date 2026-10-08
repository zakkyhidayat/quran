package io.zakkyhidayat.quran.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** ID varian pengukuran (lihat applicationIdSuffix ".bench" di app/build.gradle.kts). */
internal const val TARGET = "io.zakkyhidayat.quran.bench"

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(packageName = TARGET, includeInStartupProfile = true) {
        pressHome()
        startActivityAndWait()
        readingJourney()
    }
}

/** Alur yang paling sering: lewati onboarding, geser halaman, ganti mode baca, buka pengaturan dan daftar. */
internal fun MacrobenchmarkScope.readingJourney() {
    // Onboarding dan petunjuk hanya tampil di pemasangan baru; teks tombol mengikuti bahasa perangkat.
    device.findObject(By.text("Skip"))?.click() ?: device.findObject(By.text("Lewati"))?.click()
    device.waitForIdle()
    device.findObject(By.text("Got it"))?.click() ?: device.findObject(By.text("Mengerti"))?.click()
    device.waitForIdle()

    val width = device.displayWidth
    val height = device.displayHeight
    // Mushaf dibaca kanan ke kiri: geser ke kanan = halaman berikutnya.
    repeat(8) {
        device.swipe(width / 5, height / 2, width * 4 / 5, height / 2, 12)
        device.waitForIdle()
    }
    repeat(3) {
        device.swipe(width * 4 / 5, height / 2, width / 5, height / 2, 12)
        device.waitForIdle()
    }

    // Ganti mode baca lewat pill (tombol yang tidak aktif punya contentDescription).
    clickDesc("Ayah + translation", "Ayat + terjemahan")
    device.wait(Until.hasObject(By.scrollable(true)), 3_000)
    device.findObject(By.scrollable(true))?.fling(Direction.DOWN)
    device.waitForIdle()
    clickDesc("Mushaf", "Mushaf")
    device.waitForIdle()

    clickDesc("Settings", "Pengaturan")
    device.wait(Until.hasObject(By.scrollable(true)), 3_000)
    device.findObject(By.scrollable(true))?.fling(Direction.DOWN)
    device.pressBack()
    device.waitForIdle()

    clickDesc("Surah and juz list", "Daftar surah dan juz")
    device.wait(Until.hasObject(By.scrollable(true)), 3_000)
    device.findObject(By.scrollable(true))?.fling(Direction.DOWN)
    device.pressBack()
    device.waitForIdle()
}

private fun MacrobenchmarkScope.clickDesc(vararg descriptions: String) {
    for (d in descriptions) {
        val node = device.findObject(By.desc(d))
        if (node != null) {
            node.click()
            device.waitForIdle()
            return
        }
    }
}
