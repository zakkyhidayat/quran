package io.zakkyhidayat.quran

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.LocaleList
import android.content.res.Configuration
import java.util.Locale

/** Bahasa antarmuka: "" mengikuti perangkat, selain itu kode bahasa (id/en). Disimpan sinkron agar bisa dibaca sebelum Activity dibuat. */
object AppLanguage {
    val supported = listOf("id", "en")

    private fun prefs(context: Context) = context.getSharedPreferences("language", Context.MODE_PRIVATE)

    fun saved(context: Context): String = prefs(context).getString("code", "").orEmpty()

    fun save(context: Context, code: String) {
        prefs(context).edit().putString("code", code).apply()
    }

    /** Bahasa yang berlaku: pilihan pengguna, atau bahasa perangkat bila didukung, atau Inggris. */
    fun effective(context: Context): String {
        val code = saved(context).ifEmpty { context.resources.configuration.locales[0].language }
        return if (code == "in") "id" else if (code in supported) code else "en"
    }

    /** Simpan pilihan lalu terapkan: Android 13+ lewat LocaleManager (ikut tercatat di pengaturan sistem), versi lama dengan membuat ulang Activity. */
    fun apply(context: Context, code: String) {
        save(context, code)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java).applicationLocales =
                if (code.isEmpty()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(code)
        } else {
            context.findActivity()?.recreate()
        }
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    fun wrap(base: Context): Context {
        val code = saved(base)
        if (code.isEmpty()) return base
        val locale = Locale.forLanguageTag(code)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration).apply { setLocale(locale) }
        return base.createConfigurationContext(config)
    }
}
