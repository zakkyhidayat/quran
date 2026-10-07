package io.zakkyhidayat.quran.data

object ArabicText {
    fun containsArabic(text: String): Boolean = text.any { it in '؀'..'ۿ' }

    // Hilangkan harakat dan tanda baca Quran, samakan bentuk alif, ya, dan ta marbutah.
    fun normalize(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text) {
            when (ch) {
                in 'ً'..'ٟ', 'ٰ', in 'ۖ'..'ۭ', 'ـ', ' ' -> Unit
                'أ', 'إ', 'آ', 'ٱ' -> sb.append('ا')
                'ى' -> sb.append('ي')
                'ة' -> sb.append('ه')
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }
}
