package io.zakkyhidayat.quran.data

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import io.zakkyhidayat.quran.settings.SettingsRepository
import io.zakkyhidayat.quran.settings.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

// Application biasa (bukan QuranApp) agar tes tidak memuat font dari aset saat mulai.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class BookmarkAndBackupTest {
    private val context: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun bookmarksToggleOnAndOff() = runBlocking {
        val store = BookmarkStore(context)
        store.load()
        store.togglePage(5)
        store.toggleAyah(page = 2, surah = 2, ayah = 255)
        assertEquals(2, store.bookmarks.value.size)
        store.togglePage(5)
        assertEquals(listOf(BookmarkKind.Ayah), store.bookmarks.value.map { it.kind })
    }

    @Test
    fun importSkipsBookmarksThatAlreadyExist() = runBlocking {
        val store = BookmarkStore(context)
        store.load()
        store.togglePage(10)
        val incoming = listOf(
            Bookmark(0, BookmarkKind.Page, 10, 0, 0, 1L), // sudah ada
            Bookmark(0, BookmarkKind.Ayah, 50, 3, 7, 2L),
        )
        assertEquals(1, store.importAll(incoming))
        assertEquals(2, store.bookmarks.value.size)
    }

    @Test
    fun backupRestoresBookmarksAndSettings() = runBlocking {
        val store = BookmarkStore(context)
        val settings = SettingsRepository(context)
        store.load()
        store.toggleAyah(page = 1, surah = 1, ayah = 1)
        settings.setThemeMode(ThemeMode.Dark)
        settings.setPosition(page = 77, surah = 4, ayah = 1)

        val file = File(context.cacheDir, "backup-test.json")
        Backup.export(context, Uri.fromFile(file), settings, store)
        assertTrue(file.readText().contains("\"io.zakkyhidayat.quran.backup\""))

        // Ubah semuanya, lalu pulihkan.
        store.toggleAyah(page = 1, surah = 1, ayah = 1)
        settings.setThemeMode(ThemeMode.Light)
        settings.setPosition(page = 3, surah = 2, ayah = 20)
        val result = Backup.import(context, Uri.fromFile(file), settings, store)

        assertEquals(1, result.bookmarksAdded)
        val restored = settings.settings.first()
        assertEquals(ThemeMode.Dark, restored.themeMode)
        assertEquals(77, restored.lastPage)
        assertEquals(4, restored.lastSurah)
    }
}
