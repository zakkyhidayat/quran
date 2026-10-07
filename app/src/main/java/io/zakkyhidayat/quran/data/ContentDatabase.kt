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

    // Nama file memuat waktu pembaruan aplikasi, jadi database baru otomatis disalin ulang setelah update.
    private fun open(context: Context): SQLiteDatabase {
        val stamp = context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime
        val file = File(context.noBackupFilesDir, "quran-$stamp.db")
        if (!file.exists()) {
            val tmp = File(file.parentFile, "${file.name}.tmp")
            context.assets.open(ASSET).use { input -> tmp.outputStream().use { input.copyTo(it) } }
            check(tmp.renameTo(file)) { "Gagal menyiapkan database" }
            file.parentFile?.listFiles { f -> f.name.startsWith("quran-") && f != file }?.forEach { it.delete() }
        }
        return SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
    }
}
