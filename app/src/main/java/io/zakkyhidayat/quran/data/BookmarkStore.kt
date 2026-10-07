package io.zakkyhidayat.quran.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class BookmarkStore(context: Context) : SQLiteOpenHelper(context, "user.db", null, 1) {
    private val state = MutableStateFlow<List<Bookmark>>(emptyList())
    val bookmarks: StateFlow<List<Bookmark>> = state

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE bookmark (id INTEGER PRIMARY KEY AUTOINCREMENT, kind TEXT NOT NULL, page INTEGER NOT NULL, " +
                "surah INTEGER NOT NULL, ayah INTEGER NOT NULL, created_at INTEGER NOT NULL, UNIQUE (kind, page, surah, ayah))",
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    suspend fun load() = withContext(Dispatchers.IO) { state.value = query() }

    suspend fun togglePage(page: Int) = toggle(BookmarkKind.Page, page, 0, 0)

    suspend fun toggleAyah(page: Int, surah: Int, ayah: Int) = toggle(BookmarkKind.Ayah, page, surah, ayah)

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        writableDatabase.delete("bookmark", "id = ?", arrayOf(id.toString()))
        state.value = query()
    }

    private suspend fun toggle(kind: BookmarkKind, page: Int, surah: Int, ayah: Int) = withContext(Dispatchers.IO) {
        val args = arrayOf(kind.name, page.toString(), surah.toString(), ayah.toString())
        val removed = writableDatabase.delete("bookmark", "kind = ? AND page = ? AND surah = ? AND ayah = ?", args)
        if (removed == 0) {
            writableDatabase.insert(
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
