package io.zakkyhidayat.quran

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.zakkyhidayat.quran.data.AyahDetail
import io.zakkyhidayat.quran.data.AyahExtras
import io.zakkyhidayat.quran.data.AyahRef
import io.zakkyhidayat.quran.data.CatalogPack
import io.zakkyhidayat.quran.data.ContentKind
import io.zakkyhidayat.quran.data.ContentPack
import io.zakkyhidayat.quran.data.InstalledContentPack
import io.zakkyhidayat.quran.data.ExploreCounts
import io.zakkyhidayat.quran.data.isPackUpdateAvailable
import io.zakkyhidayat.quran.data.Marker
import io.zakkyhidayat.quran.data.MarkerKind
import io.zakkyhidayat.quran.data.PageMeta
import io.zakkyhidayat.quran.data.Surah
import io.zakkyhidayat.quran.data.SunnahReading
import io.zakkyhidayat.quran.data.AyahRange
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

    // Hitungan kartu Jelajahi di layar utama; null selama belum dimuat.
    val exploreCounts = MutableStateFlow<ExploreCounts?>(null)

    init {
        // Hitungan dimuat terpisah agar tidak menunda daftar surah.
        viewModelScope.launch { exploreCounts.value = mushaf.exploreCounts() }
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
            translations.value = mushaf.translations()
            restoreActiveTranslations()
        }
        // Cadangan otomatis menyimpan bookmark dan posisi baca ke folder yang dipilih.
        autoBackup()
        recordHistory()
        // Pasang ulang jadwal pengingat setiap kali pengaturannya berubah (termasuk saat aplikasi dibuka).
        viewModelScope.launch {
            settingsRepository.settings
                .map { listOf(it.reminderEnabled, it.reminderMinutes, it.reminderUnit, it.bedtimeSunnahReminderEnabled) }
                .distinctUntilChanged()
                .collect { io.zakkyhidayat.quran.reminder.Reminder.schedule(getApplication(), settingsRepository.settings.first()) }
        }
    }

    /**
     * Cadangan otomatis: setelah jeda dua detik dari perubahan bookmark atau posisi baca, simpan snapshot ke folder
     * SAF yang dipilih. Backup.exportToFolder menggabungkan data terdahulu dan mempertahankan tiga snapshot terbaru.
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
                    runCatching {
                        bookmarkStore.load()
                        Backup.exportToFolder(getApplication(), android.net.Uri.parse(uri), settingsRepository, bookmarkStore)
                    }
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

    fun startSunnahReading(reading: SunnahReading, resume: Boolean = false) {
        viewModelScope.launch {
            val stored = settingsRepository.settings.first()
            val ranges = sunnahRanges(reading)
            if (ranges.isEmpty()) return@launch
            val step = if (resume) stored.sunnahReadingStep.coerceIn(0, ranges.lastIndex) else 0
            if (!resume || stored.sunnahReadingId != reading.name) {
                settingsRepository.setSunnahReading(reading.name, step)
            }
            val range = ranges[step]
            val last = stored.lastSurah to stored.lastAyah
            val resumePosition = resume && stored.sunnahReadingId == reading.name &&
                range.contains(last.first, last.second)
            val surah = if (resumePosition) last.first else range.surah
            val ayah = if (resumePosition) last.second else range.from
            selected.value = AyahRef(surah, ayah)
            sheetVisible.value = false
            settingsRepository.setLastAyah(surah, ayah)
            goToPage(mushaf.ayahPage(surah, ayah))
        }
    }

    fun advanceSunnahReading() {
        viewModelScope.launch {
            val stored = settingsRepository.settings.first()
            val reading = SunnahReading.entries.firstOrNull { it.name == stored.sunnahReadingId } ?: return@launch
            val ranges = sunnahRanges(reading)
            val nextStep = stored.sunnahReadingStep + 1
            if (nextStep >= ranges.size) {
                settingsRepository.clearSunnahReading()
                return@launch
            }
            settingsRepository.setSunnahReading(reading.name, nextStep)
            val next = ranges[nextStep]
            selected.value = AyahRef(next.surah, next.from)
            sheetVisible.value = false
            settingsRepository.setLastAyah(next.surah, next.from)
            goToPage(mushaf.ayahPage(next.surah, next.from))
        }
    }

    fun dismissSunnahReading() = viewModelScope.launch { settingsRepository.clearSunnahReading() }

    private suspend fun sunnahRanges(reading: SunnahReading): List<AyahRange> {
        val names = surahs.value.ifEmpty { mushaf.surahs().also { surahs.value = it } }
        return AyahRange.parseList(reading.refs).mapNotNull { range ->
            val end = if (range.to == 0) names[range.surah]?.ayahCount ?: 0 else range.to
            range.copy(to = end).takeIf { end >= range.from }
        }
    }

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

    /** Paket terpasang yang punya versi lebih baru di katalog, menurut id. Diisi [checkPackUpdates]. */
    val packUpdates = MutableStateFlow<Map<String, CatalogPack>>(emptyMap())

    /** Bandingkan versi paket terpasang dengan katalog; gagal (luring) diam-diam dilewati. */
    suspend fun checkPackUpdates() {
        val packs = runCatching { mushaf.catalog() }
            .onFailure { if (it is kotlinx.coroutines.CancellationException) throw it }
            .getOrNull() ?: return
        val installed = translations.value.filter { it.downloaded }.associateBy { it.id }
        packUpdates.value = packs.filter { p -> installed[p.id]?.let { isPackUpdateAvailable(it.version, p.version) } == true }.associateBy { it.id }
    }

    /** Unduh ulang paket terpasang ke versi katalog; setelah berhasil tanda pembaruannya dihapus. */
    suspend fun updatePack(pack: CatalogPack, onProgress: (Long, Long) -> Unit = { _, _ -> }) {
        installPack(pack, onProgress)
        packUpdates.value = packUpdates.value - pack.id
    }

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

    // ---- Tafsir: paket unduhan opsional (katalog yang sama dengan terjemahan) ----

    val tafsirInstalled = MutableStateFlow<List<InstalledContentPack>>(emptyList())

    /** Tafsir yang dipilih di lembar ayat (id paket); null = tafsir terpasang pertama. Bertahan selama aplikasi hidup. */
    val tafsirChoice = MutableStateFlow<String?>(null)

    /** Tafsir terpasang yang punya versi lebih baru di katalog, menurut id. Diisi [checkTafsirUpdates]. */
    val tafsirUpdates = MutableStateFlow<Map<String, ContentPack>>(emptyMap())

    suspend fun refreshTafsir() {
        tafsirInstalled.value = mushaf.installedContent(ContentKind.Tafsir)
    }

    suspend fun tafsirCatalog(): List<ContentPack> = mushaf.contentCatalog(ContentKind.Tafsir)

    /** Bandingkan versi tafsir terpasang dengan katalog; gagal (luring) diam-diam dilewati. */
    suspend fun checkTafsirUpdates() {
        val packs = runCatching { tafsirCatalog() }
            .onFailure { if (it is kotlinx.coroutines.CancellationException) throw it }
            .getOrNull() ?: return
        val installed = tafsirInstalled.value.associateBy { it.id }
        tafsirUpdates.value = packs.filter { p -> installed[p.id]?.let { isPackUpdateAvailable(it.version, p.version) } == true }.associateBy { it.id }
    }

    /** Unduh dan pasang (atau perbarui) tafsir, lalu segarkan daftar. Melempar IOException bila gagal. */
    suspend fun installTafsir(pack: ContentPack, onProgress: (Long, Long) -> Unit = { _, _ -> }) {
        try {
            mushaf.installContent(ContentKind.Tafsir, pack, onProgress)
            tafsirUpdates.value = tafsirUpdates.value - pack.id
        } finally {
            withContext(NonCancellable) { refreshTafsir() }
        }
    }

    fun deleteTafsir(id: String) {
        viewModelScope.launch {
            mushaf.deleteContent(ContentKind.Tafsir, id)
            tafsirUpdates.value = tafsirUpdates.value - id
            refreshTafsir()
        }
    }

    fun goToPage(page: Int) {
        pendingPage.value = page.coerceIn(1, 604)
    }

    fun goToAyah(surah: Int, ayah: Int, openSheet: Boolean = false) {
        selected.value = AyahRef(surah, ayah)
        sheetVisible.value = openSheet
        viewModelScope.launch {
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
