# Data bahasa: inventaris, distribusi, dan status lisensi

Catatan kerja untuk semua data berbahasa (terjemahan, transliterasi, info surah, topik, morfologi). Status lisensi di
sini **belum final**: qul.tarteel.ai tidak bisa dibuka dari lingkungan tempat catatan ini ditulis, jadi halaman sumber
per terjemahan belum dibaca langsung. Bagian yang perlu dicek diberi tanda **[perlu verifikasi]**.

## Posisi QUL soal hak cipta

FAQ QUL: "The resources available on QUL vary in their copyright status. Some are in the public domain, while others may
be subject to specific licenses. We recommend reviewing the licensing information provided by each resource's author
before use."

Artinya: berasal dari QUL **tidak** sama dengan boleh diredistribusi. Izin harus dicek pada sumber asli tiap data
(penerbit, penerjemah, atau situs yang dikutip QUL).

## Yang diunduh user dari aplikasi

Hanya **paket terjemahan**. Data lain dibundel di `quran.db` di dalam APK.

- Katalog: `tools/translation_catalog.json`, 187 entri; 14 ditandai `excluded` beserta alasannya (termasuk 1 `skip`, `en-khattab`), sehingga
  173 terjemahan dalam 88 bahasa yang dibangun.
- Build: `tools/build_translation_packs.py` menghasilkan `build/translation-packs/<id>.db` dan `catalog.json`.
- Distribusi: GitHub Release `translations`. URL katalog tertanam di `app/build.gradle.kts`
  (`TRANSLATION_CATALOG_URL`).
- Di aplikasi: `data/TranslationPacks.kt` mengambil `catalog.json` setiap kali dialog Tambah Terjemahan dibuka, jadi
  **daftar terjemahan sudah dinamis**. Paket diperiksa (SHA-256, 6.236 baris) lalu disimpan di
  `filesDir/translations/`.
- Catatan kaki dipertahankan di tabel `footnotes` paket.

### Celah yang ditemukan

1. **Paket terpasang tidak pernah diperbarui.** Field `version` dibaca dari katalog (`TranslationPacks.kt`), tetapi
   tidak dibandingkan dengan versi paket yang terpasang. Kalau `PACK_VERSION` naik, user lama tetap memakai teks lama.
   Untuk terjemahan dari QuranEnc, ini juga melanggar syarat "update ke versi terbaru" (lihat di bawah).
2. **Sumber, atribusi, dan versi tidak ditampilkan** di dialog terjemahan. Field `source` hanya dipakai untuk pencarian.
3. Pindah lokasi katalog berarti harus merilis APK baru. Untuk saat ini ini masih wajar.

## Data opsional di sisi build (bukan unduhan user)

Data penjelajahan tematik ikut ke `quran.db` hanya bila sumbernya ada di `data-src/` (lihat `docs/DATA_SOURCES.md`).

| Data | Tabel | Status unduh |
|------|-------|--------------|
| Topik (2.512) | `topics`, `topic_ayahs`, `topic_links` | Sudah |
| Ayat serupa | `similar_ayahs` | Sudah |
| Morfologi akar/lema/stem | `morph_*`, `word_morph` | Sudah |
| Mutasyabihat | `mutashabihat*` | Sudah (814 frasa) |
| Tema ayat | `ayah_themes` | Sudah (2.098 tema) |
| Jenis kata (POS) | `word_morph.pos` | Belum, halaman QUL galat 500 |

## Inventaris data berbahasa dan status lisensi

| Data | Sumber | Status |
|------|--------|--------|
| Transliterasi Latin | QUL 469 | Kontributor QUL, lisensi belum diketahui **[perlu verifikasi]** |
| Info surah Indonesia | QUL 454 | Belum diketahui **[perlu verifikasi]** |
| Info surah Inggris | QUL 3 | Kemungkinan dari Tafhim (Maududi), masih berhak cipta **[perlu verifikasi]** |
| Topik (nama, deskripsi Inggris) | QUL 45 | Belum diketahui **[perlu verifikasi]** |
| Morfologi (akar, lema, stem) | QUL 75-77 | Kemungkinan dari Quranic Arabic Corpus (GPL; wajib atribusi, data tidak boleh diubah) **[perlu verifikasi]** |
| String antarmuka (10 bahasa) | Proyek ini | Aman |
| Terjemahan | Lihat kelompok di bawah | Bervariasi |

