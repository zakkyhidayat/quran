package io.zakkyhidayat.quran.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class BookmarkStore(context: Context) : SQLiteOpenHelper(context, "user.db", null, 2) {
    private val state = MutableStateFlow<List<Bookmark>>(emptyList())
    val bookmarks: StateFlow<List<Bookmark>> = state

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE bookmark (id INTEGER PRIMARY KEY AUTOINCREMENT, kind TEXT NOT NULL, page INTEGER NOT NULL, " +
                "surah INTEGER NOT NULL, ayah INTEGER NOT NULL, created_at INTEGER NOT NULL, UNIQUE (kind, page, surah, ayah))",
        )
        createTombstones(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) createTombstones(db)
    }

    // Penanda hapus: tanpa ini, bookmark yang dihapus di satu edisi muncul lagi saat cadangan bersama digabung.
    private fun createTombstones(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS bookmark_deleted (kind TEXT NOT NULL, page INTEGER NOT NULL, surah INTEGER NOT NULL, " +
                "ayah INTEGER NOT NULL, deleted_at INTEGER NOT NULL, PRIMARY KEY (kind, page, surah, ayah))",
        )
    }

    suspend fun load() = withContext(Dispatchers.IO) { state.value = query() }

    suspend fun togglePage(page: Int) = toggle(BookmarkKind.Page, page, 0, 0)

    suspend fun toggleAyah(page: Int, surah: Int, ayah: Int) = toggle(BookmarkKind.Ayah, page, surah, ayah)

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.rawQuery("SELECT kind, page, surah, ayah FROM bookmark WHERE id = ?", arrayOf(id.toString())).use { c ->
                if (c.moveToFirst()) putTombstone(db, c.getString(0), c.getInt(1), c.getInt(2), c.getInt(3), System.currentTimeMillis())
            }
            db.delete("bookmark", "id = ?", arrayOf(id.toString()))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        state.value = query()
    }

    private fun putTombstone(db: SQLiteDatabase, kind: String, page: Int, surah: Int, ayah: Int, at: Long) {
        db.execSQL(
            "INSERT INTO bookmark_deleted (kind, page, surah, ayah, deleted_at) VALUES (?, ?, ?, ?, ?) " +
                "ON CONFLICT (kind, page, surah, ayah) DO UPDATE SET deleted_at = MAX(deleted_at, excluded.deleted_at)",
            arrayOf<Any>(kind, page, surah, ayah, at),
        )
    }

    private fun tombstoneAt(db: SQLiteDatabase, kind: String, page: Int, surah: Int, ayah: Int): Long? =
        db.rawQuery(
            "SELECT deleted_at FROM bookmark_deleted WHERE kind = ? AND page = ? AND surah = ? AND ayah = ?",
            arrayOf(kind, page.toString(), surah.toString(), ayah.toString()),
        ).use { if (it.moveToFirst()) it.getLong(0) else null }

    private suspend fun toggle(kind: BookmarkKind, page: Int, surah: Int, ayah: Int) = withContext(Dispatchers.IO) {
        val args = arrayOf(kind.name, page.toString(), surah.toString(), ayah.toString())
        val db = writableDatabase
        val removed = db.delete("bookmark", "kind = ? AND page = ? AND surah = ? AND ayah = ?", args)
        if (removed > 0) {
            putTombstone(db, kind.name, page, surah, ayah, System.currentTimeMillis())
        } else {
            db.delete("bookmark_deleted", "kind = ? AND page = ? AND surah = ? AND ayah = ?", args)
            db.insert(
                "bookmark",
                null,
                ContentValues().apply {
                    put("kind", kind.name)
                    put("page", page)
                    put("surah", surah)
                    put("ayah", ayah)
                    put("created_at", System.currentTimeMillis())
                },
            )
        }
        state.value = query()
    }

    /** Penghapusan bookmark: [at] adalah waktu hapus (ms). */
    data class Deletion(val kind: BookmarkKind, val page: Int, val surah: Int, val ayah: Int, val at: Long)

    /** Bookmark aktif dan penanda hapusnya, untuk cadangan bersama antar edisi. */
    suspend fun deletions(): List<Deletion> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Deletion>()
        readableDatabase.rawQuery("SELECT kind, page, surah, ayah, deleted_at FROM bookmark_deleted", null).use { c ->
            while (c.moveToNext()) list += Deletion(BookmarkKind.valueOf(c.getString(0)), c.getInt(1), c.getInt(2), c.getInt(3), c.getLong(4))
        }
        list
    }

    data class MergeResult(val added: Int, val removed: Int)

    /**
     * Gabungkan bookmark dan penghapusan dari cadangan. Yang terbaru menang: bookmark dihapus bila penanda hapusnya
     * tidak lebih lama dari pembuatannya, dan bookmark dari cadangan dilewati bila sudah pernah dihapus sesudahnya.
     * Pemulihan manual ([respectLocalDeletions] = false) selalu memulihkan bookmark aktif di berkas.
     */
    suspend fun merge(items: List<Bookmark>, deletions: List<Deletion> = emptyList(), respectLocalDeletions: Boolean = true): MergeResult = withContext(Dispatchers.IO) {
        var added = 0
        var removed = 0
        val db = writableDatabase
        db.beginTransaction()
        try {
            deletions.forEach { d ->
                val args = arrayOf(d.kind.name, d.page.toString(), d.surah.toString(), d.ayah.toString())
                val gone = db.delete("bookmark", "kind = ? AND page = ? AND surah = ? AND ayah = ? AND created_at <= ?", args + d.at.toString())
                removed += gone
                val stillThere = db.rawQuery("SELECT 1 FROM bookmark WHERE kind = ? AND page = ? AND surah = ? AND ayah = ?", args).use { it.moveToFirst() }
                if (!stillThere) putTombstone(db, d.kind.name, d.page, d.surah, d.ayah, d.at)
            }
            items.forEach { b ->
                val created = b.createdAt.takeIf { it > 0 } ?: System.currentTimeMillis()
                val deletedAt = if (respectLocalDeletions) tombstoneAt(db, b.kind.name, b.page, b.surah, b.ayah) else null
                if (deletedAt != null && deletedAt >= created) return@forEach
                val id = db.insertWithOnConflict(
                    "bookmark",
                    null,
                    ContentValues().apply {
                        put("kind", b.kind.name)
                        put("page", b.page)
                        put("surah", b.surah)
                        put("ayah", b.ayah)
                        put("created_at", created)
                    },
                    SQLiteDatabase.CONFLICT_IGNORE,
                )
                if (id != -1L) {
                    added++
                    db.delete(
                        "bookmark_deleted", "kind = ? AND page = ? AND surah = ? AND ayah = ?",
                        arrayOf(b.kind.name, b.page.toString(), b.surah.toString(), b.ayah.toString()),
                    )
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        state.value = query()
        MergeResult(added, removed)
    }

    /** Tambahkan bookmark dari cadangan; yang sudah ada (jenis, halaman, surah, ayat sama) dilewati. */
    suspend fun importAll(items: List<Bookmark>): Int = merge(items).added

    private fun query(): List<Bookmark> {
        val list = mutableListOf<Bookmark>()
        readableDatabase.rawQuery(
            "SELECT id, kind, page, surah, ayah, created_at FROM bookmark ORDER BY created_at DESC",
            null,
        ).use { c ->
            while (c.moveToNext()) {
                list += Bookmark(c.getLong(0), BookmarkKind.valueOf(c.getString(1)), c.getInt(2), c.getInt(3), c.getInt(4), c.getLong(5))
            }
        }
        return list
    }
}
