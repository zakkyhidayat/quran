package io.zakkyhidayat.quran.reader

import android.content.Context
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

// Font V4 berwarna punya 6 palet CPAL, tetapi Android selalu memakai palet 0 dan tidak punya API untuk memilih palet.
// Tabel CPAL versi 0 menyimpan indeks awal tiap palet; menimpa indeks palet 0 dengan indeks palet lain pada salinan font
// membuat palet itu aktif. Salinan dibuat saat pertama dibutuhkan dan disimpan di cache, jadi APK tidak bertambah besar.
internal enum class GlyphPalette(val index: Int) {
    LightTajweed(0), // P6: huruf berwarna tajwid
    LightPlain(3), // P3: huruf hitam, nomor ayat berwarna
    DarkTajweed(1), // P1: huruf putih dengan warna tajwid yang lebih cerah
    DarkPlain(4); // P4: huruf putih, nomor ayat berwarna

    companion object {
        fun of(tajweed: Boolean, dark: Boolean): GlyphPalette = when {
            dark && tajweed -> DarkTajweed
            dark -> DarkPlain
            tajweed -> LightTajweed
            else -> LightPlain
        }
    }
}

internal object PalettedFonts {
    private val cache = ConcurrentHashMap<String, FontFamily>()

    private fun key(name: String, palette: Int, copyColors: List<Pair<Int, Int>>) = "$name#$palette#$copyColors"

    /** Font yang sudah dimuat (tanpa kerja disk); null bila belum. Aman dipanggil di komposisi. */
    fun cached(name: String, palette: Int, copyColors: List<Pair<Int, Int>> = emptyList()): FontFamily? =
        cache[key(name, palette, copyColors)]

    /**
     * Font di assets/fonts/[name] dengan palet CPAL [palette] sebagai palet utama, sebagai typeface yang sudah dimuat.
     * Membaca aset, menambal palet, menulis salinan, dan memuat typeface semuanya terjadi di thread IO: dulu dikerjakan
     * di thread utama saat halaman baru muncul dan membuat geser halaman tersendat.
     */
    suspend fun load(context: Context, name: String, palette: Int, copyColors: List<Pair<Int, Int>> = emptyList()): FontFamily =
        cached(name, palette, copyColors) ?: withContext(Dispatchers.IO) { loadBlocking(context, name, palette, copyColors) }

    /** Versi sinkron untuk dipanggil dari thread latar belakang. */
    fun loadBlocking(context: Context, name: String, palette: Int, copyColors: List<Pair<Int, Int>> = emptyList()): FontFamily =
        cache.getOrPut(key(name, palette, copyColors)) {
            val typeface = if (palette == 0 && copyColors.isEmpty()) {
                android.graphics.Typeface.createFromAsset(context.assets, "fonts/$name")
            } else {
                val suffix = if (copyColors.isEmpty()) "" else "-" + copyColors.joinToString("") { "c${it.first}_${it.second}" }
                val file = File(File(context.cacheDir, "palette").apply { mkdirs() }, "${name.removeSuffix(".ttf")}-$palette$suffix.ttf")
                if (!file.exists() || file.length() == 0L) {
                    val original = context.assets.open("fonts/$name").use { it.readBytes() }
                    val tmp = File(file.parentFile, file.name + "." + Thread.currentThread().id + ".tmp")
                    tmp.writeBytes(withPalette(original, palette, copyColors))
                    if (!tmp.renameTo(file)) tmp.delete()
                }
                if (file.exists()) android.graphics.Typeface.createFromFile(file) else android.graphics.Typeface.createFromAsset(context.assets, "fonts/$name")
            }
            FontFamily(androidx.compose.ui.text.font.Typeface(typeface))
        }

    // copyColors: pasangan (tujuan, sumber) indeks warna di palet terpilih; warna sumber disalin ke warna tujuan.
    private fun withPalette(font: ByteArray, palette: Int, copyColors: List<Pair<Int, Int>>): ByteArray {
        val buf = ByteBuffer.wrap(font.copyOf())
        val tableCount = buf.getShort(4).toInt() and 0xFFFF
        for (i in 0 until tableCount) {
            val record = 12 + 16 * i
            if (buf.getInt(record) != CPAL_TAG) continue
            val offset = buf.getInt(record + 8)
            val paletteCount = buf.getShort(offset + 4).toInt() and 0xFFFF
            if (palette >= paletteCount) continue
            val first = buf.getShort(offset + 12 + 2 * palette).toInt() and 0xFFFF
            val records = offset + buf.getInt(offset + 8)
            for ((dst, src) in copyColors) {
                for (b in 0 until 4) buf.put(records + 4 * (first + dst) + b, buf.get(records + 4 * (first + src) + b))
            }
            buf.putShort(offset + 12, first.toShort())
        }
        return buf.array()
    }

    private const val CPAL_TAG = 0x4350414C // "CPAL"
}
