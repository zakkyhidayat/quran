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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class ThemeMode { System, Light, Dark }

enum class ColorMode { Original, Dynamic }

enum class ContrastLevel { Standard, Medium, High }

/** Satuan bagian harian untuk pengingat membaca. */
enum class ReminderUnit { Juz, Hizb, Manzil }

/** Cara baca, berlaku untuk semua surah: halaman mushaf, ayat dengan terjemahan, atau terjemahan saja. */
enum class ReadingMode { Mushaf, AyahTranslation, Translation }

/** Isi penghitung di bilah atas pembaca; berganti tiap diketuk. */
enum class CounterMode { Surah, Juz, Hizb, Rub, Manzil, Ruku }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val colorMode: ColorMode = ColorMode.Dynamic,
    val amoled: Boolean = false,
    val contrast: ContrastLevel = ContrastLevel.Standard,
    val tajweed: Boolean = true,
    val showTransliteration: Boolean = true,
    val translationIds: List<String> = listOf("id-kemenag"),
    val readingMode: ReadingMode = ReadingMode.Mushaf,
    /** Ukuran teks Arab (daftar ayat, lembar ayat) dalam persen dari ukuran bawaan. Halaman mushaf tidak terpengaruh. */
    val arabicTextPercent: Int = 100,
    /** Ukuran teks terjemahan dalam persen dari ukuran bawaan. */
    val translationTextPercent: Int = 100,
    val onboardingDone: Boolean = false,
    val gestureHintDone: Boolean = false,
    /** Berkas tujuan cadangan otomatis (URI SAF); null = mati. */
    val autoBackupUri: String? = null,
    /** Waktu cadangan otomatis terakhir berhasil (ms); -1 = terakhir gagal; 0 = belum pernah. */
    val autoBackupAt: Long = 0,
    val reminderEnabled: Boolean = false,
    /** Jam pengingat dalam menit sejak tengah malam (bawaan 20.00). */
    val reminderMinutes: Int = 20 * 60,
    val reminderUnit: ReminderUnit = ReminderUnit.Juz,
    val counterMode: CounterMode = CounterMode.Surah,
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
    private val arabicTextKey = intPreferencesKey("arabic_text_percent")
    private val translationTextKey = intPreferencesKey("translation_text_percent")
    private val onboardingKey = booleanPreferencesKey("onboarding_done")
    private val gestureHintKey = booleanPreferencesKey("gesture_hint_done")
    private val autoBackupUriKey = stringPreferencesKey("auto_backup_uri")
    private val autoBackupAtKey = stringPreferencesKey("auto_backup_at")
    private val reminderKey = booleanPreferencesKey("reminder_enabled")
    private val reminderMinutesKey = intPreferencesKey("reminder_minutes")
    private val reminderUnitKey = stringPreferencesKey("reminder_unit")
    private val counterModeKey = stringPreferencesKey("counter_mode")

    // Khusus perangkat ini: tidak ikut diekspor dan tidak ditimpa saat memulihkan cadangan.
    private val deviceOnlyKeys = setOf(autoBackupUriKey.name, autoBackupAtKey.name)
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
            arabicTextPercent = prefs[arabicTextKey] ?: 100,
            translationTextPercent = prefs[translationTextKey] ?: 100,
            autoBackupUri = prefs[autoBackupUriKey],
            autoBackupAt = prefs[autoBackupAtKey]?.toLongOrNull() ?: 0,
            reminderEnabled = prefs[reminderKey] ?: false,
            reminderMinutes = prefs[reminderMinutesKey] ?: (20 * 60),
            reminderUnit = prefs[reminderUnitKey].toEnum(ReminderUnit.Juz),
            // Pengguna lama (sudah pernah membaca) tidak perlu onboarding maupun petunjuk gerakan.
            onboardingDone = prefs[onboardingKey] ?: (prefs[lastPageKey] != null),
            // Pengguna baru: onboarding_done tersimpan, jadi petunjuk tetap tampil walau baca terakhir sudah tercatat.
            gestureHintDone = prefs[gestureHintKey] ?: (prefs[onboardingKey] == null && prefs[lastPageKey] != null),
            counterMode = prefs[counterModeKey].toEnum(CounterMode.Surah),
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

    /** Semua pengaturan tersimpan (untuk cadangan), dengan nilai Boolean/Int/String apa adanya. */
    suspend fun exportAll(): Map<String, Any> =
        context.dataStore.data.first().asMap().mapKeys { it.key.name }.filterKeys { it !in deviceOnlyKeys }

    /** Ganti semua pengaturan dengan isi cadangan; tipe ditentukan dari nilainya. */
    suspend fun importAll(values: Map<String, Any?>) = context.dataStore.edit { prefs ->
        val kept = prefs.asMap().filterKeys { it.name in deviceOnlyKeys }
        prefs.clear()
        kept.forEach { (key, value) -> if (value is String) prefs[stringPreferencesKey(key.name)] = value }
        values.filterKeys { it !in deviceOnlyKeys }.forEach { (name, value) ->
            when (value) {
                is Boolean -> prefs[booleanPreferencesKey(name)] = value
                is Int -> prefs[intPreferencesKey(name)] = value
                is String -> prefs[stringPreferencesKey(name)] = value
                else -> Unit
            }
        }
    }

    suspend fun setAutoBackup(uri: String?) = context.dataStore.edit {
        if (uri == null) it.remove(autoBackupUriKey) else it[autoBackupUriKey] = uri
        it[autoBackupAtKey] = "0"
    }

    suspend fun setAutoBackupAt(time: Long) = context.dataStore.edit { it[autoBackupAtKey] = time.toString() }

    suspend fun setReminder(enabled: Boolean) = context.dataStore.edit { it[reminderKey] = enabled }

    suspend fun setReminderMinutes(minutes: Int) = context.dataStore.edit { it[reminderMinutesKey] = minutes }

    suspend fun setReminderUnit(unit: ReminderUnit) = context.dataStore.edit { it[reminderUnitKey] = unit.name }

    suspend fun setOnboardingDone() = context.dataStore.edit { it[onboardingKey] = true }

    suspend fun setGestureHintDone() = context.dataStore.edit { it[gestureHintKey] = true }

    suspend fun setArabicTextPercent(value: Int) = context.dataStore.edit { it[arabicTextKey] = value }

    suspend fun setTranslationTextPercent(value: Int) = context.dataStore.edit { it[translationTextKey] = value }

    suspend fun setReadingMode(value: ReadingMode) = context.dataStore.edit { it[readingModeKey] = value.name }

    suspend fun setCounterMode(value: CounterMode) = context.dataStore.edit { it[counterModeKey] = value.name }

    suspend fun setLastPage(page: Int) = context.dataStore.edit { it[lastPageKey] = page }

    /** Halaman dan ayat terakhir dalam satu penulisan (satu emisi, bukan dua). */
    suspend fun setPosition(page: Int, surah: Int, ayah: Int) = context.dataStore.edit {
        it[lastPageKey] = page
        it[lastSurahKey] = surah
        it[lastAyahKey] = ayah
    }

    suspend fun setLastAyah(surah: Int, ayah: Int) = context.dataStore.edit {
        it[lastSurahKey] = surah
        it[lastAyahKey] = ayah
    }
}

private inline fun <reified T : Enum<T>> String?.toEnum(default: T): T =
    enumValues<T>().firstOrNull { it.name == this } ?: default
