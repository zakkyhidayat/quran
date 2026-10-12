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

    @Test
    fun sharedFileKeepsOtherEditionProfileAndSyncsDeletions() = runBlocking {
        val store = BookmarkStore(context)
        val settings = SettingsRepository(context)
        store.load()
        val file = File(context.cacheDir, "shared-test.json")
        // Berkas dari edisi lain: profil lite, satu bookmark, dan satu penanda hapus.
        val other0 = if (io.zakkyhidayat.quran.BuildConfig.LITE) "full" else "lite"
        file.writeText(
            """{"format":"io.zakkyhidayat.quran.backup","version":2,"app":"$other0",
            "profiles":{"$other0":{"settings":{"tajweed":true}}},
            "bookmarks":[{"kind":"Page","page":9,"surah":0,"ayah":0,"createdAt":100}],
            "deleted":[{"kind":"Page","page":8,"surah":0,"ayah":0,"deletedAt":500}]}""",
        )
        store.togglePage(8) // dibuat sekarang (> 500), jadi tidak terhapus
        Backup.export(context, Uri.fromFile(file), settings, store)

        val json = org.json.JSONObject(file.readText())
        val own = if (io.zakkyhidayat.quran.BuildConfig.LITE) "lite" else "full"
        val other = if (io.zakkyhidayat.quran.BuildConfig.LITE) "full" else "lite"
        assertTrue(json.getJSONObject("profiles").has(own))
        assertTrue(json.getJSONObject("profiles").has(other))
        assertEquals(setOf(8, 9), store.bookmarks.value.map { it.page }.toSet())
        assertEquals(2, json.getJSONArray("bookmarks").length())
    }

    @Test
    fun deletionPropagatesThroughTombstone() = runBlocking {
        val store = BookmarkStore(context)
        store.load()
        store.merge(listOf(Bookmark(0, BookmarkKind.Page, 12, 0, 0, 100L)))
        val result = store.merge(emptyList(), listOf(BookmarkStore.Deletion(BookmarkKind.Page, 12, 0, 0, 200L)))
        assertEquals(1, result.removed)
        assertTrue(store.bookmarks.value.isEmpty())
        // Bookmark lama dari berkas tidak muncul lagi setelah dihapus.
        assertEquals(0, store.merge(listOf(Bookmark(0, BookmarkKind.Page, 12, 0, 0, 100L))).added)
    }

    @Test
    fun v1FileRestoresBookmarksAndPosition() = runBlocking {
        val store = BookmarkStore(context)
        val settings = SettingsRepository(context)
        store.load()
        val file = File(context.cacheDir, "v1-test.json")
        file.writeText(
            """{"format":"io.zakkyhidayat.quran.backup","version":1,"settings":{"last_page":42,"last_surah":3,"last_ayah":1},
            "bookmarks":[{"kind":"Page","page":42,"surah":0,"ayah":0,"createdAt":1}]}""",
        )
        assertEquals(1, Backup.import(context, Uri.fromFile(file), settings, store).bookmarksAdded)
        assertEquals(42, settings.settings.first().lastPage)
    }
}
