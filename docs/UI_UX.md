# Catatan UI/UX

Ringkasan diskusi UI/UX (Oktober 2026).

**Batasan analisis:** catatan ini disusun dari membaca kode dan screenshot aplikasi pembanding, belum dari aplikasi ini
yang berjalan di perangkat. Setiap butir perlu dicek di perangkat sebelum dan sesudah dikerjakan.

## Pembanding: QuranApp (AlfaazPlus)

Dari screenshot [QuranApp](https://github.com/AlfaazPlus/QuranApp) (GPL-3.0, sama dengan aplikasi ini):

| Fitur | Keputusan | Alasan |
|---|---|---|
| Ayat hari ini di beranda | Ditiru, dengan filter | Ayat acak bisa terpotong maknanya; dipakai potongan 1-4 ayat dari koleksi kurasi |
| Histori bacaan | Ditiru | Sebelumnya hanya ada satu posisi "lanjutkan membaca" |
| Bacaan sesuai waktu (Al-Kahfi Jumat, perlindungan malam) | Ditiru dan diperluas | Ditambah bacaan sunnah lain beserta dalilnya |
| Kartu koleksi: doa, solusi, adab, dosa besar | Ditiru | Data rujukan dari repo QuranApp |
| Kartu Nabi dan Rasul, Doa para Nabi | Ditunda | Dataset terpisah, belum diperiksa |
| Kartu Quran dan Sains | Tidak diambil | Tafsir ilmi dipersoalkan sebagian ulama; isinya kurasi QuranApp sendiri |
| Penjelajah Topik | Sudah ada (Jelajahi) | Data QUL sudah ada di `quran.db` |
| Kata per kata, murattal, mini player | Ditunda | Fitur besar, sesi tersendiri |
| Pilihan skrip mushaf, sumber unduhan | Tidak diambil | Bertentangan dengan prinsip satu gaya halaman (README) |
| Menu promosi aplikasi lain, beri rating | Tidak diambil | Khusus aplikasi mereka |

## Sudah dikerjakan

### Onboarding dan database

- Pemakaian pertama dimulai dengan salam, pilihan bahasa antarmuka, dan validasi versi database mushaf
  serta paket 604 font halaman terhadap katalog GitHub `quran-data`. Data baru atau usang baru diunduh; versi lokal yang
  sama dengan rilis tidak diunduh ulang. Ukuran total, progres gabungan, kegagalan, dan coba lagi ditampilkan; database
  dan font diperiksa sebelum mushaf dibuka. Data lokal dapat diperbarui manual dari Pengaturan, dan mushaf tetap dapat
  dibaca luring saat data terpasang.
- Pembaca mushaf menempatkan Tajwid di kiri dan dropdown counter di kanan pada strip di bawah bilah atas; counter
  memiliki enam pilihan (ayat, juz, hizb, rub', manzil, ruku). Pilihan ayat menampilkan hitungan ayat/total surah.
  Kedua menu memakai permukaan tonal membulat dan animasi spring; dropdown counter menampilkan enam pill tanpa wadah
  latar tambahan, lebar menu dipersempit, dan tombolnya berlebar tetap mengikuti label terpanjang dengan indikator
  progres melingkar. Setiap item counter menampilkan progres dalam lingkaran, nama mode, serta hitungan saat ini/total.
  Panel legenda Tajwid dan sakelar warna muncul di bawah tombol. Trio mode baca selalu tampil sebagai bilah bawah agar
  mode mushaf dan daftar ayat dapat dipilih kapan saja. Baris mushaf diberi sela vertikal 1,5 dp
  (sekitar 4–5% tinggi sel per baris) tanpa mengecilkan glyph; tinggi halaman bertambah secukupnya, dan lebar
  disesuaikan agar tetap muat di layar portrait. Perubahan warna Tajwid memakai transisi spring tanpa mengubah tata letak
  glyph. Warna medali nomor ayat tetap sama saat Tajwid diaktifkan atau dimatikan, termasuk pada tema gelap.
- Langkah setelah unduhan menampilkan pratinjau mushaf, pilihan paket terjemahan, dan pengaturan tampilan.
- Pengaturan > Terjemahan menampilkan katalog paket langsung di bawah tombol **Tambah Terjemahan**. Daftar aktif tetap
  berada di atas tombol; katalog yang dikembangkan menyediakan pencarian dan daftar lazy yang dapat digulir agar banyak
  paket tetap mudah dijelajahi tanpa dialog atau jendela mengambang.

### Navigasi dan pengaturan

- Layar daftar memakai tab yang dapat dipilih atau digeser; keduanya menggerakkan pager yang sama.
- Perpindahan antar-rute memakai shared-axis X dan dukungan back prediktif melalui `AppNav`.
- Menu utama Pengaturan tampil sebagai dialog melayang di tengah (margin 16dp, lebar maks 560dp, sudut 28dp), bukan bottom sheet. Evaluasi Material 3: bottom sheet cocok untuk aksi kontekstual singkat; daftar pengaturan bertingkat dengan navigasi ke halaman lain lebih sesuai dialog/layar penuh, dan dialog yang diperkecil menjaga konteks pembaca tetap terlihat.
  Memilih kategori membuka layar penuh; tombol kembali menutup submenu dan kembali ke layar asal.

Tab **Beranda** ([zakkyhidayat/quran#1](https://github.com/zakkyhidayat/quran/pull/1)):

- Ayat hari ini: sama untuk semua pengguna per hari, berganti tengah malam.
- Bacaan sunnah dengan dalil; yang waktunya berlaku (Jumat atau malam, pendekatan pukul 18.00) diberi label
  "Dianjurkan sekarang" dan ditaruh di atas.
- Histori bacaan: satu baris per surah, 5 tampil.
- Kumpulan ayat: doa, solusi, adab, dosa besar; layar kumpulan dengan teks Arab, terjemahan, dan "Buka di pembaca".

Perlu dicek pemilik sebelum rilis: redaksi hadits di bacaan sunnah, dan tampilan di perangkat.

## Usulan berikutnya


### Prioritas 1: navigasi

- Layar daftar memuat 8 tab geser (Bookmark, Beranda, Surah, Juz, Hizb, Rub', Manzil, Ruku) dan 4 ikon tanpa label
  (Jelajahi, Lompat ke ayat, Ayat acak, Pengaturan). Sulit ditebak oleh pengguna baru.
- Usulan: bar navigasi bawah **Beranda · Daftar Isi · Cari · Bookmark**.
  - Daftar Isi: Surah dan Juz; Hizb, Rub', Manzil, Ruku digabung ke satu pilihan "Pembagian lain".
  - "Lanjutkan membaca" pindah dari FAB ke kartu di Beranda (FAB sekarang menutupi daftar, perlu ruang bawah 96dp).
- Pencarian bisa dibuka dari layar utama, tidak hanya dari pembaca.
- Risiko: mengubah navigasi yang sudah dikenal pengguna lama; menyentuh file yang sama dengan PR #1, jadi dikerjakan
  setelah PR itu di-merge.

### Prioritas 2: pembaca

- Known issue: animasi geser halaman kadang hilang (halaman mengecil lalu hilang). Perlu diuji di perangkat.
- Saat ayat dimuat hanya tampil ruang kosong 48dp; ganti dengan placeholder berbentuk baris teks agar halaman tidak
  melompat.

### Prioritas 3: Beranda

- Kartu ayat hari ini belum punya tombol bagikan dan bookmark.
- Bacaan sunnah (6 butir) memanjangkan beranda; tampilkan yang sedang dianjurkan, sisanya di balik "Lihat semua".

### Prioritas 4: aksesibilitas (dari AUDIT.md)

- Uji dengan TalkBack sungguhan.
- Uji dengan ukuran font sistem besar.
- Tes UI dasar otomatis: buka, pilih surah, geser, cari, bookmark.

## Yang dibutuhkan dari pemilik

- Screenshot aplikasi saat ini: daftar, pembaca, lembar ayat, pengaturan.
- Keputusan urutan: merge PR #1 dulu, lalu prioritas 1 di PR baru.
- Transliterasi ada di Pengaturan > Terjemahan (bukan Tampilan).
- Carousel Jelajahi dipindah dari atas tab ke Beranda (di bawah Ayat Hari Ini, meluber ke tepi layar). Ayat Hari Ini memakai gradien primer-tersier; koleksi ayat berupa kartu grid 2 kolom. Header collapsing dihapus.
