package io.zakkyhidayat.quran.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
object ContentDatabase {
    @Volatile
    private var db: SQLiteDatabase? = null

    fun get(context: Context): SQLiteDatabase = db ?: synchronized(this) {
        db ?: open(context.applicationContext).also { db = it }
    }

    private fun open(context: Context): SQLiteDatabase {
        val file = QuranDatabaseInstaller.databaseFile(context)
        check(file.isFile) { "Database Qur’an belum diunduh melalui onboarding" }
        return SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
    }
}
