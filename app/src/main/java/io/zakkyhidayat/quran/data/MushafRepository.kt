package io.zakkyhidayat.quran.data

import android.content.Context
import android.database.Cursor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MushafRepository(context: Context) {
    private val db = ContentDatabase.get(context)

    @Volatile private var surahCache: Map<Int, Surah>? = null
    private val markerCache = java.util.concurrent.ConcurrentHashMap<MarkerKind, List<Marker>>()
    @Volatile private var pageMetaCache: List<PageMeta>? = null
    @Volatile private var translationCache: List<TranslationInfo>? = null

    suspend fun page(page: Int): List<PageLine> = withContext(Dispatchers.IO) {
        val lines = mutableListOf<PageLine>()
        db.rawQuery(
            "SELECT line, type, centered, first_word, last_word, surah FROM page_lines WHERE page = ? ORDER BY line",
            arrayOf(page.toString()),
        ).use { c ->
            while (c.moveToNext()) {
                val type = when (c.getString(1)) {
                    "surah_name" -> LineType.SurahName
                    "basmallah" -> LineType.Basmallah
                    else -> LineType.Ayah
                }
                lines += PageLine(
                    number = c.getInt(0),
                    type = type,
                    centered = c.getInt(2) == 1,
                    words = if (!c.isNull(3)) words(c.getInt(3), c.getInt(4)) else emptyList(),
                    surah = if (c.isNull(5)) null else c.getInt(5),
                )
            }
        }
        lines
    }

    private fun words(first: Int, last: Int): List<Word> {
        val words = ArrayList<Word>(last - first + 1)
        db.rawQuery(
            "SELECT text, surah, ayah FROM words WHERE id BETWEEN ? AND ? ORDER BY id",
            arrayOf(first.toString(), last.toString()),
        ).use { c -> while (c.moveToNext()) words += Word(c.getString(0), c.getInt(1), c.getInt(2)) }
        return words
    }

    suspend fun surahs(): Map<Int, Surah> = surahCache ?: withContext(Dispatchers.IO) {
        val map = LinkedHashMap<Int, Surah>()
        db.rawQuery("SELECT id, name_ar, name_latin, ayah_count, first_page, name_glyph FROM surahs ORDER BY id", null).use { c ->
            while (c.moveToNext()) {
                map[c.getInt(0)] = Surah(c.getInt(0), c.getString(1), c.getString(2), c.getInt(3), c.getInt(4), c.getInt(5).toChar())
            }
        }
        map.also { surahCache = it }
    }

    suspend fun markers(kind: MarkerKind): List<Marker> = markerCache[kind] ?: withContext(Dispatchers.IO) {
        val list = mutableListOf<Marker>()
        val extra = when (kind) {
            MarkerKind.Ruku -> ", CAST(surah_ruku AS TEXT)"
            MarkerKind.Sajda -> ", type"
            else -> ", ''"
        }
        db.rawQuery("SELECT id, surah, ayah, page$extra FROM ${kind.table} ORDER BY id", null).use { c ->
            while (c.moveToNext()) list += Marker(c.getInt(0), c.getInt(1), c.getInt(2), c.getInt(3), c.getString(4))
        }
        list.also { markerCache[kind] = it }
    }

    suspend fun juz(): List<Marker> = markers(MarkerKind.Juz)

    // Bagian yang memuat ayat ini: penanda terakhir yang dimulai pada atau sebelum ayat tersebut.
    private fun containing(table: String, surah: Int, ayah: Int): Int =
        db.rawQuery(
            "SELECT id FROM $table WHERE surah < ? OR (surah = ? AND ayah <= ?) ORDER BY surah DESC, ayah DESC LIMIT 1",
            arrayOf(surah.toString(), surah.toString(), ayah.toString()),
        ).use { if (it.moveToFirst()) it.getInt(0) else 1 }

    private fun ayahInfo(surah: Int, ayah: Int): AyahInfo {
        val rub = containing("rub", surah, ayah)
        val sajda = db.rawQuery("SELECT type FROM sajda WHERE surah = ? AND ayah = ?", arrayOf(surah.toString(), ayah.toString()))
            .use { if (it.moveToFirst()) it.getString(0) else null }
        return AyahInfo(
            juz = containing("juz", surah, ayah),
            hizb = (rub - 1) / 4 + 1,
            rubInHizb = (rub - 1) % 4 + 1,
            manzil = containing("manzil", surah, ayah),
            ruku = containing("ruku", surah, ayah),
            sajda = sajda,
        )
    }

    // Indeks 0 = halaman 1. Surah = surah kata pertama di halaman itu.
    suspend fun pageMeta(): List<PageMeta> = pageMetaCache ?: withContext(Dispatchers.IO) {
        val juzStarts = juz().map { it.page }
        val surahByPage = IntArray(605)
        db.rawQuery("SELECT page, surah FROM words WHERE id IN (SELECT MIN(id) FROM words GROUP BY page)", null).use { c ->
            while (c.moveToNext()) surahByPage[c.getInt(0)] = c.getInt(1)
        }
        List(604) { i ->
            val page = i + 1
            PageMeta(surahByPage[page], juzStarts.indexOfLast { it <= page } + 1)
        }.also { pageMetaCache = it }
    }

    suspend fun translations(): List<TranslationInfo> = translationCache ?: withContext(Dispatchers.IO) {
        val list = mutableListOf<TranslationInfo>()
        db.rawQuery("SELECT id, lang, name FROM translations ORDER BY rowid", null).use { c ->
            while (c.moveToNext()) list += TranslationInfo(c.getString(0), c.getString(1), c.getString(2))
        }
        list.also { translationCache = it }
    }

    // Ayat yang punya kata di halaman ini (termasuk lanjutan dari halaman sebelumnya), untuk pembaca layar.
    suspend fun pageAyahs(page: Int): List<AyahText> = withContext(Dispatchers.IO) {
        val list = mutableListOf<AyahText>()
        db.rawQuery(
            "SELECT a.surah, a.ayah, a.text_ar FROM ayahs a WHERE EXISTS (" +
                "SELECT 1 FROM words w WHERE w.surah = a.surah AND w.ayah = a.ayah AND w.page = ?) ORDER BY a.surah, a.ayah",
            arrayOf(page.toString()),
        ).use { c -> while (c.moveToNext()) list += AyahText(c.getInt(0), c.getInt(1), c.getString(2)) }
        list
    }

    suspend fun surahDetails(id: Int, lang: String): SurahDetails = withContext(Dispatchers.IO) {
        val surah = surahs().getValue(id)
        val place = db.rawQuery("SELECT place FROM surahs WHERE id = ?", arrayOf(id.toString()))
            .use { if (it.moveToFirst() && !it.isNull(0)) it.getString(0) else null }
        val lastPage = db.rawQuery("SELECT MAX(page) FROM words WHERE surah = ?", arrayOf(id.toString())).use { if (it.moveToFirst()) it.getInt(0) else surah.firstPage }
        val ruku = db.rawQuery("SELECT COUNT(*) FROM ruku WHERE surah = ?", arrayOf(id.toString())).use { if (it.moveToFirst()) it.getInt(0) else 0 }
        val html = db.rawQuery("SELECT text FROM surah_info WHERE lang = ? AND surah = ?", arrayOf(lang, id.toString()))
            .use { if (it.moveToFirst()) it.getString(0) else "" }
        SurahDetails(
            place = place,
            ayahCount = surah.ayahCount,
            firstPage = surah.firstPage,
            lastPage = lastPage,
            juzFrom = containing("juz", id, 1),
            juzTo = containing("juz", id, surah.ayahCount),
            rukuCount = ruku,
            infoHtml = html,
        )
    }

    suspend fun ayahPage(surah: Int, ayah: Int): Int = withContext(Dispatchers.IO) {
        db.rawQuery("SELECT page FROM ayahs WHERE surah = ? AND ayah = ?", arrayOf(surah.toString(), ayah.toString())).use {
            if (it.moveToFirst()) it.getInt(0) else 1
        }
    }

    suspend fun ayahDetail(surah: Int, ayah: Int, translationIds: List<String>): AyahDetail = withContext(Dispatchers.IO) {
        val key = arrayOf(surah.toString(), ayah.toString())
        var arabic = ""
        var page = 1
        db.rawQuery("SELECT text_ar, page FROM ayahs WHERE surah = ? AND ayah = ?", key).use {
            if (it.moveToFirst()) { arabic = it.getString(0); page = it.getInt(1) }
        }
        val infos = translations().associateBy { it.id }
        val texts = translationIds.mapNotNull { id ->
            val info = infos[id] ?: return@mapNotNull null
            val text = db.rawQuery(
                "SELECT text FROM translation_texts WHERE tr = ? AND surah = ? AND ayah = ?",
                arrayOf(id, key[0], key[1]),
            ).use { if (it.moveToFirst()) it.getString(0) else "" }
            val notes = mutableListOf<Footnote>()
            db.rawQuery(
                "SELECT label, text FROM footnotes WHERE tr = ? AND surah = ? AND ayah = ? ORDER BY idx",
                arrayOf(id, key[0], key[1]),
            ).use { c -> while (c.moveToNext()) notes += Footnote(if (c.isNull(0)) null else c.getInt(0), c.getString(1)) }
            TranslationText(info, text, notes)
        }
        val transliteration = db.rawQuery("SELECT text FROM transliteration WHERE surah = ? AND ayah = ?", key)
            .use { if (it.moveToFirst()) it.getString(0) else null }
        AyahDetail(surah, ayah, page, arabic, texts, ayahInfo(surah, ayah), transliteration)
    }

    suspend fun neighbour(ref: AyahRef, step: Int): AyahRef? {
        val surahs = surahs()
        var s = ref.surah
        var a = ref.ayah + step
        if (a < 1) {
            s -= 1
            if (s < 1) return null
            a = surahs.getValue(s).ayahCount
        } else if (a > surahs.getValue(s).ayahCount) {
            s += 1
            if (s > 114) return null
            a = 1
        }
        return AyahRef(s, a)
    }

    suspend fun searchArabic(query: String, limit: Int = 100): List<SearchResult> = withContext(Dispatchers.IO) {
        val needle = ArabicText.normalize(query).trim()
        if (needle.isEmpty()) return@withContext emptyList()
        val results = mutableListOf<SearchResult>()
        db.rawQuery("SELECT surah, ayah, page, text_ar FROM ayahs ORDER BY surah, ayah", null).use { c ->
            while (c.moveToNext() && results.size < limit) {
                val arabic = c.getString(3)
                val normalized = ArabicText.normalize(arabic)
                val at = normalized.indexOf(needle)
                if (at >= 0) results += SearchResult(c.getInt(0), c.getInt(1), c.getInt(2), "Arab", arabic, -1, -1)
            }
        }
        results
    }

    suspend fun searchTranslations(query: String, translationIds: List<String>, limit: Int = 100): List<SearchResult> =
        withContext(Dispatchers.IO) {
            val needle = query.trim().lowercase()
            if (needle.length < 2) return@withContext emptyList()
            val names = translations().associate { it.id to it.name }
            val results = mutableListOf<SearchResult>()
            for (id in translationIds) {
                db.rawQuery(
                    "SELECT t.surah, t.ayah, a.page, t.text FROM translation_texts t JOIN ayahs a ON a.surah = t.surah AND a.ayah = t.ayah " +
                        "WHERE t.tr = ? ORDER BY t.surah, t.ayah",
                    arrayOf(id),
                ).use { c -> collect(c, needle, names[id] ?: id, results, limit) }
                if (results.size >= limit) break
            }
            results
        }

    private fun collect(c: Cursor, needle: String, source: String, out: MutableList<SearchResult>, limit: Int) {
        while (c.moveToNext() && out.size < limit) {
            val plain = SUP.replace(c.getString(3), "")
            val at = plain.lowercase().indexOf(needle)
            if (at < 0) continue
            val start = (at - 50).coerceAtLeast(0)
            val end = (at + needle.length + 70).coerceAtMost(plain.length)
            val prefix = if (start > 0) "…" else ""
            val snippet = prefix + plain.substring(start, end) + if (end < plain.length) "…" else ""
            val hs = prefix.length + (at - start)
            out += SearchResult(c.getInt(0), c.getInt(1), c.getInt(2), source, snippet, hs, hs + needle.length)
        }
    }

    private companion object {
        val SUP = Regex("<sup>\\d+</sup>")
    }
}
