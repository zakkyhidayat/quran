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
import java.security.MessageDigest
import java.util.zip.GZIPInputStream

/** Jenis paket unduhan selain terjemahan. [key] = nama larik di catalog.json, [table] = tabel isi di paketnya. */
enum class ContentKind(val key: String, val dir: String, val table: String) {
    SurahInfo("surah_info", "surah-info", "surah_info"),
    Tafsir("tafsir", "tafsir", "tafsir"),
}

/** Satu entri paket info surah atau tafsir di catalog.json. [bytes] = ukuran berkas gzip, [size] = ukuran database. */
data class ContentPack(
    val id: String,
    val lang: String,
    val name: String,
    val source: String,
    val file: String,
    val bytes: Long,
    val size: Long,
    val sha256: String,
    val version: Int,
)

/** Paket yang sudah terpasang, dibaca dari tabel `info` di berkasnya. */
data class InstalledContentPack(val id: String, val lang: String, val name: String, val source: String, val version: Int)

/**
 * Paket info surah dan tafsir yang diunduh dari katalog yang sama dengan terjemahan (GitHub Releases, tag `translations`)
 * dan disimpan di filesDir/content/<jenis>/<id>.db. Berbeda dengan paket terjemahan, berkas di katalog di-gzip.
 */
class ContentPacks(context: Context) {
    private val root = File(context.applicationContext.filesDir, "content")
    private val handles = HashMap<String, SQLiteDatabase>()

    private fun dir(kind: ContentKind) = File(root, kind.dir)

    /** Ambil daftar paket [kind] dari catalog.json; kosong bila rilis belum punya larik itu. Melempar IOException bila gagal. */
    suspend fun catalog(kind: ContentKind): List<ContentPack> = withContext(Dispatchers.IO) {
        val body = openHttpsDownload(BuildConfig.TRANSLATION_CATALOG_URL).let { conn ->
            try { conn.inputStream.bufferedReader().use { it.readText() } } finally { conn.disconnect() }
        }
        try {
            val json = JSONObject(body)
            require(json.getInt("version") == 1) { "versi katalog tidak didukung" }
            val packs = json.optJSONArray(kind.key) ?: return@withContext emptyList()
            List(packs.length()) { i ->
                val p = packs.getJSONObject(i)
                require(p.optString("compression", "gzip") == "gzip") { "kompresi tidak didukung" }
                ContentPack(
                    id = p.getString("id"), lang = p.getString("lang"), name = p.getString("name"),
                    source = p.optString("source"), file = p.getString("file"), bytes = p.optLong("bytes"),
                    size = p.optLong("size"), sha256 = p.getString("sha256"), version = p.optInt("version", 1),
                )
            }
        } catch (e: Exception) {
            throw IOException("Katalog tidak valid: ${e.message}", e)
        }
    }

    /** Paket terpasang, urut id. Berkas rusak atau tanpa tabel info dilewati. */
    suspend fun installed(kind: ContentKind): List<InstalledContentPack> = withContext(Dispatchers.IO) {
        val files = dir(kind).listFiles { f -> f.isFile && f.name.endsWith(".db") }?.sortedBy { it.name }.orEmpty()
        files.mapNotNull { file ->
            runCatching {
                val db = database(kind, file.nameWithoutExtension) ?: return@runCatching null
                val info = db.rawQuery("SELECT key, value FROM info", null).use { c ->
                    buildMap { while (c.moveToNext()) put(c.getString(0), c.getString(1)) }
                }
                InstalledContentPack(
                    file.nameWithoutExtension, info.getValue("lang"), info.getValue("name"),
                    info["author"].orEmpty(), info["version"]?.toIntOrNull() ?: 1,
                )
            }.getOrNull()
        }
    }

    /** Handle baca-saja untuk paket terpasang (di-cache); null bila tidak ada atau tidak bisa dibuka. */
    @Synchronized
    fun database(kind: ContentKind, id: String): SQLiteDatabase? {
        val key = "${kind.key}/$id"
        handles[key]?.let { if (it.isOpen) return it }
        val file = File(dir(kind), "$id.db")
        if (!file.exists()) return null
        return runCatching { SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY) }
            .getOrNull()?.also { handles[key] = it }
    }

    @Synchronized
    fun delete(kind: ContentKind, id: String) {
        handles.remove("${kind.key}/$id")?.close()
        File(dir(kind), "$id.db").delete()
    }

    /**
     * Unduh berkas gzip ke berkas sementara, periksa sha256, buka (dibatasi 1,25x ukuran database di katalog agar berkas
     * gzip yang membengkak tidak memenuhi penyimpanan), periksa isi database, lalu pindahkan ke tempat akhir.
     * onProgress dipanggil dengan (byte terunduh, total byte) di thread IO.
     */
    suspend fun install(kind: ContentKind, pack: ContentPack, onProgress: (Long, Long) -> Unit = { _, _ -> }) =
        withContext(Dispatchers.IO) {
            val target = File(dir(kind), "${pack.id}.db").also { it.parentFile?.mkdirs() }
            val archive = File(target.parentFile, "${pack.id}.db.gz.tmp")
            val unpacked = File(target.parentFile, "${pack.id}.db.tmp")
            try {
                val url = BuildConfig.TRANSLATION_CATALOG_URL.substringBeforeLast('/') + "/" + pack.file
                val digest = MessageDigest.getInstance("SHA-256")
                val conn = openHttpsDownload(url)
                try {
                    val total = conn.contentLengthLong.takeIf { it > 0 } ?: pack.bytes
                    var done = 0L
                    conn.inputStream.use { input ->
                        archive.outputStream().use { output ->
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
                val limit = if (pack.size > 0) pack.size + pack.size / 4 else 512L * 1024 * 1024
                GZIPInputStream(archive.inputStream().buffered()).use { input ->
                    unpacked.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var written = 0L
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val n = input.read(buffer)
                            if (n < 0) break
                            written += n
                            if (written > limit) throw IOException("Isi paket lebih besar dari yang tercatat")
                            output.write(buffer, 0, n)
                        }
                    }
                }
                verify(kind, unpacked, pack.id)
                // Tutup handle lama (bila memperbarui paket yang sudah ada) sebelum menimpa berkasnya.
                synchronized(this@ContentPacks) { handles.remove("${kind.key}/${pack.id}")?.close() }
                if (!unpacked.renameTo(target)) {
                    target.delete()
                    if (!unpacked.renameTo(target)) throw IOException("Gagal menyimpan paket")
                }
            } finally {
                archive.delete()
                unpacked.delete()
            }
        }

    private fun verify(kind: ContentKind, file: File, id: String) {
        val db = try {
            SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
        } catch (e: Exception) {
            throw IOException("Paket bukan SQLite yang valid", e)
        }
        db.use {
            val rows = it.rawQuery("SELECT COUNT(*) FROM ${kind.table}", null).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }
            if (rows == 0) throw IOException("Paket kosong")
            val info = it.rawQuery("SELECT key, value FROM info WHERE key IN ('id', 'kind')", null).use { c ->
                buildMap { while (c.moveToNext()) put(c.getString(0), c.getString(1)) }
            }
            if (info["id"] != id) throw IOException("Id paket tidak cocok")
            if (info["kind"] != kind.key) throw IOException("Jenis paket tidak cocok")
        }
    }
}