## Terjemahan menurut asal

Pemetaan entri ke kelompok belum dilakukan per entri **[perlu verifikasi di halaman QUL tiap entri]**.

### 1. QuranEnc.com

Contoh penerbit: Ruwwad/Rowwad/Rowad Center, Dar Al-Salam Center, IslamHouse.com, Pioneers of Translation Center, dan
kemungkinan sebagian terjemahan King Fahad Quran Complex.

Syarat QuranEnc untuk menerbitkan ulang (https://quranenc.com):

- teks tidak diubah, ditambah, atau dikurangi
- menyebut QuranEnc.com sebagai penerbit dan sumber
- mencantumkan nomor versi
- mempertahankan informasi terjemahan
- melaporkan catatan koreksi ke QuranEnc
- memperbarui ke versi terbaru
- tanpa iklan yang tidak pantas

Kondisi aplikasi saat ini:

- Teks dan catatan kaki dipertahankan: sudah memenuhi.
- Atribusi dan nomor versi: belum ditampilkan.
- Pembaruan ke versi terbaru: belum ada.
- Iklan: tidak ada, jadi sudah memenuhi.

### 2. Tanzil.net

Contoh: Pickthall, Yusuf Ali, Arberry, Shakir, Sablukov, dan karya klasik lain.

Syarat Tanzil:

- Penggunaan non-komersial saja. Penggunaan lain perlu izin penerjemah atau penerbit.
- Aplikasi yang memakai lebih dari tiga terjemahan Tanzil wajib menautkan ke halaman terjemahan Tanzil.

Aplikasi ini gratis dan tanpa iklan, jadi syarat non-komersial terpenuhi. Tautan balik ke Tanzil belum ada.

### 3. Hak cipta komersial yang jelas (risiko tinggi tanpa izin tertulis)

- **Abdul Haleem**: Oxford University Press. Halaman hak cipta buku mewajibkan izin tertulis untuk reproduksi.
- Muhammad Asad, Mufti Taqi Usmani, Maarif-ul-Quran, Fi Zilal al-Quran, Wahiduddin Khan, Abdul Majid Daryabadi,
  Maududi (Tafhim): kemungkinan masih dilindungi penerbitnya **[perlu verifikasi]**.
- **Saheeh International** (bawaan): Dar Abul-Qasim. Tidak ditemukan kebijakan izin yang dipublikasikan. Perlu kontak
  penerbit.

### 4. Kemenag RI (bawaan)

Edisi 2019 dibagikan gratis lewat Pustaka Lajnah (pustakalajnah.kemenag.go.id). Syarat untuk aplikasi pihak ketiga
belum ditemukan. Ada juga soal tashih LPMQ untuk aplikasi Al-Qur'an di Indonesia. Perlu dikonfirmasi ke LPMQ.

### 5. Asal tidak diketahui

Sekitar 15 entri bernama "... translation (QUL)", "unknown", atau tanpa penerjemah yang jelas. Karena penerbitnya tidak
diketahui, izinnya juga tidak bisa dipastikan. Usulan: tandai `skip` sampai asalnya jelas.

### Karya lama (kemungkinan domain publik)

Contoh: Pickthall, Sablukov, Bielawski. Statusnya bergantung pada negara dan edisi. Edisi revisi bisa punya hak cipta
baru **[perlu verifikasi]**.

## Langkah berikutnya

1. Tambah kolom `origin` (`quranenc` / `tanzil` / `publisher` / `unknown`) dan `license_note` di
   `tools/translation_catalog.json`, lalu isi dari halaman QUL tiap entri. Ini butuh akses ke qul.tarteel.ai.
2. Tampilkan sumber, atribusi, dan versi di dialog terjemahan, plus tautan ke Tanzil dan QuranEnc.
3. Perbarui paket terpasang ketika `version` di katalog naik. Pilihannya otomatis atau lewat tombol "Perbarui"; belum
   diputuskan.
4. Tarik entri kelompok 3 dan 5 dari release `translations` sampai izinnya jelas.
5. Hubungi Dar Abul-Qasim (Saheeh), OUP (Haleem), dan LPMQ (Kemenag) untuk izin tertulis.
6. Perbarui `THIRD_PARTY_NOTICES.md` setelah langkah 1 selesai.

Catatan ini bukan nasihat hukum.
