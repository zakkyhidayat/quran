package io.zakkyhidayat.quran.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import io.zakkyhidayat.quran.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream

data class QuranDatabaseDownloadProgress(val downloaded: Long, val total: Long)
data class QuranDataCheck(
    val current: Boolean,
    val latestVersion: Int,
    val downloadBytes: Long,
)

private data class QuranDatabaseAsset(val file: String, val bytes: Long, val sha256: String)
private data class QuranFontAsset(val file: String, val bytes: Long, val sha256: String)

private val pageFontName = Regex("p(?:[1-9]|[1-9][0-9]|[1-5][0-9]{2}|60[0-4])\\.ttf")

private data class QuranDatabaseCatalog(
    val databaseVersion: Int,
    val database: QuranDatabaseAsset,
    val fonts: QuranFontAsset,
)

/**
 * Checks the installed database and fonts against the latest GitHub Release, downloading and atomically replacing them
 * only when the local installation is missing, damaged, or out of date.
 */
class QuranDatabaseInstaller(context: Context) {
    private val appContext = context.applicationContext
    private val target = databaseFile(appContext)
    private val fontsTarget = fontsDirectory(appContext)
    private val installManifest = File(appContext.noBackupFilesDir, "quran-data-install.json")
    val progress = MutableStateFlow<QuranDatabaseDownloadProgress?>(null)

    suspend fun hasInstalledDatabase(): Boolean = withContext(Dispatchers.IO) {
        if (target.isFile && isValidDatabase(target) && hasInstalledFonts()) return@withContext true

        val legacyFiles = target.parentFile
            ?.listFiles { file -> file.isFile && file.name.startsWith("quran-") && file.name.endsWith(".db") }
            .orEmpty()
            .sortedByDescending { it.lastModified() }
        for (legacy in legacyFiles) {
            if (!isValidDatabase(legacy) || !hasInstalledFonts()) continue
            val temp = File(target.parentFile, "${target.name}.migration")
            if (legacy.copyTo(temp, overwrite = true).renameTo(target)) return@withContext true
            temp.delete()
            throw IOException("Gagal memindahkan database yang sudah terpasang")
        }
        false
    }

    suspend fun checkForUpdate(): QuranDataCheck = withContext(Dispatchers.IO) {
        val metadata = catalog()
        val asset = metadata.database
        QuranDataCheck(
            current = isCurrentInstall(metadata, asset),
            latestVersion = metadata.databaseVersion,
            downloadBytes = asset.bytes + metadata.fonts.bytes,
        )
    }

    suspend fun install(): Boolean = withContext(Dispatchers.IO) {
        progress.value = null
        val metadata = catalog()
        val asset = metadata.database
        if (isCurrentInstall(metadata, asset)) return@withContext false
        val archive = File(target.parentFile, "${target.name}.gz.tmp")
        val database = File(target.parentFile, "${target.name}.tmp")
        val backup = File(target.parentFile, "${target.name}.old")
        val fontsArchive = File(target.parentFile, "quran-fonts.zip.tmp")
        val fontsStage = File(target.parentFile, "quran-fonts.stage")
        val fontsBackup = File(target.parentFile, "quran-fonts.old")
        val downloadTotal = asset.bytes + metadata.fonts.bytes
        try {
            download(asset.file, asset.bytes, asset.sha256, archive, 0L, downloadTotal)
            download(metadata.fonts.file, metadata.fonts.bytes, metadata.fonts.sha256, fontsArchive, asset.bytes, downloadTotal)

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

            extractFonts(fontsArchive, fontsStage)
            if (!hasInstalledFonts(fontsStage)) throw IOException("Paket font mushaf tidak lengkap")

            fontsBackup.deleteRecursively()
            if (fontsTarget.exists() && !fontsTarget.renameTo(fontsBackup)) {
                throw IOException("Gagal mengganti font mushaf")
            }
            if (!fontsStage.renameTo(fontsTarget)) {
                if (fontsBackup.exists()) fontsBackup.renameTo(fontsTarget)
                throw IOException("Gagal menyimpan font mushaf")
            }

            backup.delete()
            if (target.exists() && !target.renameTo(backup)) {
                fontsTarget.deleteRecursively()
                if (fontsBackup.exists()) fontsBackup.renameTo(fontsTarget)
                throw IOException("Gagal mengganti database lama")
            }
            if (!database.renameTo(target)) {
                if (backup.exists()) backup.renameTo(target)
                fontsTarget.deleteRecursively()
                if (fontsBackup.exists()) fontsBackup.renameTo(fontsTarget)
                throw IOException("Gagal menyimpan database")
            }
            backup.delete()
            fontsBackup.deleteRecursively()
            writeInstallManifest(metadata, asset)
            true
        } finally {
            archive.delete()
            database.delete()
            fontsArchive.delete()
            fontsStage.deleteRecursively()
            progress.value = null
        }
    }

