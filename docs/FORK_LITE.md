# Rencana varian "Mushaf Lite": hanya membaca per halaman

Status: usulan, belum dikerjakan.

## Lingkup

Yang **ada**:

- Pembaca mushaf per halaman (604 halaman, font V4, warna tajwid sesuai mode tema).
- Layar utama berisi tab: Bookmark, Surah, Juz, Hizb, Rub', Manzil, Ruku.
- Bookmark halaman (tombol yang sudah ada di bar atas pembaca).
- Bookmark ayat lewat sheet ringkas: tap ayat → sheet berisi nama surah dan nomor ayat, posisi (halaman, juz, hizb), dan
  satu tombol bookmark. Tanpa terjemahan, transliterasi, salin, bagikan, atau info tambahan.
- Lanjutkan dari posisi terakhir.
- Pengaturan: hanya tema terang, gelap, dan ikuti sistem.

Yang **dibuang**: terjemahan dan transliterasi, isi sheet ayat selain bookmark (terjemahan, salin, bagikan, ayat serupa, dan lain-lain), mode ayat+terjemahan dan terjemahan saja,
pencarian, Beranda, Jelajahi/topik, info surah, pengingat, cadangan, updater, onboarding, pilihan bahasa antarmuka di
dalam aplikasi, dan unduhan apa pun.

Konsekuensi yang menguntungkan: aplikasi **tanpa izin internet dan notifikasi**, sehingga kebijakan privasinya menjadi
"tidak mengumpulkan dan tidak mengirim data apa pun".

## Keputusan utama: repo terpisah atau flavor di repo ini

Ada dua jalan. Saya sarankan **flavor di repo ini**, kecuali Anda memang ingin keduanya berkembang terpisah.

