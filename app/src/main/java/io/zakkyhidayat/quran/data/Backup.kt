package io.zakkyhidayat.quran.data

import android.content.Context
import android.net.Uri
import io.zakkyhidayat.quran.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * Cadangan lokal: bookmark dan pengaturan dalam satu berkas JSON. Berkas ditulis/dibaca lewat Storage Access Framework,
 * jadi pengguna bisa menyimpannya di perangkat atau di aplikasi awan yang menyediakan penyedia dokumen (Google Drive,
 * Dropbox, OneDrive) tanpa izin penyimpanan. Terjemahan tidak ikut: yang aktif diunduh ulang otomatis.
 */
object Backup {
    private const val FORMAT = "io.zakkyhidayat.quran.backup"
    private const val VERSION = 1

    data class Result(val bookmarksAdded: Int)

    suspend fun export(context: Context, uri: Uri, settings: SettingsRepository, bookmarks: BookmarkStore) = withContext(Dispatchers.IO) {
        val json = JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("exportedAt", System.currentTimeMillis())
            .put("settings", JSONObject().also { obj ->
                settings.exportAll().forEach { (key, value) -> obj.put(key, value) }
            })
            .put("bookmarks", JSONArray().also { arr ->
                bookmarks.bookmarks.value.forEach { b ->
                    arr.put(
                        JSONObject().put("kind", b.kind.name).put("page", b.page).put("surah", b.surah)
                            .put("ayah", b.ayah).put("createdAt", b.createdAt),
                    )
                }
            })
        val out = context.contentResolver.openOutputStream(uri, "wt") ?: throw IOException("Tidak bisa menulis berkas")
        out.bufferedWriter().use { it.write(json.toString(2)) }
    }

    /** Bookmark digabung (yang sudah ada dilewati); pengaturan diganti dengan isi berkas. */
    suspend fun import(context: Context, uri: Uri, settings: SettingsRepository, bookmarks: BookmarkStore): Result = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: throw IOException("Tidak bisa membaca berkas")
        val json = try {
            JSONObject(text)
        } catch (e: Exception) {
            throw IOException("Bukan berkas cadangan", e)
        }
        if (json.optString("format") != FORMAT) throw IOException("Bukan berkas cadangan aplikasi ini")
        if (json.optInt("version") > VERSION) throw IOException("Cadangan dibuat oleh versi aplikasi yang lebih baru")

        val list = json.optJSONArray("bookmarks") ?: JSONArray()
        val incoming = List(list.length()) { i ->
            val o = list.getJSONObject(i)
            Bookmark(0, BookmarkKind.valueOf(o.getString("kind")), o.getInt("page"), o.getInt("surah"), o.getInt("ayah"), o.optLong("createdAt"))
        }
        val added = bookmarks.importAll(incoming)
        json.optJSONObject("settings")?.let { obj ->
            settings.importAll(obj.keys().asSequence().associateWith { obj.get(it) })
        }
        Result(added)
    }
}