    private suspend fun isCurrentInstall(metadata: QuranDatabaseCatalog, asset: QuranDatabaseAsset): Boolean {
        if (!installManifest.isFile) {
            val legacyInstallIsCurrent =
                isValidDatabase(target, metadata.databaseVersion) && hasInstalledFonts()
            if (!legacyInstallIsCurrent) return false
            writeInstallManifest(metadata, asset)
            return true
        }
        val recorded = try {
            JSONObject(installManifest.readText())
        } catch (_: JSONException) {
            return false
        }
        val matchesRelease =
            recorded.optInt("database_version") == metadata.databaseVersion &&
                recorded.optString("database_sha256").equals(asset.sha256, ignoreCase = true) &&
                recorded.optString("fonts_sha256").equals(metadata.fonts.sha256, ignoreCase = true)
        if (!matchesRelease || !isValidDatabase(target, metadata.databaseVersion) || !hasInstalledFonts()) return false
        if (!recorded.optString("installed_database_sha256").equals(sha256(target), ignoreCase = true)) return false
        val expectedFonts = recorded.optJSONObject("font_files") ?: return false
        val fontFiles = fontsTarget.listFiles()
            ?.filter { it.isFile && pageFontName.matches(it.name) }
            ?.sortedBy { it.name }
            .orEmpty()
        if (fontFiles.size != 604) return false
        for (file in fontFiles) {
            currentCoroutineContext().ensureActive()
            if (!expectedFonts.optString(file.name).equals(sha256(file), ignoreCase = true)) return false
        }
        return true
    }

    private suspend fun writeInstallManifest(metadata: QuranDatabaseCatalog, asset: QuranDatabaseAsset) {
        val temp = File(installManifest.parentFile, "${installManifest.name}.tmp")
        val backup = File(installManifest.parentFile, "${installManifest.name}.old")
        try {
            val fontHashes = JSONObject()
            fontsTarget.listFiles()
                ?.filter { it.isFile && pageFontName.matches(it.name) }
                ?.sortedBy { it.name }
                .orEmpty()
                .forEach { file ->
                    currentCoroutineContext().ensureActive()
                    fontHashes.put(file.name, sha256(file))
                }
            temp.writeText(
                JSONObject()
                    .put("database_version", metadata.databaseVersion)
                    .put("database_sha256", asset.sha256)
                    .put("fonts_sha256", metadata.fonts.sha256)
                    .put("installed_database_sha256", sha256(target))
                    .put("font_files", fontHashes)
                    .toString(),
            )
            if (backup.exists() && !backup.delete()) throw IOException("Gagal membersihkan versi data sementara")
            if (installManifest.exists() && !installManifest.renameTo(backup)) {
                throw IOException("Gagal mengganti versi data mushaf")
            }
            if (!temp.renameTo(installManifest)) {
                if (backup.exists()) backup.renameTo(installManifest)
                throw IOException("Gagal menyimpan versi data mushaf")
            }
            backup.delete()
        } finally {
            temp.delete()
        }
    }