| | Flavor `lite` di repo ini (disarankan) | Fork ke repo baru |
|---|---|---|
| Perbaikan pembaca (mis. animasi geser, issue #9) | Otomatis berlaku di kedua aplikasi | Harus di-port manual, mudah tertinggal |
| Data `quran.db`, font, verifikasi teks Qur'an | Satu sumber, satu CI | Dua salinan yang harus disinkronkan |
| Kerumitan kode | Ada percabangan per flavor (`BuildConfig.LITE`) | Kode lebih bersih, hanya yang dipakai |
| Ukuran APK | Kode fitur lain bisa dipangkas R8, tetapi `quran.db` tetap penuh kecuali dibuat versi ramping | Bebas memangkas `quran.db` |
| Rilis | Dua `applicationId` dari satu tag | Siklus rilis sendiri |

Risiko terbesar fork: **teks Qur'an dan perbaikan pembaca bercabang**. Kalau suatu saat ditemukan galat konten
(`docs/CONTENT_ERRORS.md`), Anda harus memperbaikinya di dua tempat. Untuk aplikasi Qur'an, ini risiko yang paling
mahal.

## Jalan A: flavor `lite` (disarankan)

1. **Dimensi flavor baru** di `app/build.gradle.kts`: `edition` = `full` / `lite`, berdampingan dengan `github` / `play`.
   `lite` memakai `applicationIdSuffix = ".lite"` (atau id sendiri), nama aplikasi dan ikon sendiri, dan
   `buildConfigField("boolean", "LITE", "true")`.
2. **Manifest khusus `lite`** (`app/src/lite/AndroidManifest.xml`) dengan `tools:node="remove"` untuk izin `INTERNET`,
   `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED` dan `ReminderReceiver`.
3. **Navigasi** (`AppNav.kt`): untuk `LITE`, daftarkan hanya rute `index` dan `reader`, ditambah pengaturan tema.
4. **Layar utama** (`index/IndexScreen.kt`): sembunyikan tab Beranda dan ikon Jelajahi, Cari, Ayat acak. Tab
   Bookmark, Surah, Juz, dan seterusnya tetap.
5. **Pembaca** (`reader/ReaderScreen.kt`): kunci ke mode mushaf, sembunyikan pil mode, tombol cari, dan penghitung.
   Tombol bookmark halaman tetap. Tap ayat tetap menyorot ayat dan membuka sheet, tetapi isinya `AyahBookmarkSheet`
   (lihat bagian "Sheet bookmark ayat"), bukan `AyahSheetContent`. Lewati pemuatan `extras` dan terjemahan untuk sheet.
6. **Pengaturan**: layar ringkas berisi pilihan tema saja (pakai ulang bagian dari `AppearanceSettings.kt`).
7. **Startup** (`AppViewModel.kt`): lewati onboarding, unduhan terjemahan otomatis, updater, dan cadangan otomatis bila
   `LITE`.
8. **CI**: tambah `:app:assembleGithubLiteDebug` (nama tugas mengikuti urutan dimensi) ke `ci.yml`.
9. **Dokumen**: tambah baris di `ARCHITECTURE.md` dan `CLAUDE.md` bahwa fitur baru harus dicek dampaknya ke `lite`.

Perkiraan: perubahan di sekitar 8–10 berkas, tanpa menyentuh logika render halaman.

## Jalan B: fork ke repo baru

Bila tetap memilih repo terpisah:

1. **Buat repo dengan riwayat**, bukan salin berkas, supaya perbaikan dari repo induk bisa di-cherry-pick:
   ```bash
   git clone https://github.com/zakkyhidayat/quran quran-lite
   cd quran-lite
   git remote rename origin upstream
   git remote add origin https://github.com/zakkyhidayat/quran-lite
   git push -u origin main
   ```
   Untuk menarik perbaikan: `git fetch upstream && git cherry-pick <commit>`.
2. **Ganti identitas**: `applicationId`, nama aplikasi, ikon, `GITHUB_REPO`. Hapus `TRANSLATION_CATALOG_URL` dan
   flavor updater.
3. **Hapus paket dan berkas**:
   - Paket utuh: `home/`, `explore/`, `search/`, `info/`, `onboarding/`, `reminder/`.
   - `data/`: `TranslationPacks.kt`, `Backup.kt`, `Updater.kt`, `Collections.kt`, `ReadingHistory.kt`.
   - `reader/`: `AyahListReader.kt`, `AyahShare.kt`, `ReaderControls.kt` (bagian pil mode). `AyahSheet.kt` diganti
     `AyahBookmarkSheet` (lihat bagian "Sheet bookmark ayat").
   - `settings/`: semua kecuali `AppSettings.kt` dan bagian tema dari `AppearanceSettings.kt`.
   - `ui/`: `HtmlContent.kt`, `UpdateDialog.kt`.
   - `MushafRepository.kt`: fungsi topik, tema, ayat serupa, mutasyabihat, morfologi, info surah, transliterasi.
   - Izin dan receiver di `AndroidManifest.xml`; string yang tidak terpakai (lint `UnusedResources` membantu).
4. **Ramping-kan `quran.db`**: di `tools/build_db.py`, lewati tabel `surah_info`, `transliteration`, tabel terjemahan,
   dan semua tabel penjelajahan. Tabel yang tetap: `surahs`, `ayahs` (dipakai TalkBack), `words`, `page_lines`, `juz`,
   `hizb`, `rub`, `manzil`, `ruku`, `sajda`. Jalankan `tools/verify_quran.py` setelahnya.
5. **Dokumen**: tulis ulang README, `PRIVACY.md` (tanpa data, tanpa internet), dan `THIRD_PARTY_NOTICES.md` (cukup
   KFGQPC dan QUL untuk teks dan metadata; bagian terjemahan dihapus). Lisensi tetap **GPL-3.0** karena turunan.
6. **CLAUDE.md** baru yang menyatakan lingkup sempit ini sebagai aturan keras: fitur di luar lingkup ditolak, bukan
   ditambahkan.

## Sheet bookmark ayat

Buat composable kecil baru, jangan menambah parameter ke `AyahSheetContent`. Sheet penuh sudah punya banyak cabang
(transliterasi, extras, salin, bagikan); menambah mode `lite` ke dalamnya membuat keduanya sulit dirawat.

```kotlin
@Composable
fun AyahBookmarkSheet(detail: AyahDetail, surah: Surah?, bookmarked: Boolean, onToggleBookmark: () -> Unit,
                      onPrevious: () -> Unit, onNext: () -> Unit)
```

Isi, dari atas ke bawah:

1. Baris judul: `Al-Baqarah 2:255`, di bawahnya `ayah_meta` (halaman, juz, hizb, rub', manzil, ruku). Panah ayat
   sebelum/berikutnya tetap, supaya bisa bookmark ayat tetangga tanpa menutup sheet.
2. Satu tombol bookmark yang lebar dengan label teks ("Tandai ayat" / "Hapus tanda"), bukan ikon saja, karena hanya
   ini satu-satunya aksi.
3. Label sajdah bila ayatnya ayat sajdah (sudah ada di `detail.info.sajda`).

Teks Arab ayat tidak perlu ditampilkan ulang: ayat sudah tersorot di halaman di belakang sheet. String yang dipakai
sudah ada (`bookmark_ayah`, `remove_bookmark`, `ayah_meta`, `previous_ayah`, `next_ayah`), jadi tidak ada kunci
terjemahan baru.

Alternatif yang lebih ringan: tekan lama ayat langsung menandai, dengan snackbar "Ayat ditandai · Batal". Lebih cepat,
tetapi gestur tekan lama sulit ditemukan pengguna dan rawan tertekan tanpa sengaja. Sheet lebih jelas; tekan lama bisa
ditambahkan nanti sebagai jalan pintas.

## Hal yang perlu diputuskan

- Jalan A atau B.
- Bahasa antarmuka: tetap 10 bahasa (ikut bahasa sistem) atau hanya Indonesia dan Inggris.
- Tab Sajdah saat ini disembunyikan di aplikasi penuh; tetap disembunyikan di lite?
