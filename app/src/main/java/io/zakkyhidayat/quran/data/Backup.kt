package io.zakkyhidayat.quran.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
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
 * Format v2 menyimpan bookmark, penanda hapus, posisi baca, dan profil pengaturan. Saat mengekspor, isi berkas yang ada
 * dibaca dulu sehingga profil lain yang mungkin tersimpan di cadangan lama tidak hilang.
 * Format v1 (bookmark + pengaturan Full) tetap bisa dibaca.
 */
object Backup {
    private const val FORMAT = "io.zakkyhidayat.quran.backup"
    private const val VERSION = 2
    private const val PROFILE = "full"
    private val POSITION_KEYS = listOf("last_page", "last_surah", "last_ayah")
    private const val AUTO_PREFIX = "quran-autobackup-"
    private const val AUTO_SUFFIX = ".json"
    private const val MAX_AUTO_BACKUPS = 3

    private data class AutoBackupDocument(val uri: Uri, val name: String, val modified: Long)

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

    /**
     * Simpan snapshot baru ke folder SAF, gabungkan dahulu semua snapshot yang ada, lalu pertahankan maksimal tiga
     * berkas terbaru. Folder dapat dipilih pengguna dari penyedia dokumen lokal atau awan.
     */
    suspend fun exportToFolder(context: Context, treeUri: Uri, settings: SettingsRepository, bookmarks: BookmarkStore) =
        withContext(Dispatchers.IO) {
            if (!DocumentsContract.isTreeUri(treeUri)) throw IOException("URI cadangan otomatis bukan folder")

            val snapshots = listAutoBackups(context, treeUri)
            var latest: JSONObject? = null
            snapshots.forEach { document ->
                val json = readSnapshot(context, document.uri)
                bookmarks.merge(bookmarksOf(json), deletionsOf(json))
                latest = json
            }

            val payload = buildPayload(latest, settings, bookmarks)
            val parentId = DocumentsContract.getTreeDocumentId(treeUri)
            val parent = DocumentsContract.buildDocumentUriUsingTree(treeUri, parentId)
            val timestamp = System.currentTimeMillis()
            val name = "$AUTO_PREFIX$timestamp$AUTO_SUFFIX"
            val documentUri = DocumentsContract.createDocument(
                context.contentResolver,
                parent,
                "application/json",
                name,
            ) ?: throw IOException("Penyedia dokumen tidak dapat membuat berkas cadangan")
            try {
                val out = context.contentResolver.openOutputStream(documentUri, "wt")
                    ?: throw IOException("Tidak bisa menulis berkas cadangan")
                out.bufferedWriter().use { it.write(payload.toString(2)) }
            } catch (e: Exception) {
                DocumentsContract.deleteDocument(context.contentResolver, documentUri)
                throw e
            }

            val current = listAutoBackups(context, treeUri)
            val toDelete = autoBackupNamesToDelete(current.map { it.name to it.modified }).toSet()
            current.filter { it.name in toDelete }.forEach { old ->
                if (!DocumentsContract.deleteDocument(context.contentResolver, old.uri)) {
                    throw IOException("Tidak bisa menghapus cadangan lama: ${old.name}")
                }
            }
        }

    internal fun autoBackupNamesToDelete(entries: List<Pair<String, Long>>): List<String> =
        entries.sortedWith(compareByDescending<Pair<String, Long>> { it.second }.thenByDescending { it.first })
            .drop(MAX_AUTO_BACKUPS)
            .map { it.first }

    private suspend fun buildPayload(
        existing: JSONObject?,
        settings: SettingsRepository,
        bookmarks: BookmarkStore,
    ): JSONObject {
        val mine = settings.exportAll()
        val profiles = JSONObject()
        existing?.let { old ->
            if (old.optInt("version") >= 2) {
                old.optJSONObject("profiles")?.let { p -> p.keys().forEach { if (it != PROFILE) profiles.put(it, p.get(it)) } }
            }
        }
        profiles.put(PROFILE, JSONObject().put("settings", JSONObject().also { obj ->
            mine.filterKeys { it !in POSITION_KEYS }.forEach { (key, value) -> obj.put(key, value) }
        }))

        return JSONObject()
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
    }

    private fun listAutoBackups(context: Context, treeUri: Uri): List<AutoBackupDocument> {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri),
        )
        val documents = mutableListOf<AutoBackupDocument>()
        context.contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val modifiedColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameColumn) ?: continue
                if (name.startsWith(AUTO_PREFIX) && name.endsWith(AUTO_SUFFIX)) {
                    documents += AutoBackupDocument(
                        DocumentsContract.buildDocumentUriUsingTree(treeUri, cursor.getString(idColumn)),
                        name,
                        cursor.getLong(modifiedColumn),
                    )
                }
            }
        } ?: throw IOException("Tidak bisa membaca isi folder cadangan")
        return documents.sortedWith(compareBy<AutoBackupDocument> { it.modified }.thenBy { it.name })
    }

    private fun readSnapshot(context: Context, uri: Uri): JSONObject {
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: throw IOException("Tidak bisa membaca snapshot cadangan")
        val json = try {
            JSONObject(text)
        } catch (e: Exception) {
            throw IOException("Snapshot cadangan rusak", e)
        }
        if (json.optString("format") != FORMAT || json.optInt("version") > VERSION) {
            throw IOException("Snapshot bukan cadangan yang didukung aplikasi ini")
        }
        return json
    }

    private suspend fun mergeInto(json: JSONObject, bookmarks: BookmarkStore): BookmarkStore.MergeResult =
        bookmarks.merge(bookmarksOf(json), deletionsOf(json))

    /** Bookmark digabung (yang terbaru menang, termasuk penghapusan); pengaturan dan posisi baca dipulihkan bila ada. */
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
        else json.optJSONObject("settings")
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
