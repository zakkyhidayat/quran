package io.zakkyhidayat.quran.data

/** Kedudukan sebuah ayat di dalam satu bagian: bagian ke-[index], ayat ke-[n] dari [total] ayat di bagian itu. */
data class DivisionProgress(val index: Int, val n: Int, val total: Int)

/** Hitungan murni (tanpa database) untuk penghitung ayat di bilah atas. */
object DivisionMath {
    /** Nomor urut ayat di seluruh mushaf (mulai 1) dari jumlah ayat tiap surah ([ayahCounts][0] = Al-Fatihah). */
    fun ordinal(ayahCounts: List<Int>, surah: Int, ayah: Int): Int {
        var before = 0
        for (i in 0 until (surah - 1).coerceIn(0, ayahCounts.size)) before += ayahCounts[i]
        return before + ayah
    }

    /**
     * [starts]: nomor urut ayat awal tiap bagian, naik. Bagian terakhir berakhir di [totalAyahs].
     * Ayat sebelum awal bagian pertama dihitung sebagai bagian pertama.
     */
    fun progress(starts: List<Int>, totalAyahs: Int, ordinal: Int): DivisionProgress {
        if (starts.isEmpty()) return DivisionProgress(1, ordinal, totalAyahs)
        val i = starts.indexOfLast { it <= ordinal }.coerceAtLeast(0)
        val start = starts[i]
        val end = if (i + 1 < starts.size) starts[i + 1] else totalAyahs + 1
        return DivisionProgress(i + 1, (ordinal - start + 1).coerceAtLeast(1), end - start)
    }
}
