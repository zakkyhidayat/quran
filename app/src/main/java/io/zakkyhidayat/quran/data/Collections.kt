package io.zakkyhidayat.quran.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

/** Rentang ayat; [to] 0 = sampai akhir surah. */
data class AyahRange(val surah: Int, val from: Int, val to: Int) {
    val wholeSurah get() = from == 1 && to == 0
    override fun toString() = when {
        wholeSurah -> "$surah"
        to == from -> "$surah:$from"
        else -> "$surah:$from-$to"
    }

    companion object {
        /** "2:255", "2:285-286", atau "67" (seluruh surah); dipisah koma. Bagian yang tidak sah dilewati. */
        fun parseList(refs: String): List<AyahRange> = refs.split(",").mapNotNull { parse(it.trim()) }

        fun parse(ref: String): AyahRange? {
            val surah = ref.substringBefore(":").toIntOrNull()?.takeIf { it in 1..114 } ?: return null
            if (':' !in ref) return AyahRange(surah, 1, 0)
            val ayahs = ref.substringAfter(":")
            val from = ayahs.substringBefore("-").toIntOrNull()?.takeIf { it > 0 } ?: return null
            val to = if ('-' in ayahs) ayahs.substringAfter("-").toIntOrNull()?.takeIf { it >= from } ?: return null else from
            return AyahRange(surah, from, to)
        }
    }
}

enum class CollectionKind(val key: String) { Dua("dua"), Solution("solution"), Etiquette("etiquette"), MajorSins("major_sins") }

data class CollectionItem(val index: Int, val refs: String, val title: String, val description: String?)

/**
 * Koleksi ayat kurasi QuranApp (doa, solusi, adab, dosa besar) dari assets/collections.json; lihat
 * tools/build_collections.py. Judul mengikuti bahasa aplikasi, jatuh ke bahasa Inggris bila tidak ada.
 */
class Collections(private val context: Context) {
    @Volatile private var root: JSONObject? = null

    private suspend fun root(): JSONObject = root ?: withContext(Dispatchers.IO) {
        JSONObject(context.assets.open("collections.json").bufferedReader().use { it.readText() }).also { root = it }
    }

    suspend fun items(kind: CollectionKind, lang: String): List<CollectionItem> {
        val array = root().optJSONArray(kind.key) ?: return emptyList()
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            CollectionItem(i, o.getString("refs"), localized(o.getJSONObject("title"), lang).orEmpty(), o.optJSONObject("description")?.let { localized(it, lang) })
        }
    }

    /** Kumpulan "Ayat hari ini": potongan dari koleksi kurasi, bukan ayat acak yang maknanya bisa terputus. */
    suspend fun dailyPool(): List<AyahRange> {
        val r = root()
        return listOf(CollectionKind.Solution, CollectionKind.Dua, CollectionKind.Etiquette).flatMap { kind ->
            val array = r.optJSONArray(kind.key) ?: return@flatMap emptyList()
            (0 until array.length()).flatMap { AyahRange.parseList(array.getJSONObject(it).getString("refs")) }
        }.let(::dailyCandidates)
    }

    private fun localized(o: JSONObject, lang: String): String? =
        o.optString(lang).ifEmpty { o.optString("en") }.ifEmpty { null }

    companion object {
        /** Maksimal 4 ayat agar muat di kartu; urutan diacak tetap (seed konstan) supaya tema berganti tiap hari. */
        fun dailyCandidates(ranges: List<AyahRange>): List<AyahRange> =
            ranges.filter { !it.wholeSurah && it.to - it.from < 4 }.distinct().sortedBy { it.surah * 1000 + it.from }.shuffled(kotlin.random.Random(114))

        fun pickForDay(pool: List<AyahRange>, day: LocalDate): AyahRange? =
            if (pool.isEmpty()) null else pool[Math.floorMod(day.toEpochDay(), pool.size.toLong()).toInt()]
    }
}

/** Bacaan yang dianjurkan dalam hadits shahih/hasan, dengan waktu yang disebut dalilnya. */
enum class SunnahReading(val refs: String, val time: SunnahTime) {
    KahfFriday("18", SunnahTime.Friday),
    MulkSajdah("32,67", SunnahTime.Night),
    Kursi("2:255", SunnahTime.Night),
    BaqarahEnd("2:285-286", SunnahTime.Night),
    ThreeQuls("112,113,114", SunnahTime.Night),
    KahfFirstTen("18:1-10", SunnahTime.Any);

    companion object {
        /** Yang waktunya sedang berlaku di depan, lalu sisanya dalam urutan semula. */
        fun ordered(now: LocalDateTime): List<SunnahReading> = entries.sortedBy { if (it.time.isNow(now)) 0 else 1 }
    }
}

enum class SunnahTime {
    /**
     * Hari Jumat dimulai sejak maghrib Kamis sampai maghrib Jumat. Waktu maghrib tidak dihitung (tanpa lokasi), jadi
     * dipakai pukul 18.00 sebagai pendekatan.
     */
    Friday,
    /** Malam / sebelum tidur: pukul 18.00 sampai 04.00. */
    Night,
    Any;

    fun isNow(now: LocalDateTime): Boolean {
        val evening = now.hour >= 18
        return when (this) {
            Friday -> (now.dayOfWeek == DayOfWeek.THURSDAY && evening) || (now.dayOfWeek == DayOfWeek.FRIDAY && !evening)
            Night -> evening || now.hour < 4
            Any -> false
        }
    }
}
