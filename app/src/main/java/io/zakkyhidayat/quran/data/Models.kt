package io.zakkyhidayat.quran.data

enum class LineType { Ayah, SurahName, Basmallah }

data class Word(val text: String, val surah: Int, val ayah: Int, val isEnd: Boolean = false)

data class PageLine(
    val number: Int,
    val type: LineType,
    val centered: Boolean,
    val words: List<Word>,
    val surah: Int?,
)

data class Surah(
    val id: Int,
    val nameAr: String,
    val nameLatin: String,
    val ayahCount: Int,
    val firstPage: Int,
    val nameGlyph: Char,
)

// Titik awal sebuah bagian (juz, hizb, rub, manzil, ruku, atau sajdah). extra: nomor ruku di surah, atau tipe sajdah.
data class Marker(val id: Int, val surah: Int, val ayah: Int, val page: Int, val extra: String = "")

enum class MarkerKind(val table: String) { Juz("juz"), Hizb("hizb"), Rub("rub"), Manzil("manzil"), Ruku("ruku"), Sajda("sajda") }

data class AyahInfo(val juz: Int, val hizb: Int, val rubInHizb: Int, val manzil: Int, val ruku: Int, val sajda: String?)

data class PageMeta(val surah: Int, val juz: Int)

data class TranslationInfo(val id: String, val lang: String, val name: String, val downloaded: Boolean = false, val langName: String? = null)

data class Footnote(val label: Int?, val text: String)

data class TranslationText(val info: TranslationInfo, val text: String, val footnotes: List<Footnote>)

data class AyahDetail(
    val surah: Int,
    val ayah: Int,
    val page: Int,
    val arabic: String,
    val translations: List<TranslationText>,
    val info: AyahInfo,
    val transliteration: String?,
)

data class AyahRef(val surah: Int, val ayah: Int)

/** Letak satu ayat: untuk daftar ayat seluruh mushaf (mode baca daftar). */
data class AyahPos(val surah: Int, val ayah: Int, val page: Int)

data class AyahText(val surah: Int, val ayah: Int, val text: String)

data class SearchResult(
    val surah: Int,
    val ayah: Int,
    val page: Int,
    val source: String,
    val snippet: String,
    val highlightStart: Int,
    val highlightEnd: Int,
)

enum class BookmarkKind { Page, Ayah }

data class Bookmark(val id: Long, val kind: BookmarkKind, val page: Int, val surah: Int, val ayah: Int, val createdAt: Long)

data class SurahDetails(
    val place: String?,
    val ayahCount: Int,
    val firstPage: Int,
    val lastPage: Int,
    val juzFrom: Int,
    val juzTo: Int,
    val rukuCount: Int,
    val revelationOrder: Int?,
    val infoHtml: String,
)

// Penjelajahan tematik (data QUL opsional; semua kosong bila tabelnya tidak ada di quran.db).

/** Topik atau konsep. [ayahCount] = jumlah ayat yang ditautkan langsung ke topik ini. */
data class Topic(
    val id: Int,
    val name: String,
    val nameAr: String?,
    val description: String?,
    val parentId: Int?,
    val ayahCount: Int,
)

/** Tema untuk sekelompok ayat berurutan [ayahFrom]..[ayahTo] dalam satu surah. */
data class AyahTheme(val surah: Int, val ayahFrom: Int, val ayahTo: Int, val theme: String, val keywords: String?)

/** Ayat yang mirip dengan ayat lain. [score] 0-100; [fromWord]..[toWord] = kata yang cocok di ayat ini (1-based). */
data class SimilarAyah(val surah: Int, val ayah: Int, val score: Int, val coverage: Int, val fromWord: Int?, val toWord: Int?)

/** Frasa mutasyabihat pada satu ayat ([fromWord]..[toWord], 1-based inklusif) dan ayat lain yang memuat frasa yang sama. */
data class Mutashabih(val phraseId: Int, val fromWord: Int, val toWord: Int, val phrase: String, val others: List<AyahRef>, val totalAyahs: Int)

/** Semua tambahan tematik untuk satu ayat; null/kosong bila datanya tidak ada. */
data class AyahExtras(
    val themes: List<AyahTheme>,
    val topics: List<Topic>,
    val similar: List<SimilarAyah>,
    val mutashabihat: List<Mutashabih>,
) {
    val isEmpty get() = themes.isEmpty() && topics.isEmpty() && similar.isEmpty() && mutashabihat.isEmpty()
}

/** Morfologi satu kata (dari QUL; belum dipakai UI). [word] = posisi kata di ayat, 1-based. */
data class WordMorphology(
    val surah: Int,
    val ayah: Int,
    val word: Int,
    val root: String?,
    val lemma: String?,
    val stem: String?,
    val pos: String?,
)
