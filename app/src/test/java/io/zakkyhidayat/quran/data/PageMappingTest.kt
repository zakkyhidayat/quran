package io.zakkyhidayat.quran.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

// Pemetaan ayat <-> halaman mushaf Madinah, dibaca dari quran.db yang sebenarnya.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class PageMappingTest {
    private val context: Application = ApplicationProvider.getApplicationContext()
    private lateinit var repo: MushafRepository

    @Before
    fun installTestDatabase() {
        val source = File("src/main/assets/quran.db")
        check(source.isFile) { "Test database source is missing: ${source.absolutePath}" }
        val target = QuranDatabaseInstaller.databaseFile(context)
        target.parentFile?.mkdirs()
        source.copyTo(target, overwrite = true)
        repo = MushafRepository(context)
    }

    @Test
    fun knownPagesOfTheMadaniMushaf() = runBlocking {
        assertEquals(1, repo.ayahPage(1, 1))
        assertEquals(2, repo.ayahPage(2, 1))
        assertEquals(42, repo.ayahPage(2, 255)) // ayat kursi
        assertEquals(50, repo.ayahPage(3, 1))
        assertEquals(604, repo.ayahPage(114, 6))
    }

    @Test
    fun firstAyahOfPages() = runBlocking {
        assertEquals(AyahRef(1, 1), repo.firstAyahOnPage(1))
        assertEquals(AyahRef(2, 1), repo.firstAyahOnPage(2))
        assertEquals(AyahRef(112, 1), repo.firstAyahOnPage(604))
    }

    @Test
    fun everyPageHasLinesAndAyahs() = runBlocking {
        for (page in listOf(1, 2, 3, 300, 603, 604)) {
            val lines = repo.page(page)
            assertTrue("page $page has lines", lines.isNotEmpty())
            assertTrue("page $page has ayah lines", lines.any { it.type == LineType.Ayah })
        }
        val meta = repo.pageMeta()
        assertEquals(604, meta.size)
        assertEquals(30, meta.last().juz)
    }
}