    private suspend fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                currentCoroutineContext().ensureActive()
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private suspend fun download(file: String, bytes: Long, sha256: String, destination: File, offset: Long, total: Long) {
        val digest = MessageDigest.getInstance("SHA-256")
        val conn = openHttpsDownload("${BuildConfig.QURAN_DATA_CATALOG_URL.substringBeforeLast('/')}/$file")
        try {
            var downloaded = 0L
            conn.inputStream.use { input ->
                destination.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        digest.update(buffer, 0, count)
                        downloaded += count
                        progress.value = QuranDatabaseDownloadProgress(offset + downloaded, total)
                    }
                }
            }
            if (downloaded != bytes) throw IOException("Ukuran berkas data mushaf tidak cocok")
        } finally {
            conn.disconnect()
        }
        val hash = digest.digest().joinToString("") { "%02x".format(it) }
        if (!hash.equals(sha256, ignoreCase = true)) throw IOException("Checksum data mushaf tidak cocok")
    }

    private suspend fun extractFonts(archive: File, destination: File) {
        destination.deleteRecursively()
        if (!destination.mkdirs()) throw IOException("Gagal menyiapkan folder font mushaf")
        val seen = HashSet<String>()
        ZipInputStream(archive.inputStream().buffered()).use { zip ->
            while (true) {
                currentCoroutineContext().ensureActive()
                val entry = zip.nextEntry ?: break
                val name = entry.name
                if (entry.isDirectory || !pageFontName.matches(name) || !seen.add(name)) {
                    throw IOException("Isi paket font mushaf tidak valid")
                }
                val output = File(destination, name)
                output.outputStream().use { stream ->
                    val buffer = ByteArray(32 * 1024)
                    var fontBytes = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = zip.read(buffer)
                        if (count < 0) break
                        fontBytes += count
                        if (fontBytes > MAX_PAGE_FONT_BYTES) throw IOException("Berkas font mushaf terlalu besar")
                        stream.write(buffer, 0, count)
                    }
                }
                if (output.length() == 0L) throw IOException("Berkas font mushaf kosong")
                zip.closeEntry()
            }
        }
        if (seen.size != 604) throw IOException("Paket font mushaf tidak lengkap")
    }

    private fun hasInstalledFonts(directory: File = fontsTarget): Boolean =
        directory.listFiles()?.count { file ->
            file.isFile && file.length() in 1..MAX_PAGE_FONT_BYTES && pageFontName.matches(file.name)
        } == 604

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
            val item = databases.getJSONObject("full")
            val name = item.getString("file")
            if (!name.matches(Regex("[A-Za-z0-9._-]+\\.db\\.gz"))) throw IOException("Nama berkas database tidak valid")
            val bytes = item.getLong("bytes")
            val sha256 = item.getString("sha256")
            if (bytes <= 0 || !sha256.matches(Regex("[a-fA-F0-9]{64}"))) {
                throw IOException("Metadata database tidak valid")
            }
            val asset = QuranDatabaseAsset(name, bytes, sha256)
            val fontEntry = json.getJSONObject("fonts")
            val fontName = fontEntry.getString("file")
            val fontBytes = fontEntry.getLong("bytes")
            val fontSha256 = fontEntry.getString("sha256")
            if (fontName != "page-fonts.zip" || fontBytes <= 0 || !fontSha256.matches(Regex("[a-fA-F0-9]{64}"))) {
                throw IOException("Metadata font mushaf tidak valid")
            }
            if (version <= 0) throw IOException("Versi database tidak valid")
            return QuranDatabaseCatalog(version, asset, QuranFontAsset(fontName, fontBytes, fontSha256))
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw IOException("Katalog database tidak valid: ${e.message}", e)
        }
    }

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
        private const val MAX_PAGE_FONT_BYTES = 1024L * 1024

        fun databaseFile(context: Context) = File(context.applicationContext.noBackupFilesDir, "quran.db")
        fun fontsDirectory(context: Context) = File(context.applicationContext.noBackupFilesDir, "quran-fonts")
    }
}
