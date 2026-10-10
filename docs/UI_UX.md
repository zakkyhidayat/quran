# Catatan UI/UX

Ringkasan diskusi UI/UX (Oktober 2026). Bahasa dan database dibahas di dokumen/sesi terpisah.

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
