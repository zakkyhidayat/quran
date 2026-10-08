package io.zakkyhidayat.quran.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { System, Light, Dark }

enum class ColorMode { Original, Dynamic }

enum class ContrastLevel { Standard, Medium, High }

/** Cara baca, berlaku untuk semua surah: halaman mushaf, ayat dengan terjemahan, atau terjemahan saja. */
enum class ReadingMode { Mushaf, AyahTranslation, Translation }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val colorMode: ColorMode = ColorMode.Dynamic,
    val amoled: Boolean = false,
    val contrast: ContrastLevel = ContrastLevel.Standard,
    val tajweed: Boolean = true,
    val showTransliteration: Boolean = true,
    val translationIds: List<String> = listOf("id-kemenag"),
    val readingMode: ReadingMode = ReadingMode.Mushaf,
    val lastPage: Int = 1,
    val lastSurah: Int = 0,
    val lastAyah: Int = 0,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme_mode")
    private val colorKey = stringPreferencesKey("color_mode")
    private val amoledKey = booleanPreferencesKey("amoled")
    private val contrastKey = stringPreferencesKey("contrast")
    private val tajweedKey = booleanPreferencesKey("tajweed")
    private val transliterationKey = booleanPreferencesKey("transliteration")
    private val translationsKey = stringPreferencesKey("translations")
    private val readingModeKey = stringPreferencesKey("reading_mode")
    private val lastPageKey = intPreferencesKey("last_page")
    private val lastSurahKey = intPreferencesKey("last_surah")
    private val lastAyahKey = intPreferencesKey("last_ayah")

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[themeKey].toEnum(ThemeMode.System),
            colorMode = prefs[colorKey].toEnum(ColorMode.Dynamic),
            amoled = prefs[amoledKey] ?: false,
            contrast = prefs[contrastKey].toEnum(ContrastLevel.Standard),
            tajweed = prefs[tajweedKey] ?: true,
            showTransliteration = prefs[transliterationKey] ?: true,
            translationIds = prefs[translationsKey]?.split(',')?.filter { it.isNotBlank() } ?: defaultTranslations(),
            readingMode = prefs[readingModeKey].toEnum(ReadingMode.Mushaf),
            lastPage = prefs[lastPageKey] ?: 1,
            lastSurah = prefs[lastSurahKey] ?: 0,
            lastAyah = prefs[lastAyahKey] ?: 0,
        )
    }

    // Terjemahan awal mengikuti bahasa aplikasi: Indonesia memakai Kemenag, Arab tanpa terjemahan, selain itu Inggris.
    // Daftar kosong yang disimpan pengguna tetap kosong (nilai tersimpan "" bukan null, jadi tidak jatuh ke bawaan).
    private fun defaultTranslations() = when (io.zakkyhidayat.quran.AppLanguage.effective(context)) {
        "id" -> listOf("id-kemenag")
        "ar" -> emptyList()
        else -> listOf("en-sahih")
    }

    suspend fun setThemeMode(value: ThemeMode) = context.dataStore.edit { it[themeKey] = value.name }

    suspend fun setColorMode(value: ColorMode) = context.dataStore.edit { it[colorKey] = value.name }

    suspend fun setAmoled(value: Boolean) = context.dataStore.edit { it[amoledKey] = value }

    suspend fun setContrast(value: ContrastLevel) = context.dataStore.edit { it[contrastKey] = value.name }

    suspend fun setTajweed(value: Boolean) = context.dataStore.edit { it[tajweedKey] = value }

    suspend fun setShowTransliteration(value: Boolean) = context.dataStore.edit { it[transliterationKey] = value }

    suspend fun setTranslations(ids: List<String>) = context.dataStore.edit { it[translationsKey] = ids.joinToString(",") }

    suspend fun setReadingMode(value: ReadingMode) = context.dataStore.edit { it[readingModeKey] = value.name }

    suspend fun setLastPage(page: Int) = context.dataStore.edit { it[lastPageKey] = page }

    suspend fun setLastAyah(surah: Int, ayah: Int) = context.dataStore.edit {
        it[lastSurahKey] = surah
        it[lastAyahKey] = ayah
    }
}

private inline fun <reified T : Enum<T>> String?.toEnum(default: T): T =
    enumValues<T>().firstOrNull { it.name == this } ?: default
