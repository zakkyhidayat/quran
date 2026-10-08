package io.zakkyhidayat.quran.reminder

import io.zakkyhidayat.quran.data.DivisionMath

/** Bagian harian: bagian ke-[number], dari ayat bernomor urut [start] sampai [end] (keduanya inklusif, mulai 1). */
data class DailyPortion(val number: Int, val start: Int, val end: Int)

/** Hitungan murni (tanpa database) untuk bagian yang disebut pengingat. */
object DailyPortionMath {
    /**
     * Bagian yang dibaca hari ini: bagian yang memuat posisi baca ([ordinal]); bila posisi sudah di ayat terakhirnya,
     * bagian berikutnya (setelah bagian terakhir kembali ke yang pertama). [starts]: nomor urut ayat awal tiap bagian.
     */
    fun next(starts: List<Int>, totalAyahs: Int, ordinal: Int): DailyPortion {
        if (starts.isEmpty()) return DailyPortion(1, 1, totalAyahs)
        val p = DivisionMath.progress(starts, totalAyahs, ordinal)
        val i = if (p.n >= p.total) p.index % starts.size else p.index - 1
        val start = starts[i]
        val end = if (i + 1 < starts.size) starts[i + 1] - 1 else totalAyahs
        return DailyPortion(i + 1, start, end)
    }

    /** Kebalikan [DivisionMath.ordinal]: (surah, ayat) dari nomor urut ayat. */
    fun fromOrdinal(ayahCounts: List<Int>, ordinal: Int): Pair<Int, Int> {
        var left = ordinal
        for ((i, count) in ayahCounts.withIndex()) {
            if (left <= count) return (i + 1) to left.coerceAtLeast(1)
            left -= count
        }
        return ayahCounts.size to (ayahCounts.lastOrNull() ?: 1)
    }
}
