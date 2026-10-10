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

## Sumber opsional: penjelajahan tematik

Lima sumber QUL untuk fitur Jelajahi (topik, tema ayat, ayat serupa, mutasyabihat, morfologi). **Opsional**: tanpa foldernya,
`build_db.py` tetap menghasilkan `quran.db` tanpa tabel ini dan aplikasi menyembunyikan fiturnya. `quran.db` yang di-commit
**sudah berisi** kelimanya. Untuk memperbarui, ambil dari halaman berikut (butuh login), simpan di `data-src/`, lalu jalankan
`python tools/build_db.py` (atau `--explore-only`, lihat di bawah). `data_manifest.py` menampilkan yang tidak ada sebagai `BELUM DIUNDUH`.

| # | Data | Halaman QUL | Simpan ke (di `data-src/`) | Tabel di `quran.db` |
|---|------|-------------|----------------------------|---------------------|
| 17 | Topik dan konsep (2.512 topik, hubungan induk-anak, ontologi dan tematik) | [ayah-topics/45](https://qul.tarteel.ai/resources/ayah-topics/45), SQLite | `topics/` | `topics`, `topic_ayahs` |
| 18 | Tema ayat (kelompok ayat berurutan dengan satu tema) | [ayah-theme/62](https://qul.tarteel.ai/resources/ayah-theme/62), SQLite | `ayah-theme/` | `ayah_themes` |
| 19 | Ayat serupa | [similar-ayah/74](https://qul.tarteel.ai/resources/similar-ayah/74), SQLite (atau JSON; hanya SQLite yang dibaca) | `similar-ayah/` | `similar_ayahs` |
| 20 | Mutasyabihat (frasa mirip) | [mutashabihat/73](https://qul.tarteel.ai/resources/mutashabihat/73), JSON (zip berisi `phrases.json` dan `phrase_verses.json`) | `mutashabihat/` (ekstrak zip) | `mutashabihat`, `mutashabihat_ayahs` |
| 21 | Akar kata per kata | [morphology/76](https://qul.tarteel.ai/resources/morphology/76), SQLite | `morphology/` | `morph_roots`, `word_morph.root_id` |
| 22 | Lema per kata | [morphology/75](https://qul.tarteel.ai/resources/morphology/75), SQLite | `morphology/` | `morph_lemmas`, `word_morph.lemma_id` |
| 23 | Stem per kata | [morphology/77](https://qul.tarteel.ai/resources/morphology/77), SQLite | `morphology/` | `morph_stems`, `word_morph.stem_id` |

Folder `morphology/` boleh berisi beberapa berkas (satu per jenis); `build_db.py` mengenali tabelnya dari nama (`roots`/`word_roots`,
`lemmas`/`word_lemmas`, `stems`/`word_stems`).

Skema yang dibaca skrip (dari berkas asli; nama kolom alternatif juga diterima, lihat `tools/build_db.py`):

- **Topik** `topics/topics.db`: tabel `topics` (`topic_id`, `name`, `arabic_name`, `parent_id`, `thematic_parent_id`,
  `ontology_parent_id`, `description` HTML dengan `<topic data-id>`, `thematic`, `ontology`, `ayahs` berupa daftar `1:1, 1:2`,
  `related_topics`). Deskripsi disimpan sebagai teks polos; tautan topik masuk ke tabel `topic_links`.
- **Tema ayat**: kolom `theme`, `surah_number`, `ayah_from`/`from_ayah`, `ayah_to`/`to_ayah`, `keywords`, `total_ayahs`.
- **Ayat serupa** `similar-ayah/matching-ayah.db`: tabel `similar_ayahs` (`verse_key`, `matched_ayah_key`, `matched_words_count`,
  `coverage`, `score`, `match_words_range` berbentuk `[[5,8]]`).
- **Mutasyabihat**: `phrases.json` = objek id frasa, berisi `surahs`, `ayahs`, `count`, `source` (`key`, `from`, `to`) dan `ayah`
  (kunci ayat ke daftar `[awal, akhir]`, indeks kata 1-based inklusif). `phrase_verses.json` (indeks balik) tidak dipakai.
- **Akar/lema/stem** `morphology/word-root.db`, `word-lemma.db`, `word-stem.db`: tabel induk (`roots`/`lemmas`/`stems`) dan tabel
  kata `root_words`/`lemma_words`/`stem_words` (`word_location` `surah:ayah:kata`).
- **Jenis kata (POS)**: ada di [morphology/78](https://qul.tarteel.ai/resources/morphology/78) ("Word Morphology"), tetapi halaman itu
  menjawab galat 500 saat dibaca sehingga skemanya belum diketahui. Kolom `word_morph.pos` sengaja dibiarkan kosong.

Skrip berhenti dengan galat bila tabel yang diharapkan tidak ada di berkas. Naikkan `DATA_VERSION` setelah menambah data ini.

### Membangun hanya tabel penjelajahan

Bila sumber dasar (layout, qpc-hafs, meta, dan lain-lain) tidak ada di `data-src/`, jangan bangun ulang seluruhnya:

```bash
python tools/build_db.py --explore-only   # buka quran.db yang ada, buang dan isi ulang hanya tabel penjelajahan, VACUUM
python tools/verify_quran.py
```

Tabel dasar tidak disentuh. Ini juga menaikkan `user_version` ke `DATA_VERSION`.

## Paket terjemahan unduhan

Tidak ada terjemahan yang dibundel (tabel terjemahan di `quran.db` sengaja kosong); aplikasi mengunduh semuanya sebagai paket,
sekitar 170 terjemahan dalam banyak bahasa (termasuk empat terjemahan bawaan nomor 13 sampai 16). QUL tidak punya API dan butuh login, jadi aplikasi tidak mengunduh dari QUL. Pemelihara mengunduh berkas QUL secara
manual, mengubahnya jadi paket kecil, lalu menaruhnya di GitHub Releases (tag `translations`). Aplikasi membaca `catalog.json`
dari rilis itu (alamatnya `TRANSLATION_CATALOG_URL` di `app/build.gradle.kts`), mengunduh paket, memeriksa sha256 dan jumlah
ayat (6236), lalu menyimpannya di `filesDir/translations/`. Dialog "Tambah terjemahan" mengelompokkan paket per bahasa dan
punya kolom cari (bahasa atau penerjemah).

Alurnya, semuanya lokal sampai langkah unggah:

```
qul_catch.py  ->  qul_translation_meta.py  ->  qul_translation_meta.py --footnotes  ->  build_translation_packs.py  ->  unggah
(tangkap)         (pecah varian simple)         (pecah varian catatan kaki)              (bangun paket)
```

1. **Tangkap unduhan.** Unduh tiap terjemahan dari <https://qul.tarteel.ai/resources/translation> lewat browser (perlu login),
   varian `simple` ke `data-src/translation/` dan varian `with-footnote-tags` ke `data-src/translation-footnote/`:
   `python tools/qul_catch.py translation` (lihat `--help`; untuk folder catatan kaki simpan manual atau pindahkan hasilnya).
2. **Pecah per terjemahan.** Server QUL memberi nama berkas sama untuk terjemahan berjudul sama dan unduhan berikutnya
   BERISI unduhan sebelumnya (ditambahkan di belakang; contoh: `montada-islamic-foundation-with-footnote-tags-1.db` berisi
   Prancis dan Spanyol). `python tools/qul_translation_meta.py` memecah tiap berkas menjadi blok, mencocokkan tiap blok ke id
   resource QUL lewat teks pratinjau (cache halaman di `data-src/.qul-cache/`, `--offline` bila cache sudah lengkap), lalu
   menulis `data-src/translation-split/<id>-<slug>.db` dan `index.json` (id, judul, bahasa, deskripsi, jumlah baris,
   `complete`).
3. **Pecah varian catatan kaki.** `python tools/qul_translation_meta.py --footnotes` (setelah langkah 2) mencocokkan tiap blok
   di `data-src/translation-footnote/` ke id yang sama dengan membandingkan teksnya (tag catatan kaki dibuang) dengan berkas
   simple hasil langkah 2, lalu menulis `data-src/translation-footnote-split/<id>-<slug>.db` (kolom `footnotes` dipertahankan)
   dan `index.json`.
4. **Katalog kurasi.** `tools/translation_catalog.json` (masuk git) adalah sumber kebenaran: untuk tiap terjemahan ada
   `pack_id` (`<bahasa>-<slug>`), `lang` (ISO 639-1; 639-3 bila tak punya 639-1, mis. `mos`, `yao`, `luy`, `mdh`), `lang_name`
   (nama Inggris untuk kode 639-3, dipakai aplikasi bila `Locale` tak mengenalnya), `name`, `author`, `qul`, `variant`
   (`footnote` bila ada, selain itu `simple`) dan `source`. Bahasa dari daftar QUL sering kosong atau keliru (misalnya "Malay"
   untuk terjemahan Malayalam, "Turkmen" untuk Cebuano), jadi bahasa ditetapkan manual dari judul dan isi teks.
   Entri dengan kunci `excluded` (disertai alasan) tidak dibangun: terjemahan tidak lengkap (baris di bawah 6236), terlalu
   banyak ayat kosong, atau berkas yang tak bisa dicocokkan ke satu id QUL. `skip` menandai yang memang tak bisa diunduh
   (`en-khattab`, QUL 426). Id paket lama (`id-kemenag`, `id-sabiq`, `id-kfqpc`, `en-sahih`, `en-yusufali`, `ur-jalandhari`,
   `bn-mujibur`, `tr-diyanet`, `fa-islamhouse`, `ms-basmeih`, `fr-hamidullah`, `ru-kuliev`) TIDAK BOLEH diubah karena pilihan
   pengguna tersimpan dengan id itu.
5. **Bangun paket.**

```bash
python tools/build_translation_packs.py              # semua paket di katalog; atau sebut id: ... ur-jalandhari id-kemenag
# hasil: build/translation-packs/<id>.db dan catalog.json (tidak masuk git)
```

   Sumber dibaca dari folder pecahan (`translation-footnote-split/` untuk varian `footnote`, selain itu `translation-split/`).
   Skrip memeriksa 6236 ayat unik per paket (kunci ayat dari `ayah_key`, karena kolom `sura`/`ayah` rusak di beberapa
   berkas), mengizinkan paling banyak 10 ayat kosong di sumber (dicatat sebagai `empty_ayah` di `catalog.json`), dan melaporkan
   paket yang gagal. Format `catalog.json` tidak berubah (`id`, `lang`, `name`, `source`, `file`, `bytes`, `sha256`, `version`);
   ditambah `qul`, serta `lang_name` dan `empty_ayah` bila ada (diabaikan aplikasi lama).
6. **Unggah** (hanya bila sudah siap dirilis):

```bash
# pertama kali: buat rilis (tag translations)
gh release create translations build/translation-packs/* --title "Paket terjemahan" --notes "Paket terjemahan unduhan untuk aplikasi"
# pembaruan berikutnya
gh release upload translations build/translation-packs/* --clobber
```

Untuk menambah paket baru: unduh dan pecah (langkah 1 sampai 3), tambahkan entrinya di `tools/translation_catalog.json`, bangun,
lalu unggah ulang `catalog.json`. Naikkan `PACK_VERSION` di `tools/build_translation_packs.py` bila isi paket yang sama berubah.

**Atribusi.** Hak cipta tiap terjemahan ada pada penerbit atau penerjemahnya (tercantum di `author` katalog dan nama paket di
aplikasi), diambil lewat QUL. Lihat `THIRD_PARTY_NOTICES.md`; izin redistribusi belum dipastikan (lihat `docs/AUDIT.md`).

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
