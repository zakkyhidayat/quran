package io.zakkyhidayat.quran.data

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.content.FileProvider
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

/** Rilis aplikasi yang lebih baru di GitHub Releases. */
data class UpdateInfo(val version: String, val notes: String, val apkUrl: String, val bytes: Long, val pageUrl: String)

/**
 * Pembaruan untuk build dari GitHub Releases: cek rilis terbaru (bukan pre-release, jadi rilis `translations` tidak
 * terhitung), unduh APK-nya ke cache, lalu serahkan ke pemasang paket Android. Tidak dipakai di build Play Store
 * (BuildConfig.UPDATER_ENABLED = false).
 */
object Updater {
    private val latestUrl get() = "https://api.github.com/repos/${BuildConfig.GITHUB_REPO}/releases/latest"

    /** Null bila tidak ada versi yang lebih baru (atau belum ada rilis). Melempar IOException bila gagal terhubung. */
    suspend fun check(): UpdateInfo? = withContext(Dispatchers.IO) {
        val conn = (URL(latestUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/vnd.github+json")
        }
        try {
            when (conn.responseCode) {
                HttpURLConnection.HTTP_NOT_FOUND -> return@withContext null // belum ada rilis
                HttpURLConnection.HTTP_OK -> Unit
                else -> throw IOException("GitHub menjawab ${conn.responseCode}")
            }
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val version = json.getString("tag_name").removePrefix("v")
            if (!isNewer(version, BuildConfig.VERSION_NAME)) return@withContext null
            val assets = json.getJSONArray("assets")
            val apk = (0 until assets.length()).map { assets.getJSONObject(it) }
                .firstOrNull { it.getString("name").endsWith(".apk") } ?: return@withContext null
            UpdateInfo(
                version = version,
                notes = json.optString("body"),
                apkUrl = apk.getString("browser_download_url"),
                bytes = apk.optLong("size"),
                pageUrl = json.optString("html_url"),
            )
        } finally {
            conn.disconnect()
        }
    }

    /** Bandingkan semver X.Y.Z (sufiks setelah '-' diabaikan). */
    fun isNewer(candidate: String, current: String): Boolean {
        fun parts(v: String) = v.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 } + listOf(0, 0, 0)
        val a = parts(candidate)
        val b = parts(current)
        for (i in 0 until 3) if (a[i] != b[i]) return a[i] > b[i]
        return false
    }

    /** Unduh APK ke cache/updates; GitHub mengalihkan aset rilis ke host lain (https ke https). */
    suspend fun download(context: Context, info: UpdateInfo, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() } // APK lama dari pembaruan sebelumnya
        val target = File(dir, "quran-${info.version}.apk")
        val conn = (URL(info.apkUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
        }
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) throw IOException("Unduhan gagal (${conn.responseCode})")
            val total = conn.contentLengthLong.takeIf { it > 0 } ?: info.bytes
            var done = 0L
            conn.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        done += n
                        if (total > 0) onProgress(done.toFloat() / total)
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
        target
    }

    /** Android 8+: pengguna harus mengizinkan aplikasi ini memasang aplikasi. */
    fun canInstall(context: Context): Boolean = context.packageManager.canRequestPackageInstalls()

    fun openInstallPermissionSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, android.net.Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    /** Buka pemasang paket sistem untuk APK yang sudah diunduh. */
    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", apk)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
