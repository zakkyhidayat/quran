package io.zakkyhidayat.quran.data

import android.content.Context
import android.net.Uri
import io.zakkyhidayat.quran.BuildConfig
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
 *
 * Format v2 netral edisi, supaya Quran dan Quran Lite bisa berbagi satu berkas: bookmark, penanda hapus, dan posisi
 * baca bersama; pengaturan disimpan per profil (`full`/`lite`) dan hanya diterapkan oleh edisi pemiliknya. Saat
 * mengekspor, isi berkas yang ada dibaca dulu sehingga profil edisi lain dan penanda hapus tidak hilang.
 * Format v1 (bookmark + pengaturan Full) tetap bisa dibaca.
 */
object Backup {
    private const val FORMAT = "io.zakkyhidayat.quran.backup"
    private const val VERSION = 2
    private val PROFILE = if (BuildConfig.LITE) "lite" else "full"
    private val POSITION_KEYS = listOf("last_page", "last_surah", "last_ayah")

    data class Result(val bookmarksAdded: Int, val bookmarksRemoved: Int = 0)

    private fun read(context: Context, uri: Uri): JSONObject? {
        val text = runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()
        if (text.isNullOrBlank()) return null
        return runCatching { JSONObject(text) }.getOrNull()?.takeIf { it.optString("format") == FORMAT }
    }

    private fun bookmarksOf(json: JSONObject): List<Bookmark> {
        val list = json.optJSONArray("bookmarks") ?: JSONArray()
        return List(list.length()) { i ->
            val o = list.getJSONObject(i)
            Bookmark(0, BookmarkKind.valueOf(o.getString("kind")), o.getInt("page"), o.getInt("surah"), o.getInt("ayah"), o.optLong("createdAt"))
        }
    }

    private fun deletionsOf(json: JSONObject): List<BookmarkStore.Deletion> {
        val list = json.optJSONArray("deleted") ?: return emptyList()
        return List(list.length()) { i ->
            val o = list.getJSONObject(i)
            BookmarkStore.Deletion(BookmarkKind.valueOf(o.getString("kind")), o.getInt("page"), o.getInt("surah"), o.getInt("ayah"), o.optLong("deletedAt"))
        }
    }

    suspend fun export(context: Context, uri: Uri, settings: SettingsRepository, bookmarks: BookmarkStore) = withContext(Dispatchers.IO) {
        // Gabungkan dulu isi berkas yang sudah ada, agar data edisi lain tidak tertimpa.
        val existing = read(context, uri)?.takeIf { it.optInt("version") <= VERSION }
        if (existing != null) mergeInto(existing, bookmarks)

        val mine = settings.exportAll()
        val profiles = JSONObject()
        existing?.let { old ->
            if (old.optInt("version") >= 2) {
                old.optJSONObject("profiles")?.let { p -> p.keys().forEach { if (it != PROFILE) profiles.put(it, p.get(it)) } }
            } else if (PROFILE != "full") {
                // Pengaturan v1 berasal dari Full; simpan sebagai profilnya.
                old.optJSONObject("settings")?.let { profiles.put("full", JSONObject().put("settings", it)) }
            }
        }
        profiles.put(PROFILE, JSONObject().put("settings", JSONObject().also { obj ->
            mine.filterKeys { it !in POSITION_KEYS }.forEach { (key, value) -> obj.put(key, value) }
        }))

        val json = JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("exportedAt", System.currentTimeMillis())
            .put("app", PROFILE)
            .put("position", JSONObject().also { obj -> POSITION_KEYS.forEach { k -> mine[k]?.let { obj.put(k, it) } } })
            .put("profiles", profiles)
            .put("bookmarks", JSONArray().also { arr ->
                bookmarks.bookmarks.value.forEach { b ->
                    arr.put(
                        JSONObject().put("kind", b.kind.name).put("page", b.page).put("surah", b.surah)
                            .put("ayah", b.ayah).put("createdAt", b.createdAt),
                    )
                }
            })
            .put("deleted", JSONArray().also { arr ->
                bookmarks.deletions().forEach { d ->
                    arr.put(
                        JSONObject().put("kind", d.kind.name).put("page", d.page).put("surah", d.surah)
                            .put("ayah", d.ayah).put("deletedAt", d.at),
                    )
                }
            })
        val out = context.contentResolver.openOutputStream(uri, "wt") ?: throw IOException("Tidak bisa menulis berkas")
        out.bufferedWriter().use { it.write(json.toString(2)) }
    }

    private suspend fun mergeInto(json: JSONObject, bookmarks: BookmarkStore) =
        runCatching { bookmarks.merge(bookmarksOf(json), deletionsOf(json)) }

    /**
     * Bookmark digabung (yang terbaru menang, termasuk penghapusan). Pengaturan diganti dengan profil edisi ini; berkas
     * v1 hanya menerapkan pengaturannya di Full. Posisi baca ikut diterapkan bila ada.
     */
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

        val merged = bookmarks.merge(bookmarksOf(json), deletionsOf(json), respectLocalDeletions = false)
        val values = mutableMapOf<String, Any?>()
        val v2 = json.optInt("version") >= 2
        val own = if (v2) json.optJSONObject("profiles")?.optJSONObject(PROFILE)?.optJSONObject("settings")
        else if (!BuildConfig.LITE) json.optJSONObject("settings") else null
        own?.let { obj -> obj.keys().forEach { values[it] = obj.get(it) } }
        val position = if (v2) json.optJSONObject("position") else json.optJSONObject("settings")
        position?.let { obj -> POSITION_KEYS.forEach { k -> if (obj.has(k)) values[k] = obj.get(k) } }
        if (own != null) {
            settings.importAll(values)
        } else if (position != null) {
            settings.mergeValues(values.filterKeys { it in POSITION_KEYS })
        }
        Result(merged.added, merged.removed)
    }

    /** Sinkron dua arah dengan berkas bersama: gabungkan isinya ke bookmark lokal tanpa mengubah pengaturan. */
    suspend fun syncFrom(context: Context, uri: Uri, bookmarks: BookmarkStore) = withContext(Dispatchers.IO) {
        read(context, uri)?.takeIf { it.optInt("version") <= VERSION }?.let { mergeInto(it, bookmarks) }
        Unit
    }
}
