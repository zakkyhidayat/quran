package io.zakkyhidayat.quran.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File

object ContentDatabase {
    private const val ASSET = "quran.db"

    @Volatile
    private var db: SQLiteDatabase? = null

    fun get(context: Context): SQLiteDatabase = db ?: synchronized(this) {
        db ?: open(context.applicationContext).also { db = it }
    }

    // Nama file memuat ukuran asset supaya data baru otomatis disalin ulang.
    private fun open(context: Context): SQLiteDatabase {
        val size = context.assets.openFd(ASSET).use { it.declaredLength }
        val file = File(context.noBackupFilesDir, "quran-$size.db")
        if (!file.exists()) {
            val tmp = File(file.parentFile, "${file.name}.tmp")
            context.assets.open(ASSET).use { input -> tmp.outputStream().use { input.copyTo(it) } }
            check(tmp.renameTo(file)) { "Gagal menyiapkan database" }
            file.parentFile?.listFiles { f -> f.name.startsWith("quran-") && f != file }?.forEach { it.delete() }
        }
        return SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
    }
}
