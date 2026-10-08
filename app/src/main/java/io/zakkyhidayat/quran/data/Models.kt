package io.zakkyhidayat.quran.data

enum class LineType { Ayah, SurahName, Basmallah }

data class Word(val text: String, val surah: Int, val ayah: Int)

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

data class Juz(val id: Int, val surah: Int, val ayah: Int, val page: Int)

data class PageMeta(val surah: Int, val juz: Int)

data class TranslationInfo(val id: String, val lang: String, val name: String)

data class Footnote(val label: Int?, val text: String)

data class TranslationText(val info: TranslationInfo, val text: String, val footnotes: List<Footnote>)

data class AyahDetail(
    val surah: Int,
    val ayah: Int,
    val page: Int,
    val arabic: String,
    val translations: List<TranslationText>,
)

data class AyahRef(val surah: Int, val ayah: Int)

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
