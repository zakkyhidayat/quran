package io.zakkyhidayat.quran.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import io.zakkyhidayat.quran.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** True bila katalog menawarkan versi paket yang lebih baru daripada yang terpasang. */
fun isPackUpdateAvailable(installedVersion: Int, catalogVersion: Int): Boolean = catalogVersion > installedVersion

/** Satu entri di catalog.json: paket terjemahan yang bisa diunduh. */
data class CatalogPack(
    val id: String,
    val lang: String,
    val name: String,
    val source: String,
    val file: String,
    val bytes: Long,
    val sha256: String,
    val version: Int,
    /** Nama bahasa Inggris untuk kode ISO 639-3 yang tidak dikenal Locale (mis. "mos" -> Moore); kosong untuk kode 639-1. */
    val langName: String? = null,
) {
    val info get() = TranslationInfo(id, lang, name, downloaded = true, langName = langName)
}

/**
 * Paket terjemahan yang diunduh dari katalog (GitHub Releases) dan disimpan di filesDir/translations/<id>.db.
 * Setiap paket berupa SQLite: info(key,value), translation_texts(surah,ayah,text), footnotes(surah,ayah,idx,label,text).
 */
class TranslationPacks(context: Context) {
    private val dir = File(context.applicationContext.filesDir, "translations")
    private val handles = HashMap<String, SQLiteDatabase>()

    /** Paket terpasang, urut nama berkas. Berkas rusak atau tanpa tabel info dilewati. */
    suspend fun installed(): List<TranslationInfo> = withContext(Dispatchers.IO) {
        val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".db") }?.sortedBy { it.name }.orEmpty()
        files.mapNotNull { file ->
            runCatching {
                val db = database(file.nameWithoutExtension) ?: return@runCatching null
                val info = db.rawQuery("SELECT key, value FROM info", null).use { c ->
                    buildMap { while (c.moveToNext()) put(c.getString(0), c.getString(1)) }
                }
                TranslationInfo(file.nameWithoutExtension, info.getValue("lang"), info.getValue("name"), downloaded = true, langName = info["lang_name"], version = info["version"]?.toIntOrNull() ?: 1)
            }.getOrNull()
        }
    }

    /** Handle baca-saja untuk paket terpasang (di-cache); null bila tidak ada atau tidak bisa dibuka. */
    @Synchronized
    fun database(id: String): SQLiteDatabase? {
        handles[id]?.let { if (it.isOpen) return it }
        val file = File(dir, "$id.db")
        if (!file.exists()) return null
        return runCatching { SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY) }
            .getOrNull()?.also { handles[id] = it }
    }

    @Synchronized
    fun delete(id: String) {
        handles.remove(id)?.close()
        File(dir, "$id.db").delete()
    }

    /** Ambil catalog.json. Melempar IOException bila tidak ada jaringan, status bukan 200, atau isinya tidak valid. */
    suspend fun catalog(): List<CatalogPack> = withContext(Dispatchers.IO) {
        val body = open(BuildConfig.TRANSLATION_CATALOG_URL).let { conn ->
            try { conn.inputStream.bufferedReader().use { it.readText() } } finally { conn.disconnect() }
        }
        try {
            val json = JSONObject(body)
            require(json.getInt("version") == 1) { "versi katalog tidak didukung" }
            val packs = json.getJSONArray("packs")
            List(packs.length()) { i ->
                val p = packs.getJSONObject(i)
                CatalogPack(
                    id = p.getString("id"), lang = p.getString("lang"), name = p.getString("name"),
                    source = p.optString("source"), file = p.getString("file"), bytes = p.optLong("bytes"),
                    sha256 = p.getString("sha256"), version = p.optInt("version", 1),
                    langName = p.optString("lang_name").takeIf { it.isNotBlank() },
                )
            }
        } catch (e: Exception) {
            throw IOException("Katalog tidak valid: ${e.message}", e)
        }
    }

    /**
     * Unduh paket ke berkas sementara, periksa sha256 dan isinya (6236 ayat), lalu pindahkan ke tempat akhir.
     * onProgress dipanggil dengan (byte terunduh, total byte) di thread IO.
     */
    suspend fun install(pack: CatalogPack, onProgress: (Long, Long) -> Unit = { _, _ -> }) = withContext(Dispatchers.IO) {
        dir.mkdirs()
        val tmp = File(dir, "${pack.id}.db.tmp")
        try {
            val url = BuildConfig.TRANSLATION_CATALOG_URL.substringBeforeLast('/') + "/" + pack.file
            val digest = MessageDigest.getInstance("SHA-256")
            val conn = open(url)
            try {
                val total = conn.contentLengthLong.takeIf { it > 0 } ?: pack.bytes
                var done = 0L
                conn.inputStream.use { input ->
                    tmp.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val n = input.read(buffer)
                            if (n < 0) break
                            output.write(buffer, 0, n)
                            digest.update(buffer, 0, n)
                            done += n
                            onProgress(done, total)
                        }
                    }
                }
            } finally {
                conn.disconnect()
            }
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            if (!hash.equals(pack.sha256, ignoreCase = true)) throw IOException("Berkas rusak (sha256 tidak cocok)")
            verify(tmp, pack.id)
            // Tutup handle lama (bila memperbarui paket yang sudah ada) sebelum menimpa berkasnya.
            synchronized(this@TranslationPacks) { handles.remove(pack.id)?.close() }
            val target = File(dir, "${pack.id}.db")
            // rename menimpa berkas lama secara atomik; hapus dulu hanya sebagai cadangan bila rename gagal.
            if (!tmp.renameTo(target)) {
                target.delete()
                if (!tmp.renameTo(target)) throw IOException("Gagal menyimpan paket")
            }
        } finally {
            tmp.delete()
        }
    }

    private fun verify(file: File, id: String) {
        val db = try {
            SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
        } catch (e: Exception) {
            throw IOException("Paket bukan SQLite yang valid", e)
        }
        db.use {
            val rows = it.rawQuery("SELECT COUNT(*) FROM translation_texts", null).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }
            if (rows != 6236) throw IOException("Paket tidak lengkap ($rows ayat)")
            val packId = it.rawQuery("SELECT value FROM info WHERE key = 'id'", null).use { c -> if (c.moveToFirst()) c.getString(0) else null }
            if (packId != id) throw IOException("Id paket tidak cocok")
        }
    }

    // GET dengan pengalihan manual (GitHub mengalihkan aset rilis ke host lain); hanya https.
    private fun open(start: String): HttpURLConnection {
        var url = URL(start)
        repeat(6) {
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.instanceFollowRedirects = false
            val code = conn.responseCode
            if (code in 300..399) {
                val location = conn.getHeaderField("Location")
                conn.disconnect()
                if (location == null) throw IOException("Pengalihan tanpa tujuan")
                url = URL(url, location)
                if (url.protocol != "https") throw IOException("Pengalihan ke alamat tidak aman")
            } else if (code == HttpURLConnection.HTTP_OK) {
                return conn
            } else {
                conn.disconnect()
                throw IOException("Server menjawab $code")
            }
        }
        throw IOException("Terlalu banyak pengalihan")
    }
}
