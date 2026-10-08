# Audit aplikasi (checklist Android Quran)

Diisi 2026-10-08 berdasarkan kode dan dokumen di repo. Perbarui setiap rilis. `[x]` = terpenuhi, `[~]` = sebagian,
`[ ]` = belum.

## 0. Profil

- Bahasa/framework: Kotlin + Jetpack Compose (Material 3 Expressive)
- `minSdk` / `targetSdk`: 26 / 37
- Sumber teks Quran: KFGQPC lewat QUL (Tarteel). Halaman: glyph V4 per kata. Pencarian, salin, TalkBack: teks Unicode
  QPC Hafs.
- Standar rasm: Utsmani, mushaf Madinah, riwayat Hafs, 604 halaman 15 baris
- Terjemahan: QUL (Kemenag RI, The Sabiq Company, King Fahad Quran Complex, Saheeh International), diunduh di aplikasi
- Audio, tafsir: belum ada
- Distribusi: APK di GitHub Releases (varian `github`); varian `play` disiapkan
- Repo: https://github.com/zakkyhidayat/quran

## 1. Integritas konten

- [x] Sumber teks tercatat: penyedia, URL, folder; hash berkas sumber di `docs/data-manifest.json` (DATA_SOURCES.md)
- [x] Teks tidak diketik ulang; dimuat dari berkas sumber oleh `tools/build_db.py`
- [x] Sidik jari SHA-256 isi (`docs/quran-content.json`) dicek otomatis di CI (`tools/verify_quran.py`); terbukti gagal
      bila satu harakat diubah
- [x] Tes jumlah: 114 surah, 6.236 ayat, jumlah per surah (Kufi) dari tabel tertulis di skrip
- [x] Perbandingan dengan sumber: `verify_quran.py --source` membandingkan **seluruh** 6.236 ayat (lokal; butuh data-src)
- [~] Rendering diuji di satu perangkat (Android 16, 120 Hz). Belum di Android lama dan layar kecil.
- [~] Font sesuai rasm, lisensi KFGQPC tercatat; status "tidak boleh diubah" vs penggantian palet perlu dikonfirmasi
- [~] Juz/hizb/rub'/manzil/sajdah: jumlah dicek; posisi awal belum dibandingkan dengan sumber kedua
- n/a Pemetaan audio (belum ada audio)

## 2. Legalitas

- [ ] Izin redistribusi terjemahan (Kemenag, Sabiq, KFQC, Saheeh) **— tugas pemilik**
- n/a Tafsir, audio
- [ ] Tashih LPMQ: perlu atau tidak untuk aplikasi ini **— tugas pemilik (hubungi LPMQ)**
- [~] Lisensi KFGQPC: font dikirim apa adanya; salinan dengan palet bawaan lain dibuat saat berjalan. Konfirmasi ke
      KFGQPC apakah ini termasuk "perubahan" **— tugas pemilik**
- [x] Atribusi di layar Tentang (QUL, KFGQPC, tautan credits) dan THIRD_PARTY_NOTICES.md

## 3. Build yang bisa diulang

- [x] Git dengan commit bermakna; wrapper Gradle di-commit; versi dikunci di `libs.versions.toml`
- [~] Clone bersih: build debug berhasil tanpa font halaman (halaman memakai font bawaan); build lengkap butuh
      `page-fonts.zip` (rilis `build-assets`, belum diunggah) atau data-src
- [x] `build/`, `.gradle/`, `local.properties`, keystore di `.gitignore`

## 4. Kunci penandatanganan

- [ ] Keystore rilis dibuat dan dicadangkan di dua tempat terenkripsi **— tugas pemilik** (RELEASING.md)
- [ ] Password di password manager **— tugas pemilik**
- [x] Tidak ada keystore di repo
- [ ] Play App Signing (bila ke Play)
- [ ] Sidik jari sertifikat SHA-256 di README (setelah keystore ada)

## 5. Tes

- [~] Unit test (9): pembanding versi, rujukan ayat pencarian, tautan ayat, normalisasi teks Arab. Belum: bookmark,
      pemetaan halaman, cadangan JSON (butuh tes berbasis perangkat atau Robolectric)
- [x] Tes integritas konten di CI
- [ ] Tes UI dasar (buka, pilih surah, geser, cari, bookmark)
- [ ] Diuji di `minSdk` 26, versi terbaru, dan layar kecil
- [~] Mode gelap diuji; ukuran font sistem besar belum
- [~] Offline: membaca tanpa jaringan berjalan; terjemahan perlu diunduh sekali
- [ ] Rotasi layar dan kembali dari background: posisi baca belum diuji

## 6. Privasi dan izin

- [x] Izin diaudit: INTERNET (terjemahan, cek pembaruan), REQUEST_INSTALL_PACKAGES (hanya varian github). Izin
      pengingat dilepas selama fiturnya disembunyikan.
