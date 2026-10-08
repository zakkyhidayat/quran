package io.zakkyhidayat.quran

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.LocaleList
import android.content.res.Configuration
import java.util.Locale

/** Bahasa antarmuka: "" mengikuti perangkat, selain itu kode bahasa (lihat supported). Disimpan sinkron agar bisa dibaca sebelum Activity dibuat. */
object AppLanguage {
    val supported = listOf("en", "id", "ar", "ur", "bn", "tr", "fa", "ms", "fr", "ru")

    /** Nama tiap bahasa dalam bahasanya sendiri, untuk pemilih bahasa. */
    val endonyms = mapOf(
        "en" to "English", "id" to "Bahasa Indonesia", "ar" to "العربية", "ur" to "اردو", "bn" to "বাংলা",
        "tr" to "Türkçe", "fa" to "فارسی", "ms" to "Bahasa Melayu", "fr" to "Français", "ru" to "Русский",
    )

    private fun prefs(context: Context) = context.getSharedPreferences("language", Context.MODE_PRIVATE)

    // Android 13+: pengaturan bahasa per aplikasi milik sistem jadi satu-satunya sumber, agar pilihan dari Pengaturan
    // Android dan dari aplikasi selalu sama. Versi lama memakai SharedPreferences.
    fun saved(context: Context): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java).applicationLocales.get(0)?.language
                ?.let { if (it == "in") "id" else it }.orEmpty()
        } else {
            prefs(context).getString("code", "").orEmpty()
        }

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java).applicationLocales =
                if (code.isEmpty()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(code)
        } else {
            save(context, code)
            context.findActivity()?.recreate()
        }
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val code = saved(base)
        if (code.isEmpty()) return base
        val locale = Locale.forLanguageTag(code)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration).apply { setLocale(locale) }
        return base.createConfigurationContext(config)
    }
}
