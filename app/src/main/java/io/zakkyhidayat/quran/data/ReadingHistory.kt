package io.zakkyhidayat.quran.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

data class HistoryEntry(val surah: Int, val ayah: Int, val readAt: Long)

/** Histori bacaan: satu baris per surah (ayat terakhir yang dibaca di surah itu), paling baru di atas. */
class ReadingHistory(context: Context) : SQLiteOpenHelper(context, "history.db", null, 1) {
    private val state = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val entries: StateFlow<List<HistoryEntry>> = state

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE history (surah INTEGER PRIMARY KEY, ayah INTEGER NOT NULL, read_at INTEGER NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    suspend fun load() = withContext(Dispatchers.IO) { state.value = query() }

    suspend fun record(surah: Int, ayah: Int) = withContext(Dispatchers.IO) {
        if (surah !in 1..114 || ayah < 1) return@withContext
        writableDatabase.insertWithOnConflict(
            "history",
            null,
            ContentValues().apply {
                put("surah", surah)
                put("ayah", ayah)
                put("read_at", System.currentTimeMillis())
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
        writableDatabase.execSQL("DELETE FROM history WHERE surah NOT IN (SELECT surah FROM history ORDER BY read_at DESC LIMIT $LIMIT)")
        state.value = query()
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        writableDatabase.delete("history", null, null)
        state.value = emptyList()
    }

    private fun query(): List<HistoryEntry> {
        val list = mutableListOf<HistoryEntry>()
        readableDatabase.rawQuery("SELECT surah, ayah, read_at FROM history ORDER BY read_at DESC", null).use { c ->
            while (c.moveToNext()) list += HistoryEntry(c.getInt(0), c.getInt(1), c.getLong(2))
        }
        return list
    }

    companion object {
        const val LIMIT = 30
    }
}