- n/a Lokasi
- [x] Tidak ada SDK pelacak/iklan; tidak ada Google Play Services. (Belum dipindai Exodus.)
- [x] Kebijakan privasi: PRIVACY.md
- [ ] Data Safety di Play Console **— tugas pemilik saat ke Play**
- [x] Bookmark dan posisi baca lokal; cadangan JSON

## 7. Play Store

- [x] `targetSdk` 37
- [x] AAB dibuat workflow rilis (artefak `play-bundle`)
- [ ] Rating konten, akun developer 2FA **— tugas pemilik**

## 8. Kualitas kode

- [x] Lapisan UI / ViewModel / repositori; quran.db diakses lewat satu lapisan, read-only
- [x] Android Lint di CI (`lint-baseline.xml` untuk 39 peringatan lama; temuan baru menggagalkan build)
- [x] CI: integritas konten, teks antarmuka, unit test, lint, build debug (`.github/workflows/ci.yml`)
- [~] R8 aktif; build setara rilis diuji berjalan normal di perangkat
- [~] File besar: `SettingsScreen.kt`, `ReaderScreen.kt`, `MushafPage.kt` perlu dipecah
- [x] Tidak ada pelaporan crash (tidak ada data dikirim)

## 9. Aksesibilitas

- [x] Ukuran teks Arab dan terjemahan bisa diatur terpisah (80-160%; daftar ayat dan lembar ayat. Halaman mushaf
      mengikuti tata letak cetak)
- [x] RTL Arab / LTR terjemahan tidak bertabrakan (antarmuka RTL juga diuji)
- [~] contentDescription ada; belum diuji dengan TalkBack sungguhan
- [x] Target sentuh 48dp (komponen M3)
- [x] Posisi baca tersimpan otomatis
- [x] Mode gelap memakai palet gelap resmi font (harakat tetap terbaca)

## 10. Rilis

- [x] `versionCode` dari tag, `versionName` SemVer
- [x] CHANGELOG.md
- [x] Rilis diberi tag (workflow rilis)
- [ ] F-Droid: build harus bisa diulang dari repo (font halaman tidak di repo)

## 11. Dokumentasi

- [~] README: tujuan, cara build, kredit. Belum: screenshot, cara menjalankan tes, sidik jari sertifikat
- [x] ARCHITECTURE.md
- [~] Sumber konten: DATA_SOURCES.md + THIRD_PARTY_NOTICES.md (lisensi). Tanggal unduh per berkas belum tercatat
- [x] ADR: docs/adr/0001 (sumber teks), 0002 (rasm), 0003 (framework)
- [x] CHANGELOG.md, LICENSE, THIRD_PARTY_NOTICES.md, PRIVACY.md
- [x] Prosedur salah teks: docs/CONTENT_ERRORS.md

## 12. Pemahaman pemilik

Diisi sendiri oleh pemilik. Alur teks dari sumber ke layar ada di ARCHITECTURE.md; prosedur salah teks di
docs/CONTENT_ERRORS.md.

---

## Catatan temuan

| No | Bagian | Temuan | Prioritas | Status |
|----|--------|--------|-----------|--------|
| 1 | 2 | Izin redistribusi terjemahan belum dikonfirmasi | Tinggi | Tugas pemilik |
| 2 | 2 | Tashih LPMQ belum diketahui perlu/tidak | Tinggi | Tugas pemilik |
| 3 | 2 | Penggantian palet font KFGQPC perlu dikonfirmasi | Tinggi | Tugas pemilik |
| 4 | 4 | Keystore rilis belum ada | Tinggi | Tugas pemilik |
| 5 | 1 | Sidik jari isi belum dicek otomatis | Tinggi | Selesai (CI) |
| 6 | 1 | Perbandingan ayat dengan sumber belum ada | Tinggi | Selesai (seluruh ayat, lokal) |
| 7 | 5 | Tidak ada tes | Tinggi | Sebagian (9 unit test, CI) |
| 8 | 6 | Izin pengingat dideklarasikan padahal fitur disembunyikan | Sedang | Selesai |
| 9 | 6 | Kebijakan privasi belum ada | Sedang | Selesai |
| 10 | 11 | ARCHITECTURE, CHANGELOG, THIRD_PARTY_NOTICES, prosedur salah teks belum ada | Sedang | Selesai |
| 11 | 9 | Ukuran font Arab/terjemahan belum bisa diatur | Sedang | Selesai |
| 12 | 5 | Belum diuji di Android 8 (API 26), layar kecil, rotasi | Sedang | Belum |
| 13 | 3 | `page-fonts.zip` belum diunggah ke rilis `build-assets` | Sedang | Menunggu izin pemilik |
| 14 | 8 | Linter belum ada; file layar terlalu besar | Rendah | Lint selesai; pemecahan file belum |
| 15 | 11 | ADR, screenshot README | Rendah | ADR selesai; screenshot belum |
| 16 | 1 | Pencarian Arab tidak menemukan ejaan mushaf (إبراهيم, الصلاة: 0 hasil) | Tinggi | Selesai (ditemukan unit test) |
| 17 | 8 | Lint: teks dari LocalContext di komposisi, Locale.getDefault di komponen | Sedang | Selesai |
