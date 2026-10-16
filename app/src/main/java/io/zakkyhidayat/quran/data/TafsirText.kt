package io.zakkyhidayat.quran.data

/** Tafsir [packId] untuk satu ayat: kelompok ayat [startSurah]:[startAyah]..[endSurah]:[endAyah] dan isinya sebagai HTML sederhana. */
data class TafsirEntry(
    val packId: String,
    val startSurah: Int,
    val startAyah: Int,
    val endSurah: Int,
    val endAyah: Int,
    val html: String,
) {
    /** True bila tafsir ini membahas lebih dari satu ayat. */
    val isGroup get() = startSurah != endSurah || startAyah != endAyah

    /** "2:1-5" untuk kelompok dalam satu surah, "2:285-3:2" bila melintasi surah. */
    fun rangeLabel(): String =
        if (startSurah == endSurah) "$startSurah:$startAyah-$endAyah" else "$startSurah:$startAyah-$endSurah:$endAyah"
}

/** Kunci ayat di paket tafsir: surah * 1000 + ayat (lihat tools/build_content_packs.py). */
internal fun tafsirKey(surah: Int, ayah: Int) = surah * 1000 + ayah

private val BLOCK_TAG = Regex("<(?:p|h[1-6]|li)\\b", RegexOption.IGNORE_CASE)

/**
 * Sebagian besar tafsir QUL berupa teks polos yang dipisah baris baru (tanpa tag p), padahal penampil HTML menggabungkan
 * baris baru menjadi satu paragraf. Teks tanpa tag blok dipecah per baris menjadi paragraf; teks yang sudah berblok tidak diubah.
 */
fun normalizeTafsirHtml(raw: String): String {
    if (BLOCK_TAG.containsMatchIn(raw)) return raw
    return raw.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("") { "<p>$it</p>" }
}
