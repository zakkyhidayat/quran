package io.zakkyhidayat.quran.explore

import io.zakkyhidayat.quran.data.ArabicText
import io.zakkyhidayat.quran.search.parseAyahReference

/** Rentang ayat untuk tampilan: "2:6–7", atau "2:6" bila hanya satu ayat. */
internal fun formatAyahRange(surah: Int, from: Int, to: Int): String =
    if (to <= from) "$surah:$from" else "$surah:$from–$to"

/**
 * Rentang karakter yang perlu disorot bila [words] digabung dengan satu spasi. [ranges] berisi indeks kata 1-based inklusif
 * (seperti data QUL); rentang di luar ayat dipotong, yang kosong dibuang, yang bertumpuk digabung.
 */
internal fun highlightCharRanges(words: List<String>, ranges: List<IntRange>): List<IntRange> {
    if (words.isEmpty()) return emptyList()
    val starts = IntArray(words.size)
    var pos = 0
    words.forEachIndexed { i, w -> starts[i] = pos; pos += w.length + 1 }
    val marked = BooleanArray(words.size)
    for (r in ranges) for (w in maxOf(r.first, 1)..minOf(r.last, words.size)) marked[w - 1] = true
    val out = mutableListOf<IntRange>()
    var i = 0
    while (i < words.size) {
        if (!marked[i]) { i++; continue }
        var j = i
        while (j + 1 < words.size && marked[j + 1]) j++
        out += starts[i] until (starts[j] + words[j].length)
        i = j + 1
    }
    return out
}

/** Cocokkan pencarian daftar ayat: "2:255" tepat, atau nomor/nama surah (Latin, tanpa memperhatikan huruf besar). */
internal fun matchesAyahQuery(query: String, surah: Int, ayah: Int, surahName: String): Boolean {
    val q = query.trim()
    if (q.isEmpty()) return true
    parseAyahReference(q)?.let { (s, a) -> return s == surah && a == ayah }
    if (q.all { it.isDigit() }) return q.toInt() == surah
    return surahName.contains(q, ignoreCase = true)
}

/** Cocokkan pencarian akar: huruf Arab (spasi diabaikan) atau transliterasi Latin. */
internal fun matchesRootQuery(query: String, arabic: String, latin: String?): Boolean {
    val q = query.trim()
    if (q.isEmpty()) return true
    val arabicQuery = ArabicText.normalize(q).filterNot { it.isWhitespace() }
    if (arabicQuery.any { it.code in 0x0600..0x06FF }) return ArabicText.normalize(arabic).filterNot { it.isWhitespace() }.contains(arabicQuery)
    return latin?.contains(q, ignoreCase = true) == true
}
