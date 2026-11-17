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

    /** Ayat tepat sebelum [ref]: ayat sebelumnya di surah yang sama, atau ayat terakhir surah sebelumnya; null di awal Qur'an. */
    fun previousAyah(ayahCounts: List<Int>, ref: AyahRef): AyahRef? = when {
        ref.ayah > 1 -> AyahRef(ref.surah, ref.ayah - 1)
        ref.surah <= 1 -> null
        else -> AyahRef(ref.surah - 1, ayahCounts.getOrElse(ref.surah - 2) { 0 })
    }

    /**
     * Rentang tiap bagian dari awal-awalnya ([starts], naik menurut urutan mushaf). Bagian berakhir tepat sebelum awal
     * bagian berikutnya; bagian terakhir berakhir di akhir Qur'an. Penanda ganda di satu ayat dipersempit menjadi satu ayat.
     */
    fun spans(starts: List<AyahRef>, ayahCounts: List<Int>): List<Pair<AyahRef, AyahRef>> {
        val last = AyahRef(ayahCounts.size, ayahCounts.lastOrNull() ?: 0)
        return starts.mapIndexed { i, start ->
            val end = starts.getOrNull(i + 1)?.let { previousAyah(ayahCounts, it) } ?: last
            val before = end == null || end.surah < start.surah || (end.surah == start.surah && end.ayah < start.ayah)
            start to if (before) start else end!!
        }
    }
}
