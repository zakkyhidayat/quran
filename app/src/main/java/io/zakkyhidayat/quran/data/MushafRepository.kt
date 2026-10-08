package io.zakkyhidayat.quran.data

import android.content.Context
import android.database.Cursor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MushafRepository(context: Context) {
    private val db = ContentDatabase.get(context)
    private val packs = TranslationPacks(context)

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
            "SELECT text, surah, ayah, is_end FROM words WHERE id BETWEEN ? AND ? ORDER BY id",
            arrayOf(first.toString(), last.toString()),
        ).use { c -> while (c.moveToNext()) words += Word(c.getString(0), c.getInt(1), c.getInt(2), c.getInt(3) == 1) }
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
    /** Nomor bagian (juz/hizb/manzil/...) yang memuat ayat ini. */
    suspend fun markerContaining(kind: MarkerKind, surah: Int, ayah: Int): Int =
        withContext(Dispatchers.IO) { containing(kind.table, surah, ayah) }

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
        val bundled = list.map { it.id }.toSet()
        list += packs.installed().filter { it.id !in bundled }
        list.also { translationCache = it }
    }

    /** Katalog paket yang bisa diunduh; melempar IOException bila gagal (offline, server, isi tidak valid). */
    suspend fun catalog(): List<CatalogPack> = packs.catalog()

    suspend fun installPack(pack: CatalogPack, onProgress: (Long, Long) -> Unit = { _, _ -> }) {
        try {
            packs.install(pack, onProgress)
        } finally {
            translationCache = null
        }
    }

    suspend fun deletePack(id: String) = withContext(Dispatchers.IO) {
        packs.delete(id)
        translationCache = null
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
        var place: String? = null
        var order: Int? = null
        db.rawQuery("SELECT place, revelation_order FROM surahs WHERE id = ?", arrayOf(id.toString())).use {
            if (it.moveToFirst()) {
                if (!it.isNull(0)) place = it.getString(0)
                if (!it.isNull(1)) order = it.getInt(1)
            }
        }
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
            revelationOrder = order,
            infoHtml = html,
        )
    }

    @Volatile private var ayahIndexCache: List<AyahPos>? = null

    /** Semua 6236 ayat beserta halamannya, urut mushaf. */
    suspend fun ayahIndex(): List<AyahPos> = ayahIndexCache ?: withContext(Dispatchers.IO) {
        val list = ArrayList<AyahPos>(6236)
        db.rawQuery("SELECT surah, ayah, page FROM ayahs ORDER BY surah, ayah", null).use { c ->
            while (c.moveToNext()) list += AyahPos(c.getInt(0), c.getInt(1), c.getInt(2))
        }
        list.also { ayahIndexCache = it }
    }

    private val divisionStartCache = java.util.concurrent.ConcurrentHashMap<MarkerKind, List<Int>>()

    /**
     * Kedudukan ayat di bagian [kind] (juz, hizb, rub', manzil, ruku): bagian ke berapa, ayat ke berapa di dalamnya,
     * dan jumlah ayat bagian itu. Awal bagian di-cache sebagai nomor urut ayat.
     */
    suspend fun divisionProgress(kind: MarkerKind, surah: Int, ayah: Int): DivisionProgress {
        val counts = surahs().values.map { it.ayahCount }
        val starts = divisionStartCache[kind] ?: markers(kind)
            .map { DivisionMath.ordinal(counts, it.surah, it.ayah) }
            .also { divisionStartCache[kind] = it }
        return DivisionMath.progress(starts, counts.sum(), DivisionMath.ordinal(counts, surah, ayah))
    }

    suspend fun firstAyahOnPage(page: Int): AyahRef = withContext(Dispatchers.IO) {
        db.rawQuery("SELECT surah, ayah FROM words WHERE page = ? ORDER BY id LIMIT 1", arrayOf(page.toString())).use {
            if (it.moveToFirst()) AyahRef(it.getInt(0), it.getInt(1)) else AyahRef(1, 1)
        }
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
            // Terjemahan bawaan ada di quran.db (kolom tr); paket unduhan punya berkas sendiri tanpa kolom tr.
            val source = if (info.downloaded) packs.database(id) ?: return@mapNotNull null else db
            val args = if (info.downloaded) key else arrayOf(id, key[0], key[1])
            val where = if (info.downloaded) "surah = ? AND ayah = ?" else "tr = ? AND surah = ? AND ayah = ?"
            val text = source.rawQuery("SELECT text FROM translation_texts WHERE $where", args)
                .use { if (it.moveToFirst()) it.getString(0) else "" }
            val notes = mutableListOf<Footnote>()
            source.rawQuery("SELECT label, text FROM footnotes WHERE $where ORDER BY idx", args).use { c -> while (c.moveToNext()) notes += Footnote(if (c.isNull(0)) null else c.getInt(0), c.getString(1)) }
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
            val infos = translations().associateBy { it.id }
            val results = mutableListOf<SearchResult>()
            for (id in translationIds) {
                val info = infos[id] ?: continue
                if (info.downloaded) {
                    // Paket unduhan tidak punya tabel ayahs; halaman dicari dari quran.db hanya untuk ayat yang cocok.
                    val pack = packs.database(id) ?: continue
                    val start = results.size
                    pack.rawQuery("SELECT surah, ayah, 0, text FROM translation_texts ORDER BY surah, ayah", null)
                        .use { c -> collect(c, needle, info.name, results, limit) }
                    for (i in start until results.size) results[i] = results[i].copy(page = ayahPageSync(results[i].surah, results[i].ayah))
                } else {
                    db.rawQuery(
                        "SELECT t.surah, t.ayah, a.page, t.text FROM translation_texts t JOIN ayahs a ON a.surah = t.surah AND a.ayah = t.ayah " +
                            "WHERE t.tr = ? ORDER BY t.surah, t.ayah",
                        arrayOf(id),
                    ).use { c -> collect(c, needle, info.name, results, limit) }
                }
                if (results.size >= limit) break
            }
            results
        }

    private fun ayahPageSync(surah: Int, ayah: Int): Int =
        db.rawQuery("SELECT page FROM ayahs WHERE surah = ? AND ayah = ?", arrayOf(surah.toString(), ayah.toString()))
            .use { if (it.moveToFirst()) it.getInt(0) else 1 }

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

    // ---- Penjelajahan tematik (data QUL opsional) ----

    /** Tabel opsional yang ada di quran.db; dibaca sekali dari sqlite_master. */
    private val optionalTables: Set<String> by lazy {
        buildSet { db.rawQuery("SELECT name FROM sqlite_master WHERE type = 'table'", null).use { while (it.moveToNext()) add(it.getString(0)) } }
    }

    val hasTopics get() = "topics" in optionalTables
    val hasMorphology get() = "word_morph" in optionalTables

    /** Apakah pintu masuk Jelajahi perlu tampil (saat ini: ada tabel topik). Query sqlite_master berjalan di thread IO. */
    suspend fun exploreAvailable(): Boolean = withContext(Dispatchers.IO) { hasTopics }

    /** Semua topik beserta jumlah ayatnya; kosong bila tabel tidak ada. */
    suspend fun topics(): List<Topic> = withContext(Dispatchers.IO) {
        if (!hasTopics) return@withContext emptyList()
        val list = mutableListOf<Topic>()
        db.rawQuery(
            "SELECT t.id, t.name, t.name_ar, t.description, t.parent_id, " +
                "(SELECT COUNT(*) FROM topic_ayahs a WHERE a.topic_id = t.id) FROM topics t ORDER BY t.name COLLATE NOCASE",
            null,
        ).use { c -> while (c.moveToNext()) list += topic(c) }
        list
    }

    private fun topic(c: Cursor) = Topic(
        c.getInt(0), c.getString(1), c.getString(2), c.getString(3), if (c.isNull(4)) null else c.getInt(4), c.getInt(5),
    )

    suspend fun topic(id: Int): Topic? = withContext(Dispatchers.IO) {
        if (!hasTopics) return@withContext null
        db.rawQuery(
            "SELECT t.id, t.name, t.name_ar, t.description, t.parent_id, " +
                "(SELECT COUNT(*) FROM topic_ayahs a WHERE a.topic_id = t.id) FROM topics t WHERE t.id = ?",
            arrayOf(id.toString()),
        ).use { if (it.moveToFirst()) topic(it) else null }
    }

    suspend fun topicChildren(id: Int): List<Topic> = withContext(Dispatchers.IO) {
        if (!hasTopics) return@withContext emptyList()
        val list = mutableListOf<Topic>()
        db.rawQuery(
            "SELECT t.id, t.name, t.name_ar, t.description, t.parent_id, " +
                "(SELECT COUNT(*) FROM topic_ayahs a WHERE a.topic_id = t.id) FROM topics t WHERE t.parent_id = ? ORDER BY t.name COLLATE NOCASE",
            arrayOf(id.toString()),
        ).use { c -> while (c.moveToNext()) list += topic(c) }
        list
    }

    suspend fun topicAyahs(id: Int): List<AyahRef> = withContext(Dispatchers.IO) {
        if (!hasTopics) return@withContext emptyList()
        val list = mutableListOf<AyahRef>()
        db.rawQuery("SELECT surah, ayah FROM topic_ayahs WHERE topic_id = ? ORDER BY surah, ayah", arrayOf(id.toString()))
            .use { c -> while (c.moveToNext()) list += AyahRef(c.getInt(0), c.getInt(1)) }
        list
    }

    /** Teks Arab satu ayat (untuk pratinjau di daftar); kosong bila ayat tidak ada. */
    suspend fun ayahArabic(surah: Int, ayah: Int): String = withContext(Dispatchers.IO) {
        db.rawQuery("SELECT text_ar FROM ayahs WHERE surah = ? AND ayah = ?", arrayOf(surah.toString(), ayah.toString()))
            .use { if (it.moveToFirst()) it.getString(0) else "" }
    }

    /** Tema, topik, ayat serupa, dan mutasyabihat untuk satu ayat. Bagian yang tabelnya tidak ada dikembalikan kosong. */
    suspend fun ayahExtras(surah: Int, ayah: Int): AyahExtras = withContext(Dispatchers.IO) {
        val key = arrayOf(surah.toString(), ayah.toString())
        val themes = mutableListOf<AyahTheme>()
        if ("ayah_themes" in optionalTables) {
            db.rawQuery(
                "SELECT surah, ayah_from, ayah_to, theme, keywords FROM ayah_themes WHERE surah = ? AND ayah_from <= ? AND ayah_to >= ? ORDER BY ayah_to - ayah_from",
                arrayOf(key[0], key[1], key[1]),
            ).use { c -> while (c.moveToNext()) themes += AyahTheme(c.getInt(0), c.getInt(1), c.getInt(2), c.getString(3), c.getString(4)) }
        }
        val topics = mutableListOf<Topic>()
        if (hasTopics) {
            db.rawQuery(
                "SELECT t.id, t.name, t.name_ar, t.description, t.parent_id, " +
                    "(SELECT COUNT(*) FROM topic_ayahs x WHERE x.topic_id = t.id) FROM topics t " +
                    "JOIN topic_ayahs a ON a.topic_id = t.id WHERE a.surah = ? AND a.ayah = ? ORDER BY t.name COLLATE NOCASE",
                key,
            ).use { c -> while (c.moveToNext()) topics += topic(c) }
        }
        val similar = mutableListOf<SimilarAyah>()
        if ("similar_ayahs" in optionalTables) {
            db.rawQuery(
                "SELECT sim_surah, sim_ayah, score, coverage, from_word, to_word FROM similar_ayahs WHERE surah = ? AND ayah = ? ORDER BY score DESC, sim_surah, sim_ayah LIMIT 30",
                key,
            ).use { c ->
                while (c.moveToNext()) {
                    similar += SimilarAyah(
                        c.getInt(0), c.getInt(1), c.getInt(2), c.getInt(3),
                        if (c.isNull(4)) null else c.getInt(4), if (c.isNull(5)) null else c.getInt(5),
                    )
                }
            }
        }
        AyahExtras(themes, topics, similar, if ("mutashabihat_ayahs" in optionalTables) mutashabihat(surah, ayah) else emptyList())
    }

    // Teks frasa dipotong dari kata-kata teks Arab ayat (indeks QUL 1-based, inklusif). Jumlah kata dicocokkan dengan tabel
    // words (kata V4 per ayat, tanpa penanda nomor); bila tidak sama, frasa ditampilkan kosong daripada salah potong.
    private fun mutashabihat(surah: Int, ayah: Int): List<Mutashabih> {
        val args = arrayOf(surah.toString(), ayah.toString())
        val expected = db.rawQuery("SELECT COUNT(*) FROM words WHERE surah = ? AND ayah = ? AND is_end = 0", args).use { if (it.moveToFirst()) it.getInt(0) else 0 }
        val tokens = db.rawQuery("SELECT text_ar FROM ayahs WHERE surah = ? AND ayah = ?", args)
            .use { if (it.moveToFirst()) arabicWords(it.getString(0)) else emptyList() }
        val words = if (tokens.size == expected) tokens else emptyList()
        val found = mutableListOf<Triple<Int, Int, Int>>()
        db.rawQuery(
            "SELECT phrase_id, from_word, to_word FROM mutashabihat_ayahs WHERE surah = ? AND ayah = ? ORDER BY from_word",
            arrayOf(surah.toString(), ayah.toString()),
        ).use { c -> while (c.moveToNext()) found += Triple(c.getInt(0), c.getInt(1), c.getInt(2)) }
        return found.map { (id, from, to) ->
            val others = mutableListOf<AyahRef>()
            db.rawQuery(
                "SELECT DISTINCT surah, ayah FROM mutashabihat_ayahs WHERE phrase_id = ? AND NOT (surah = ? AND ayah = ?) ORDER BY surah, ayah LIMIT 50",
                arrayOf(id.toString(), surah.toString(), ayah.toString()),
            ).use { c -> while (c.moveToNext()) others += AyahRef(c.getInt(0), c.getInt(1)) }
            val total = db.rawQuery("SELECT COUNT(DISTINCT surah * 1000 + ayah) FROM mutashabihat_ayahs WHERE phrase_id = ?", arrayOf(id.toString()))
                .use { if (it.moveToFirst()) it.getInt(0) else others.size + 1 }
            val phrase = if (words.isEmpty()) "" else words.subList((from - 1).coerceIn(0, words.size), to.coerceIn(0, words.size)).joinToString(" ")
            Mutashabih(id, from, to, phrase, others, total)
        }
    }

    /** Morfologi (akar, lema, stem) tiap kata satu ayat, urut posisi kata; kosong bila datanya tidak ada. Belum dipakai UI. */
    suspend fun wordMorphology(surah: Int, ayah: Int): List<WordMorphology> = withContext(Dispatchers.IO) {
        if (!hasMorphology) return@withContext emptyList()
        val list = mutableListOf<WordMorphology>()
        db.rawQuery(
            "SELECT m.word, r.text_ar, l.text, s.text, m.pos FROM word_morph m " +
                "LEFT JOIN morph_roots r ON r.id = m.root_id LEFT JOIN morph_lemmas l ON l.id = m.lemma_id " +
                "LEFT JOIN morph_stems s ON s.id = m.stem_id WHERE m.surah = ? AND m.ayah = ? ORDER BY m.word",
            arrayOf(surah.toString(), ayah.toString()),
        ).use { c -> while (c.moveToNext()) list += WordMorphology(surah, ayah, c.getInt(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4)) }
        list
    }

    suspend fun relatedTopics(id: Int): List<Topic> = withContext(Dispatchers.IO) {
        if ("topic_links" !in optionalTables) return@withContext emptyList()
        val list = mutableListOf<Topic>()
        db.rawQuery(
            "SELECT t.id, t.name, t.name_ar, t.description, t.parent_id, " +
                "(SELECT COUNT(*) FROM topic_ayahs a WHERE a.topic_id = t.id) FROM topics t " +
                "JOIN topic_links l ON l.related_id = t.id WHERE l.topic_id = ? ORDER BY t.name COLLATE NOCASE",
            arrayOf(id.toString()),
        ).use { c -> while (c.moveToNext()) list += topic(c) }
        list
    }

    private companion object {
        val SUP = Regex("<sup>\\d+</sup>")
    }
}

private val ARABIC_LETTER = Regex("[\u0621-\u064A]")
private val TRAILING_NUMBER = Regex("[\\s\u00A0]*[0-9\u0660-\u0669]+\\s*$")

/** Kata-kata teks Arab ayat tanpa penanda nomor di akhir; tanda tanpa huruf (misalnya ۞) menempel ke kata berikutnya. */
internal fun arabicWords(text: String): List<String> {
    val out = ArrayList<String>()
    var pending = ""
    for (token in text.replace(TRAILING_NUMBER, "").trim().split(Regex("[\\s\u00A0]+")).filter { it.isNotEmpty() }) {
        if (ARABIC_LETTER.containsMatchIn(token)) { out += if (pending.isEmpty()) token else "$pending $token"; pending = "" }
        else if (out.isNotEmpty()) out[out.lastIndex] = out.last() + " " + token
        else pending = token
    }
    return out
}

