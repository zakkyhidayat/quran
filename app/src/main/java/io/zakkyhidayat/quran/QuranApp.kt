package io.zakkyhidayat.quran

import android.app.Application
import android.content.Context
import io.zakkyhidayat.quran.data.BookmarkStore
import io.zakkyhidayat.quran.data.MushafRepository
import io.zakkyhidayat.quran.settings.SettingsRepository

class QuranApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Font antarmuka disiapkan di belakang sejak awal (lihat preloadUiFonts).
        Thread({ io.zakkyhidayat.quran.reader.preloadUiFonts(this) }, "preload-fonts").apply { priority = Thread.MIN_PRIORITY }.start()
    }

    val mushaf by lazy { MushafRepository(this) }
    val settings by lazy { SettingsRepository(this) }
    val bookmarks by lazy { BookmarkStore(this) }
}

val Context.app: QuranApp get() = applicationContext as QuranApp
