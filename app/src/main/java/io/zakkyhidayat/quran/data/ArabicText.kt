package io.zakkyhidayat.quran.data

object ArabicText {
    fun containsArabic(text: String): Boolean = text.any { it in '؀'..'ۿ' }

    private const val DAGGER_ALIF = 'ٰ' // U+0670, alif kecil di atas huruf
    private const val SMALL_YEH = 'ۧ' // U+06E7, ya kecil (إِبۡرَٰهِـۧمَ)

    private fun isMark(ch: Char): Boolean =
        (ch in 'ً'..'ٟ' || ch in 'ۖ'..'ۭ' || ch == 'ـ') && ch != SMALL_YEH

    /**
     * "Kerangka" teks untuk pencarian, sama untuk ejaan mushaf (rasm Utsmani) dan ketikan biasa:
     * - harakat, tanda waqaf, tatweel, dan spasi dibuang;
     * - semua bentuk alif dibuang, termasuk alif kecil. Mushaf menulis إِبۡرَٰهِيمَ dan ٱلۡكِتَٰبُ dengan alif kecil,
     *   pengguna mengetik إبراهيم dan الكتاب dengan alif biasa; tanpa alif keduanya sama, juga bila berawalan (وَإِبۡرَٰهِيمَ);
     * - waw yang dibaca alif (ٱلصَّلَوٰةَ, ٱلزَّكَوٰةَ, ٱلۡحَيَوٰةَ) dibuang, sehingga cocok dengan الصلاة;
     * - ya kecil dihitung ya; ى menjadi ي, ة menjadi ه.
     */
    fun normalize(text: String): String {
        val sb = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            when {
                ch == ' ' || ch == '\n' || ch == DAGGER_ALIF || isMark(ch) -> Unit
                ch == 'ا' || ch == 'أ' || ch == 'إ' || ch == 'آ' || ch == 'ٱ' -> Unit
                ch == 'و' && followedByDaggerAlif(text, i) -> {
                    i = text.indexOf(DAGGER_ALIF, i)
                }
                else -> sb.append(
                    when (ch) {
                        'ى', SMALL_YEH -> 'ي'
                        'ة' -> 'ه'
                        else -> ch
                    },
                )
            }
            i++
        }
        return sb.toString()
    }

    private fun followedByDaggerAlif(text: String, index: Int): Boolean {
        var j = index + 1
        while (j < text.length && isMark(text[j])) j++
        return j < text.length && text[j] == DAGGER_ALIF
    }
}
