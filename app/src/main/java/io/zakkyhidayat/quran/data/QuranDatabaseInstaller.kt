package io.zakkyhidayat.quran.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import io.zakkyhidayat.quran.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.zip.GZIPInputStream

data class QuranDatabaseDownloadProgress(val downloaded: Long, val total: Long)

private data class QuranDatabaseAsset(val file: String, val bytes: Long, val sha256: String)

private data class QuranDatabaseCatalog(
    val databaseVersion: Int,
    val assets: Map<String, QuranDatabaseAsset>,
)

/**
 * Installs the flavor-specific SQLite database from the latest GitHub Release before the app opens its repository.
 * The compressed download is hash-checked, unpacked to a temporary file, validated, then atomically installed.
 */
class QuranDatabaseInstaller(context: Context) {
    private val appContext = context.applicationContext
    private val target = databaseFile(appContext)
    val progress = MutableStateFlow<QuranDatabaseDownloadProgress?>(null)

    suspend fun hasInstalledDatabase(): Boolean = withContext(Dispatchers.IO) {
        if (target.isFile && isValidDatabase(target)) return@withContext true

        val legacyFiles = target.parentFile
            ?.listFiles { file -> file.isFile && file.name.startsWith("quran-") && file.name.endsWith(".db") }
            .orEmpty()
            .sortedByDescending { it.lastModified() }
        for (legacy in legacyFiles) {
            if (!isValidDatabase(legacy)) continue
            val temp = File(target.parentFile, "${target.name}.migration")
            if (legacy.copyTo(temp, overwrite = true).renameTo(target)) return@withContext true
            temp.delete()
            throw IOException("Gagal memindahkan database yang sudah terpasang")
        }
        false
    }

    suspend fun downloadSize(): Long? = withContext(Dispatchers.IO) {
        catalog().assets[flavorKey()]?.bytes
            ?: throw IOException("Database untuk varian ini tidak tersedia")
    }

    suspend fun install() = withContext(Dispatchers.IO) {
        progress.value = null
        val metadata = catalog()
        val asset = metadata.assets[flavorKey()] ?: throw IOException("Database untuk varian ini tidak tersedia")
        val archive = File(target.parentFile, "${target.name}.gz.tmp")
        val database = File(target.parentFile, "${target.name}.tmp")
        val backup = File(target.parentFile, "${target.name}.old")
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val conn = openHttpsDownload("${BuildConfig.QURAN_DATA_CATALOG_URL.substringBeforeLast('/')}/${asset.file}")
            try {
                val total = conn.contentLengthLong.takeIf { it > 0 } ?: asset.bytes
                var downloaded = 0L
                conn.inputStream.use { input ->
                    archive.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                            digest.update(buffer, 0, count)
                            downloaded += count
                            progress.value = QuranDatabaseDownloadProgress(downloaded, total)
                        }
                    }
                }
                if (downloaded != asset.bytes) throw IOException("Ukuran database tidak cocok")
            } finally {
                conn.disconnect()
            }
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            if (!hash.equals(asset.sha256, ignoreCase = true)) throw IOException("Checksum database tidak cocok")

            GZIPInputStream(archive.inputStream().buffered()).use { input ->
                database.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                    }
                }
            }
            if (!isValidDatabase(database, metadata.databaseVersion)) {
                throw IOException("Database Qur’an tidak lengkap atau tidak kompatibel")
            }

            backup.delete()
            if (target.exists() && !target.renameTo(backup)) throw IOException("Gagal mengganti database lama")
            if (!database.renameTo(target)) {
                if (backup.exists()) backup.renameTo(target)
                throw IOException("Gagal menyimpan database")
            }
            backup.delete()
        } finally {
            archive.delete()
            database.delete()
            progress.value = null
        }
    }

    private fun catalog(): QuranDatabaseCatalog {
        val conn = openHttpsDownload(BuildConfig.QURAN_DATA_CATALOG_URL)
        val body = try {
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
        try {
            val json = JSONObject(body)
            if (json.getInt("version") != 1) throw IOException("Versi katalog database tidak didukung")
            val version = json.getInt("database_version")
            val databases = json.getJSONObject("databases")
            val assets = listOf("full", "lite").associateWith { key ->
                val item = databases.getJSONObject(key)
                val name = item.getString("file")
                if (!name.matches(Regex("[A-Za-z0-9._-]+\\.db\\.gz"))) throw IOException("Nama berkas database tidak valid")
                val bytes = item.getLong("bytes")
                val sha256 = item.getString("sha256")
                if (bytes <= 0 || !sha256.matches(Regex("[a-fA-F0-9]{64}"))) {
                    throw IOException("Metadata database tidak valid")
                }
                QuranDatabaseAsset(name, bytes, sha256)
            }
            if (version <= 0) throw IOException("Versi database tidak valid")
            return QuranDatabaseCatalog(version, assets)
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw IOException("Katalog database tidak valid: ${e.message}", e)
        }
    }

    private fun flavorKey() = if (BuildConfig.LITE) "lite" else "full"

    private fun isValidDatabase(file: File, expectedVersion: Int? = null): Boolean = runCatching {
        val db = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
        db.use {
            val integrity = it.rawQuery("PRAGMA integrity_check", null).use { cursor ->
                cursor.moveToFirst() && cursor.getString(0) == "ok"
            }
            val databaseVersion = it.rawQuery("PRAGMA user_version", null).use { cursor ->
                cursor.moveToFirst() && cursor.getInt(0) > 0 && (expectedVersion == null || cursor.getInt(0) == expectedVersion)
            }
            val validCounts = it.rawQuery(
                """SELECT
                    (SELECT COUNT(*) FROM surahs),
                    (SELECT COUNT(*) FROM ayahs),
                    (SELECT COUNT(DISTINCT page) FROM page_lines),
                    (SELECT COUNT(*) FROM juz),
                    (SELECT COUNT(*) FROM hizb),
                    (SELECT COUNT(*) FROM rub),
                    (SELECT COUNT(*) FROM manzil),
                    (SELECT COUNT(*) FROM sajda)""",
                null,
            ).use { cursor ->
                cursor.moveToFirst() &&
                    cursor.getInt(0) == 114 && cursor.getInt(1) == 6236 && cursor.getInt(2) == 604 &&
                    cursor.getInt(3) == 30 && cursor.getInt(4) == 60 && cursor.getInt(5) == 240 &&
                    cursor.getInt(6) == 7 && cursor.getInt(7) == 15
            }
            integrity && databaseVersion && validCounts
        }
    }.getOrDefault(false)

    companion object {
        fun databaseFile(context: Context) = File(context.applicationContext.noBackupFilesDir, "quran.db")
    }
}
