<p align="center"><img src="docs/icon.png" width="128" alt="Ikon aplikasi Quran"></p>

# Quran

Aplikasi Android untuk membaca Al-Qur'an dalam **rasm Utsmani gaya Madinah**, persis seperti halaman mushaf cetak
King Fahd Complex: 604 halaman, 15 baris, dengan pewarnaan tajwid.

> *English summary below.*

[![Dukung di Ko-fi](https://img.shields.io/badge/Ko--fi-dukung-FF5E5B?logo=ko-fi&logoColor=white)](https://ko-fi.com/zakkyhidayat)

## Untuk siapa

Aplikasi ini **sengaja beropini**. Ia dibuat untuk pembaca yang ingin membaca dengan mushaf Madinah (KFGQPC), bukan untuk
semua orang.

Di Indonesia, mayoritas pembaca terbiasa dengan khat **IndoPak** atau **Mushaf Standar Indonesia** (Kemenag): bentuk huruf,
harakat, tanda waqaf, dan letak halamannya berbeda dari mushaf Madinah. Aplikasi ini **tidak** menyediakan gaya itu dan
tidak berencana menambahkannya. Bila Anda terbiasa dengan IndoPak, aplikasi lain akan lebih cocok.

Pilihan yang sudah dikunci:

| Keputusan | Pilihan | Alasannya |
|-----------|---------|-----------|
| Rasm dan khat | Utsmani, KFGQPC V4 (Madinah) | Mushaf yang paling banyak dipakai di dunia; glyph per kata identik dengan cetakan |
| Riwayat | Hafs 'an 'Ashim | Sesuai mushaf Madinah standar |
| Tata letak | 604 halaman, 15 baris, halaman tetap | Hafalan bergantung pada posisi ayat di halaman; teks tidak mengalir ulang |
| Tajwid | Warna tajwid bawaan font V4 (bisa dimatikan) | Warna dari sumber yang sama dengan mushaf cetak bertajwid |
| Tampilan | Satu gaya halaman saja | Lebih sedikit pilihan, lebih konsisten |
| Kata per kata | Tidak ada | Fokus membaca mushaf, bukan alat belajar bahasa |

## Fitur

- Halaman mushaf proporsi kertas B5, nama surah dan juz kaligrafi di atas halaman, mode terang, gelap, dan AMOLED, warna
  dinamis (Android 12+) atau palet asli, tiga tingkat kontras.
- Ketuk ayat untuk membuka terjemahan (dengan catatan kaki), transliterasi Latin, dan info letak ayat (juz, hizb, rub',
  manzil, ruku, sajdah).
- Terjemahan bawaan: Indonesia (Kemenag, The Sabiq Company, King Fahad Quran Complex) dan Inggris (Saheeh International).
  Terjemahan lain bisa diunduh dari dalam aplikasi.
- Daftar surah, juz, hizb, manzil, dan ayat sajdah; info surah; lompat ke ayat; ayat acak; baca terakhir per ayat.
- Bookmark halaman dan ayat, pencarian (teks Arab, terjemahan, nama surah, atau rujukan seperti `2:255`).
- Antarmuka 10 bahasa, mengikuti bahasa perangkat secara bawaan: Inggris, Indonesia, Arab, Urdu, Bengali, Turki, Persia,
  Melayu, Prancis, dan Rusia.
- Ramah pembaca layar: tiap ayat dibacakan sebagai teks Arab Unicode.
- Tanpa iklan, tanpa akun, tanpa pelacakan. Internet hanya dipakai saat mengunduh terjemahan.

Rencana: audio murattal.

## Sumber data, kredit, dan lisensi

Semua data Al-Qur'an diambil dari [Quranic Universal Library (QUL)](https://qul.tarteel.ai) milik Tarteel. Menurut
[halaman credits QUL](https://qul.tarteel.ai/credits), sebagian besar isinya dibuat atau dikurasi komunitas, bukan
Tarteel. Yang dipakai aplikasi ini:

| Bagian | Pembuat asli |
|--------|--------------|
| Font halaman V4 tajwid, layout 15 baris, header surah, font Hafs Uthmanic Script | King Fahd Glorious Quran Printing Complex (KFGQPC), dipakai tanpa perubahan |
| Teks Arab Unicode, metadata juz/hizb/manzil/ruku/sajdah | KFGQPC dan Tanzil, dikurasi QUL |
| Info surah, transliterasi | Quran.com dan kontributor QUL |
| Terjemahan | Penerjemah/penerbit masing-masing (Kemenag RI, The Sabiq Company, King Fahad Quran Complex, Saheeh International, dan paket unduhan) |
| Pengelolaan QUL | Naveed Ahmad dan tim/kontributor QUL |

Semoga Allah membalas semua yang terlibat. Daftar lengkap kontributor ada di halaman credits QUL, yang juga ditautkan
dari layar Tentang di aplikasi. Hak cipta font tetap pada KFGQPC; lisensi tiap terjemahan mengikuti penerbitnya.

Lisensi kode aplikasi ini belum ditentukan.

## Dukung

Aplikasi ini gratis, tanpa iklan. Bila bermanfaat, Anda bisa mendukung pengembangannya lewat
[Ko-fi](https://ko-fi.com/zakkyhidayat).

## Membangun dari sumber

Butuh JDK 17+, Android SDK, dan Python 3 dengan `fonttools`.

1. Unduh berkas QUL sesuai [docs/DATA_SOURCES.md](docs/DATA_SOURCES.md) ke folder `data-src/` (tidak masuk git; unduhan QUL
   butuh login).
2. Salin font halaman `data-src/ttf/p1.ttf`..`p604.ttf` ke `app/src/main/assets/fonts/` (ukurannya besar sehingga tidak
   disimpan di git). `quran.db` sudah ada di repo; bangun ulang hanya bila data berubah: `python tools/build_db.py`.
3. Bangun APK:
   ```bash
   ./gradlew :app:assembleDebug
   ```

Minimal Android 8.0 (API 26).

### Versi

Semantic versioning dari git tag `vX.Y.Z`; `versionCode = X*10000 + Y*100 + Z`.

### Dokumen lain

- [docs/DATA_SOURCES.md](docs/DATA_SOURCES.md): daftar sumber QUL, cara memperbarui data, dan paket terjemahan unduhan.

---

## English summary

An Android Quran reader for people who want to read the **Uthmani script in the Madani (KFGQPC V4) mushaf layout**:
604 fixed pages, 15 lines, with tajweed colouring, exactly like the printed King Fahd Complex mushaf. It is deliberately
opinionated: one script, one riwayah (Hafs 'an 'Asim), one page style, no IndoPak script and no word-by-word mode.
In Indonesia most readers use the IndoPak or Indonesian Standard Mushaf; this app intentionally does not cater to that.

Features: tappable ayahs with translations and footnotes, transliteration, bookmarks, search, surah/juz/hizb/manzil/sajdah
indexes, light/dark/AMOLED and dynamic colour, a UI in 10 languages (following the device language by default), and
downloadable translations. No ads, accounts, or tracking. Data: Quranic Universal Library (Tarteel) and its community contributors ([credits](https://qul.tarteel.ai/credits)); fonts: KFGQPC. Support: [Ko-fi](https://ko-fi.com/zakkyhidayat).
