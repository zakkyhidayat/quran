# Sumber data dan cara memperbaruinya

Seluruh data aplikasi berasal dari [Quranic Universal Library (QUL)](https://qul.tarteel.ai) milik Tarteel dan harus
diunduh **manual** dari browser (butuh login). Dokumentasi QUL tentang
[mengunduh data](https://qul.tarteel.ai/docs/downloading-data) saat ini hanya menjelaskan cara memuat berkas yang sudah
diunduh; [API-nya](https://qul.tarteel.ai/docs/api) masih ditandai "Coming soon" dan tidak ada pola URL atau token unduhan.

> **Tinjau ulang** halaman [docs/api](https://qul.tarteel.ai/docs/api) tiap kali ada pembaruan besar. Begitu API tersedia,
> langkah unduh manual di bawah bisa diganti skrip.

Berkas mentah disimpan di `data-src/` (tidak masuk git). Yang masuk git: `tools/build_db.py` (pembangun database),
`app/src/main/assets/quran.db` (hasilnya), dan `docs/data-manifest.json` (hash berkas sumber yang dipakai).

## Daftar sumber

Setiap baris: apa yang diunduh, di mana, dan ke mana ia disimpan. Semua halaman berikut bisa dibuka tanpa login, tetapi
tombol unduhnya butuh login. Ambil format **SQLite** kecuali dicatat lain.

| # | Data | Halaman QUL | Simpan ke (di `data-src/`) | Dipakai untuk |
|---|------|-------------|----------------------------|---------------|
| 1 | Layout mushaf KFGQPC V4 tajwid, 15 baris | [mushaf-layout/19](https://qul.tarteel.ai/resources/mushaf-layout/19) | `layout/qpc-v4-tajweed-15-lines.db` | Tabel `page_lines` (baris dan rentang kata tiap halaman) |
| 2 | Teks kata V4 (kode glyph) | [quran-script](https://qul.tarteel.ai/resources/quran-script) "V4 Glyphs (With Tajweed) - Word by word" ([47](https://qul.tarteel.ai/resources/quran-script/47)) | `qpc-v4/db/qpc-v4.db` | Tabel `words` |
| 3 | Teks Arab Unicode per ayat (QPC Hafs) | [quran-script](https://qul.tarteel.ai/resources/quran-script), berkas `qpc-hafs.db` (tabel `verses`) | `qpc-hafs/qpc-hafs.db` | Tabel `ayahs` (pencarian, salin, bagikan, TalkBack) |
| 4 | Font halaman V4 tajwid (604 berkas `p1.ttf`..`p604.ttf`) | [font](https://qul.tarteel.ai/resources/font) "V4 tajweed" | `ttf/` | Disalin ke `app/src/main/assets/fonts/` (tidak masuk git) |
| 5 | Font nama surah | [font](https://qul.tarteel.ai/resources/font) `surah_names.ttf` | `surah-names/surah_names.ttf` | Daftar surah |
| 6 | Font header surah berwarna | [font](https://qul.tarteel.ai/resources/font) `QCF_SurahHeader_COLOR-Regular.ttf` | `fonts-extra/` | Bingkai judul surah di halaman |
| 7 | Font Hafs Uthmanic (KFGQPC V22) | [font](https://qul.tarteel.ai/resources/font) `UthmanicHafs_V22.ttf` | `fonts-extra/` | Teks Arab di sheet ayat dan hasil pencarian |
| 8 | Font umum (judul juz, kata pembuka juz) | [font](https://qul.tarteel.ai/resources/font) `quran-common.ttf` | `fonts-extra/` | Judul juz di strip halaman, tab Juz |
| 9 | Metadata: juz, hizb, rub, manzil, ruku, sajda, surah-name | [quran-metadata](https://qul.tarteel.ai/resources/quran-metadata) (tujuh berkas `quran-metadata-*.sqlite`) | `meta/` | Tab Hizb/Manzil/Sajdah, info ayat, tempat dan urutan turun |
| 10 | Info surah Indonesia | [surah-info/454](https://qul.tarteel.ai/resources/surah-info/454) | `surah-info-id/surah-info-id.db` | Layar info surah |
| 11 | Info surah Inggris | [surah-info/3](https://qul.tarteel.ai/resources/surah-info/3) | `surah-info-en/surah-info-en.db` | Layar info surah |
| 12 | Transliterasi Latin (tajwid, simple) | [transliteration/469](https://qul.tarteel.ai/resources/transliteration/469), `english-transliteration-tajweed-simple` | `translit-tajweed-simple/` | Kartu transliterasi di sheet ayat |
| 13 | Terjemahan Indonesia, Kemenag RI (berkas `quran-id-with-footnote-tags`) | [translation/224](https://qul.tarteel.ai/resources/translation/224) ("Indonesian Islamic affairs ministry"; berkas unduhannya bernama `quran-id-...`) | `quran-id-with-footnote-tags/` | Terjemahan |
| 14 | Terjemahan Indonesia, The Sabiq Company | [translation/194](https://qul.tarteel.ai/resources/translation/194) | `the-sabiq-company-with-footnote-tags/` | Terjemahan |
| 15 | Terjemahan Indonesia, King Fahad Quran Complex | [translation/173](https://qul.tarteel.ai/resources/translation/173) | `king-fahad-quran-complex-with-footnote-tags/` | Terjemahan |
| 16 | Terjemahan Inggris, Saheeh International | [translation/193](https://qul.tarteel.ai/resources/translation/193) | `en-sahih-international-with-footnote-tags/` | Terjemahan |

Pilih varian **`with-footnote-tags`** untuk terjemahan (catatan kaki ditandai di teks). Berkas yang pernah diunduh tetapi
**tidak dipakai**: `qpc-v4.json` (sama dengan `qpc-v4.db`), `transliteration-simple.db` (kata menempel),
`pages.zip` (dokumen docx), varian ke-2 `surah_names.ttfv2` (identik dengan yang ada).

## Kapan harus memperbarui

QUL tidak memberi pemberitahuan. Periksa sendiri:

- **Setiap rilis aplikasi** dan **setiap 3 bulan**: buka tiap halaman di tabel, lihat apakah ada versi atau tanggal pembaruan
  baru, atau resource baru yang menggantikan (misalnya font V4 resmi setelah masa proofreading; QUL pernah menandai font V4
  "disabled" saat proofreading).
- **Daftar yang paling mungkin berubah**: nomor 4 (font V4 tajwid, masih dalam proofreading), nomor 1 dan 2 (layout dan kata V4),
  nomor 13 sampai 16 (terjemahan sering dikoreksi), nomor 12 (transliterasi).
- **Bila font halaman V4 berubah**: kode glyph di nomor 2 harus cocok dengan font di nomor 4. Unduh keduanya bersamaan.
- Bila ada resource baru yang ingin dipakai (tafsir, audio, dan sebagainya), tambahkan barisnya di tabel ini.

## Langkah memperbarui

1. **Unduh** berkas yang berubah dari halaman di tabel, ekstrak ke folder `data-src/` yang tercantum.
2. **Bandingkan** dengan versi yang tercatat:
   ```bash
   python tools/data_manifest.py
   ```
   Hasilnya `sama` / `BERUBAH` / `BARU` / `HILANG` per berkas. Bila semuanya `sama`, tidak ada yang perlu dilakukan.
3. **Bangun ulang database** (butuh `pip install fonttools`):
   ```bash
   python tools/build_db.py
   ```
   Skrip berhenti dengan galat bila ada yang janggal (jumlah ayat, tempat turun, terjemahan tidak lengkap).
4. **Salin font** bila nomor 4 sampai 8 berubah: `data-src/ttf/p*.ttf`, `data-src/surah-names/surah_names.ttf`, dan
   tiga berkas `data-src/fonts-extra/*.ttf` ke `app/src/main/assets/fonts/`.
5. **Naikkan `DATA_VERSION`** di `tools/build_db.py` (nilai `PRAGMA user_version`), lalu bangun dan uji di perangkat:
   halaman pertama, halaman dengan banyak surah (604), sheet ayat, pencarian, info surah, dan mode terang dan gelap.
6. **Catat** perubahan sebagai baseline baru, lalu commit `docs/data-manifest.json` bersama `quran.db`:
   ```bash
   python tools/data_manifest.py --write
   ```
7. Rilis dengan tag `vX.Y.Z` (data baru butuh rilis baru karena database dibundel di APK).

## Hal yang perlu diwaspadai

- **Skema berubah**: `build_db.py` membaca nama tabel dan kolom QUL secara langsung (`words`, `pages`, `verses`, `translation`,
  `surah_infos`, `chapters`, dan seterusnya). Bila QUL mengubah skema, skrip akan galat; sesuaikan skripnya.
- **Terjemahan**: dua ayat di King Fahad (1:2 dan 2:194) punya penanda catatan kaki yang rusak di sumber dan diperbaiki manual
  di `TEXT_OVERRIDES`. Bila berkas diperbarui dan sudah benar, hapus koreksinya; bila ada ayat rusak lain, skrip akan galat.
- **Transliterasi**: berkas tajwid-simple berisi dua varian per ayat; yang dipakai varian kedua (gaya baca). Bila urutannya
  berubah, `load_transliteration` akan galat pada pemeriksaan jumlah baris.
- **Lisensi**: font KFGQPC tidak boleh dijual atau diubah. Salinan bertambah palet (di cache perangkat, saat berjalan) tidak
  mengubah berkas aslinya di APK. Atribusi ada di layar Tentang; pertahankan bila font diganti.
- **Audio** (rencana): CDN audio Tarteel (`audio-cdn.tarteel.ai`) menurut dokumentasi QUL bukan dependensi produksi yang
  dijamin; simpan dan layani berkas audio sendiri.
