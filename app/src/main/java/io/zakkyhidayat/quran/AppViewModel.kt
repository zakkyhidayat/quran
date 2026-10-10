package io.zakkyhidayat.quran

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.zakkyhidayat.quran.data.AyahDetail
import io.zakkyhidayat.quran.data.AyahExtras
import io.zakkyhidayat.quran.data.AyahRef
import io.zakkyhidayat.quran.data.CatalogPack
import io.zakkyhidayat.quran.data.Marker
import io.zakkyhidayat.quran.data.MarkerKind
import io.zakkyhidayat.quran.data.PageMeta
import io.zakkyhidayat.quran.data.Surah
import io.zakkyhidayat.quran.data.TranslationInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import io.zakkyhidayat.quran.data.Backup
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.collectLatest
import io.zakkyhidayat.quran.data.UpdateInfo
import io.zakkyhidayat.quran.data.Updater
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as QuranApp
    val mushaf = app.mushaf
    val settingsRepository = app.settings
    val bookmarkStore = app.bookmarks
    val history = app.history
    // Kumpulan ayat yang sedang dibuka di layar "passage" (dari beranda atau koleksi).
    val passage = MutableStateFlow<io.zakkyhidayat.quran.home.HomeTarget.Passage?>(null)
    val collections = app.collections

    val surahs = MutableStateFlow<Map<Int, Surah>>(emptyMap())
    val juz = MutableStateFlow<List<Marker>>(emptyList())
    val hizb = MutableStateFlow<List<Marker>>(emptyList())
    val rub = MutableStateFlow<List<Marker>>(emptyList())
    val manzil = MutableStateFlow<List<Marker>>(emptyList())
    val ruku = MutableStateFlow<List<Marker>>(emptyList())
    val sajda = MutableStateFlow<List<Marker>>(emptyList())
    val pageMeta = MutableStateFlow<List<PageMeta>>(emptyList())
    val translations = MutableStateFlow<List<TranslationInfo>>(emptyList())

    val currentPage = MutableStateFlow(1)
    val pendingPage = MutableStateFlow<Int?>(null)

    // Ayat yang disorot di halaman; sheet hanya terbuka bila sheetVisible.
    val selected = MutableStateFlow<AyahRef?>(null)
    val sheetVisible = MutableStateFlow(false)

    val detail: StateFlow<AyahDetail?> = combine(
        selected,
        settingsRepository.settings.map { it.translationIds }.distinctUntilChanged(),
        // Muat ulang juga saat paket terjemahan selesai terpasang di latar belakang.
        translations,
    ) { sel, ids, _ -> sel to ids }
        .mapLatest { (sel, ids) -> sel?.let { mushaf.ayahDetail(it.surah, it.ayah, ids) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // Tambahan tematik ayat terpilih (tema, topik, ayat serupa, mutasyabihat); kosong bila datanya tidak dibundel.
    val ayahExtras: StateFlow<AyahExtras?> = selected
        .mapLatest { sel -> sel?.let { mushaf.ayahExtras(it.surah, it.ayah) }?.takeUnless { it.isEmpty } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // Pintu masuk Jelajahi hanya tampil bila quran.db memuat data topik.
    val exploreAvailable = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            exploreAvailable.value = mushaf.exploreAvailable()
            bookmarkStore.load()
            history.load()
            surahs.value = mushaf.surahs()
            juz.value = mushaf.markers(MarkerKind.Juz)
            hizb.value = mushaf.markers(MarkerKind.Hizb)
            rub.value = mushaf.markers(MarkerKind.Rub)
            manzil.value = mushaf.markers(MarkerKind.Manzil)
            ruku.value = mushaf.markers(MarkerKind.Ruku)
            sajda.value = mushaf.markers(MarkerKind.Sajda)
            pageMeta.value = mushaf.pageMeta()
            if (BuildConfig.LITE) return@launch
            translations.value = mushaf.translations()
            restoreActiveTranslations()
        }
        // Lite hanya membaca: tanpa terjemahan, cadangan, histori, pengingat, dan internet.
        if (!BuildConfig.LITE) {
            autoBackup()
            recordHistory()
            // Pasang ulang jadwal pengingat setiap kali pengaturannya berubah (termasuk saat aplikasi dibuka).
            viewModelScope.launch {
                settingsRepository.settings
                    .map { Triple(it.reminderEnabled, it.reminderMinutes, it.reminderUnit) }
                    .distinctUntilChanged()
                    .collect { io.zakkyhidayat.quran.reminder.Reminder.schedule(getApplication(), settingsRepository.settings.first()) }
            }
        }
        if (BuildConfig.UPDATER_ENABLED) checkForUpdate(manual = false)
    }

    /** Rilis yang lebih baru dari GitHub; dialog pembaruan tampil selama nilainya tidak null. */
    val update = MutableStateFlow<UpdateInfo?>(null)

    private val updaterPrefs get() = getApplication<Application>().getSharedPreferences("updater", android.content.Context.MODE_PRIVATE)

    /**
     * Cek pembaruan. Otomatis: paling sering sekali sehari, dan versi yang dilewati pengguna tidak ditawarkan lagi.
     * Manual (dari Pengaturan): selalu cek; hasil dilaporkan lewat [onResult] (true = ada, false = terbaru, null = gagal).
     */
    fun checkForUpdate(manual: Boolean, onResult: (Boolean?) -> Unit = {}) {
        val now = System.currentTimeMillis()
        if (!manual && now - updaterPrefs.getLong("checked_at", 0) < 24 * 60 * 60 * 1000L) return
        viewModelScope.launch {
            val result = runCatching { Updater.check() }
                .onFailure { android.util.Log.w("Updater", "Cek pembaruan gagal: $it") }
            updaterPrefs.edit().putLong("checked_at", now).apply()
            val info = result.getOrNull()
            val skipped = updaterPrefs.getString("skipped", null)
            if (info != null && (manual || info.version != skipped)) update.value = info
            onResult(if (result.isFailure) null else info != null)
        }
    }

    fun dismissUpdate(skip: Boolean) {
        if (skip) update.value?.let { updaterPrefs.edit().putString("skipped", it.version).apply() }
        update.value = null
    }

    /**
     * Cadangan otomatis: bila pengguna sudah memilih berkas tujuan, tulis ulang berkas itu setiap kali bookmark atau
     * posisi baca terakhir berubah. Jeda 2 detik menggabungkan perubahan beruntun (misalnya membalik banyak halaman).
     */
    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private fun autoBackup() {
        val settingsFlow = settingsRepository.settings
        viewModelScope.launch {
            combine(
                bookmarkStore.bookmarks,
                settingsFlow.map { Triple(it.lastPage, it.lastSurah, it.lastAyah) }.distinctUntilChanged(),
                settingsFlow.map { it.autoBackupUri }.distinctUntilChanged(),
            ) { _, _, uri -> uri }
                .debounce(2_000)
                .collectLatest { uri ->
                    if (uri == null) return@collectLatest
                    runCatching { Backup.export(getApplication(), android.net.Uri.parse(uri), settingsRepository, bookmarkStore) }
                        .onSuccess { settingsRepository.setAutoBackupAt(System.currentTimeMillis()) }
                        .onFailure {
                            android.util.Log.w("Backup", "Cadangan otomatis gagal: $it")
                            settingsRepository.setAutoBackupAt(-1)
                        }
                }
        }
    }

    /** Setiap posisi baca terakhir yang berubah (jeda 1 detik saat membalik halaman beruntun) masuk ke histori. */
    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private fun recordHistory() {
        viewModelScope.launch {
            settingsRepository.settings
                .map { it.lastSurah to it.lastAyah }
                .distinctUntilChanged()
                .debounce(1_000)
                .collect { (surah, ayah) -> history.record(surah, ayah) }
        }
    }

    fun clearHistory() = viewModelScope.launch { history.clear() }

    /**
     * Terjemahan tidak dibundel: terjemahan aktif yang belum terpasang (bawaan untuk pengguna baru, atau terjemahan yang
     * dulu dibundel bagi pengguna lama) diunduh diam-diam dari katalog. Gagal (luring) dicoba lagi saat aplikasi dibuka.
     */
    suspend fun restoreActiveTranslations() {
        val active = settingsRepository.settings.first().translationIds
        val installed = translations.value.map { it.id }.toSet()
        val missing = active.filter { it !in installed }
        if (missing.isEmpty()) return
        val packs = runCatching { mushaf.catalog() }
            .onFailure { android.util.Log.w("TranslationPacks", "Katalog gagal dimuat: $it") }
            .getOrNull() ?: return
        for (pack in packs.filter { it.id in missing }) {
            runCatching { installPack(pack) }.onFailure { android.util.Log.w("TranslationPacks", "Gagal memasang ${pack.id}: $it") }
        }
    }

    suspend fun catalog(): List<CatalogPack> = mushaf.catalog()

    /** Unduh dan pasang paket, lalu segarkan daftar terjemahan. Melempar IOException bila gagal. */
    suspend fun installPack(pack: CatalogPack, onProgress: (Long, Long) -> Unit = { _, _ -> }) {
        try {
            mushaf.installPack(pack, onProgress)
        } finally {
            withContext(NonCancellable) { translations.value = mushaf.translations() }
        }
    }

    fun deletePack(id: String) {
        viewModelScope.launch {
            mushaf.deletePack(id)
            translations.value = mushaf.translations()
        }
    }

    fun goToPage(page: Int) {
        pendingPage.value = page.coerceIn(1, 604)
    }

    fun goToAyah(surah: Int, ayah: Int, openSheet: Boolean = false) {
        viewModelScope.launch {
            selected.value = AyahRef(surah, ayah)
            sheetVisible.value = openSheet
            settingsRepository.setLastAyah(surah, ayah)
            goToPage(mushaf.ayahPage(surah, ayah))
        }
    }

    fun randomAyah() {
        val all = surahs.value.values
        if (all.isEmpty()) return
        var n = (1..all.sumOf { it.ayahCount }).random()
        for (surah in all) {
            if (n <= surah.ayahCount) { goToAyah(surah.id, n, openSheet = true); return }
            n -= surah.ayahCount
        }
    }

    fun selectAyah(ref: AyahRef) {
        selected.value = ref
        sheetVisible.value = true
        viewModelScope.launch { settingsRepository.setLastAyah(ref.surah, ref.ayah) }
    }

    fun moveSelection(step: Int) {
        val current = selected.value ?: return
        viewModelScope.launch {
            val next = mushaf.neighbour(current, step) ?: return@launch
            selected.value = next
            goToPage(mushaf.ayahPage(next.surah, next.ayah))
        }
    }

    fun clearSelection() {
        selected.value = null
        sheetVisible.value = false
    }

    fun dismissSheet() {
        sheetVisible.value = false
    }

    fun togglePageBookmark(page: Int) = viewModelScope.launch { bookmarkStore.togglePage(page) }

    fun toggleAyahBookmark(ref: AyahRef, page: Int) = viewModelScope.launch { bookmarkStore.toggleAyah(page, ref.surah, ref.ayah) }

    fun deleteBookmark(id: Long) = viewModelScope.launch { bookmarkStore.delete(id) }
}
