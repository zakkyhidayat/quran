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
  glyph. Warna medali nomor ayat memakai slot palet tanpa Tajwid pada tema terang dan gelap, sehingga kecerahannya
  konsisten saat Tajwid diaktifkan atau dimatikan. Pewarnaan Tajwid tersedia di tampilan Mushaf; mode Ayat per Ayat
  memakai glyph font halaman yang sama saat tersedia, dengan transisi warna spring saat Tajwid diaktifkan atau dimatikan;
  basmalah pada judul surah juga memakai glyph/font halaman bertajwid dengan transisi yang sama. Teks Arab Unicode dari
  database dan basmalah font umum menjadi fallback bila font halaman belum tersedia.
- Header surah pada mode Ayat per Ayat memakai glyph nama surah seperti sebelumnya, bukan banner mushaf, agar tidak
  berganti tampilan saat font banner dimuat. Glyph nama surah pada daftar/tab memakai hitam di tema terang dan putih di
  tema gelap.
- Langkah setelah unduhan menampilkan pratinjau mushaf, pilihan paket terjemahan, dan pengaturan tampilan.
- Pengaturan > Terjemahan langsung menampilkan katalog paket beserta pencarian di bawah daftar terjemahan aktif,
  tanpa perlu menekan tombol atau membuka dialog. Daftar katalog memakai sisa tinggi layar dan dapat digulir dengan
  gestur sentuh; kolom pencarian tampil segera tanpa menunggu katalog dimuat. Onboarding tetap membatasi tinggi daftar
  agar muat di langkah yang juga dapat digulir.

### Navigasi dan pengaturan

- Layar daftar memakai tab yang dapat dipilih atau digeser; keduanya menggerakkan pager yang sama.
- Perpindahan antar-rute memakai shared-axis X dan dukungan back prediktif melalui `AppNav`.
- Pengaturan utama adalah layar penuh (rute `settings`), sama dengan halaman lainnya, dengan judul dan tombol kembali. Sebelumnya dialog melayang di tengah (margin 16dp); diganti atas permintaan pemilik karena pengaturan bertingkat dengan navigasi ke halaman lain lebih sesuai layar penuh.
  Memilih kategori membuka layar penuh; tombol kembali menutup submenu dan kembali ke layar asal.

Tab **Beranda** ([zakkyhidayat/quran#1](https://github.com/zakkyhidayat/quran/pull/1)):

- Ayat hari ini: tiga potongan kurasi yang dapat digeser (pagi, siang, malam), masing-masing untuk jendela lokal
  delapan jam (04.00–12.00, 12.00–20.00, dan 20.00–04.00). Pilihan stabil selama satu hari dan berganti tengah malam.
- Histori bacaan diletakkan tepat di bawah pager Ayat Hari Ini; maksimal lima surah terakhir tampil.
- Jelajahi memakai label UI terlokalisasi. Data QUL yang tersedia hanya memberi nama topik Inggris dan Arab serta
  deskripsi Inggris; nama Arab dipakai saat bahasa UI Arab, sedangkan bahasa lain memakai fallback Inggris. Tema ayat
  juga hanya tersedia dalam bahasa Inggris. Teks ayat Qur'an tetap Arab.
- Bacaan sunnah dengan dalil; yang waktunya berlaku (Jumat atau malam, pendekatan pukul 18.00) diberi label
  "Dianjurkan sekarang" dan ditaruh di atas.
- Kartu bacaan Sunnah membuka langsung ayat/surah di pembaca. Hanya rangkaian As-Sajdah lalu Al-Mulk yang memakai
  petunjuk mengambang beraksen `tertiaryContainer` ("Lanjut ke Al-Mulk", lalu "Selesaikan bacaan Sunnah"); progres
  tersimpan dan bisa dilanjutkan atau dibuang dari tombol mengambang di daftar. Bacaan lain (Al-Kahfi, Ayat Kursi, akhir
  Al-Baqarah, Tiga Qul, sepuluh ayat Al-Kahfi) dibuka tanpa petunjuk dan tanpa progres tersimpan.
- Pengingat bacaan Sunnah, masing-masing dengan sakelarnya sendiri di Pengaturan > Pengingat (bawaan mati, khusus perangkat
  ini): malam hari pukul 20.30 dengan sakelar terpisah untuk As-Sajdah dan Al-Mulk, Ayat Kursi, dua ayat terakhir
  Al-Baqarah, dan Tiga Qul (satu notifikasi memuat semua yang menyala; ketukan membuka yang pertama menurut urutan itu),
  dan Al-Kahfi setiap Kamis pukul 18.00 (pendekatan awal malam Jumat) membuka Surah Al-Kahfi.
- Kartu "Ayat hari ini" di beranda dibuka bertahap menurut waktu: pagi sejak 04.00, siang sejak 12.00, malam sejak 20.00.
  Kartu yang belum waktunya tidak tampil (penunjuk "n/3" juga disembunyikan sampai ada lebih dari satu kartu); yang sudah
  lewat tetap bisa digeser ke belakang. Hari potongan dimulai pukul 04.00, jadi dini hari masih memakai potongan malam
  sebelumnya. Notifikasinya satu sakelar di Pengaturan > Pengingat (bawaan mati, khusus perangkat ini): potongan yang sama
  dengan kartu, pukul 06.00 (pagi), 13.00 (siang), dan 20.00 (malam); isinya terjemahan aktif (Arab bila belum ada) dan
  ketukan membuka ayatnya dalam mode ayat dan terjemahan. Saat diperluas, notifikasi menampilkan nama surah dan ayat sebagai judul,
  lalu teks Arab dan terjemahan seperti kartu di beranda (mode baca ikut berpindah ke sana dan tetap tersimpan).
- Bingkai nama surah memakai 97% lebar halaman (margin kiri dan kanan masing-masing 1,5%).
- Histori bacaan: satu baris per surah, 5 tampil, langsung setelah Ayat Hari Ini.
- Kumpulan ayat: doa, solusi, adab, dosa besar; layar kumpulan dengan teks Arab, terjemahan, dan "Buka di pembaca".
  Teks Arab memakai glyph mushaf dan mengikuti sakelar tajwid yang sama dengan pembaca (pengaturan satu untuk seluruh
  aplikasi), lengkap dengan legenda warna di atas daftar.
- Pembaca yang dibuka dari kumpulan ayat (ponsel) ditumpuk di atas halaman doa tanpa membuang back stack. Back (tombol,
  ketuk, atau geser) menampilkan dialog di tengah layar: "Kembali ke kumpulan" (ke kumpulan ayat yang dipilih) atau
  "Halaman utama"; ketuk di luar dialog tetap di pembaca. Dari tempat lain, Back tetap satu langkah mundur seperti biasa.
- Kartu Ayat Hari Ini memasangkan latar `primaryContainer` dengan `onPrimaryContainer` di tema terang/gelap; daftar
  Sunnah, histori, dan koleksi memakai wadah ikon tonal dengan pasangan warna yang sesuai.
- Kategori Pengaturan memakai kartu terpisah dengan jarak antarkelompok agar tidak tampak berjejal. Unduhan paket terjemahan
  memakai `DownloadButton` satu-ikon ala Google Translate: ikon unduh, lalu lingkaran progres dengan kotak henti yang
  bisa diketuk untuk membatalkan (berlaku juga untuk pembaruan paket).
- Layar tetap menyala selama membaca (pembaca mushaf, mode daftar ayat/terjemahan, dan halaman kumpulan doa); bisa
  dimatikan di Pengaturan > Tampilan (`keepScreenOn`, bawaan aktif, ikut dalam cadangan).
- Pengingat bacaan harian (juz/hizb/manzil) disembunyikan sementara lewat `Reminder.DAILY_AVAILABLE = false`: tanpa sakelar,
  jam, dan satuan di Pengaturan, tanpa alarm, dan notifikasi lama tidak tampil. Pilihan tersimpan tetap ada.
- Empat kotak koleksi di beranda berada dalam satu item daftar dengan jarak 8 dp ke segala arah (dulu jarak antarbaris hanya
  2 dp), berwarna `surfaceContainer` seperti daftar lain, bentuk `large`, tinggi minimal 112 dp, ikon di atas dan judul di
  bawah.
- Popup mengambang di pembaca dibuat ringkas: baris pilihan counter setinggi 40 dp dengan jarak 3 dp, dan panel panduan tajwid
  berjarak 4 dp antarbaris dengan padding vertikal 8 dp.
- Catatan kaki terjemahan tampil di bawah teksnya di lembar ayat, mode ayat dan terjemahan (juga terjemahan saja), dan
  layar kumpulan doa lewat `FootnoteList`; sebelumnya hanya lembar ayat yang menampilkannya.
- Lompat ke ayat (kartu Ayat hari ini, histori, bookmark, Sunnah) di mode ayat dan terjemahan menggulir ke ayat itu dan
  menyorotnya dengan warna `secondaryContainer`; sorotan hilang saat pengguna mengetuk ayat. Halaman dibuka lebih dulu,
  baru posisi baca disimpan (`AppViewModel.goToAyah`), jadi lompatnya tidak menunggu DataStore.
- Tema dan topik ayat memakai pilihan di Pengaturan > Terjemahan: bahasa aplikasi (bawaan), bahasa sistem, atau English /
  Indonesia. Pilihan "app" dan "system" menampilkan bahasa yang dipakai di bawah judulnya. Bahasa tanpa terjemahan di
  `quran.db` jatuh ke teks Inggris QUL. Dibaca lewat `AppLanguage.extrasLanguage` oleh `MushafRepository`.
- Ketuk kartu "Ayat hari ini" di beranda membuka teks penuhnya: tinggi kartu bertambah bila teks tidak muat (pager
  mengikuti tinggi yang diukur), dan tombol "Buka di pembaca" muncul di tengah bawah kartu. Ketuk lagi untuk menciutkan;
  geser ke kartu lain juga menciutkannya. Tombol itu yang membuka halaman, bukan ketukan kartu.
- Ketuk ayat yang sudah terpilih di mushaf membatalkan pilihannya (sorotan dan lembar ayat tertutup); ketuk ayat lain
  memilihnya. Ketuk latar halaman juga membatalkan, seperti sebelumnya.
- Tautan Notifications ada di Pengaturan > Pengingat dan membuka setelan notifikasi Android; status izin tampil ringkas
  di sisi kanan baris. Sakelar pengingat tidak meminta izin melalui dialog aplikasi. Saat ada progres Sunnah, tombol
  lanjut Sunnah tampil di atas tombol lanjut baca harian; lebarnya mengikuti konten dan tombol tutup berada di dalam
  wadah Sunnah di sisi kanan.
- Baris mushaf mengukur lebar tiap glyph seperti saat menggambar dan menyertakan jarak baris terpusat, sehingga baris
  panjang diperkecil tanpa batas ukuran minimum yang dapat membuat ujung ayat menumpuk di sisi kiri halaman.

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

## Aturan UI yang bisa diperiksa

Acuan prinsip: skill `skydashnet/material-design-3-ui-skill` v1.1.1 (MIT), dipakai sebagai referensi review, bukan
gerbang tunggal. Yang diambil: aksesibilitas sebagai syarat rilis, token semantik, varian kartu eksplisit, dan
anti-pola. Yang ditambahkan untuk repo ini ada di bawah dan diperiksa `tools/check_ui_rules.py`.

- **Warna:** `Color(0x…)` hanya di `ui/theme/`. Komponen memakai peran M3 (`colorScheme.*`). Pelanggaran baru di
  luar tema menggagalkan `--strict`.
- **Ikon:** semua ikon dari `AppIcons` (`ui/Icons.kt`), Material Symbols Rounded bobot 400, dihasilkan `tools/gen_icons.py`
  dari paket npm `@material-symbols/svg-400` (Apache-2.0; petunjuk di kepala skrip). Ikon yang dulu Filled memakai
  varian isi, garis murni sama saja; panah dan chevron berbalik di tata letak kanan-ke-kiri (`autoMirror`). Impor
  `androidx.compose.material.icons` dilarang dan menggagalkan `--strict`; paket `material-icons-core` dilepas dari
  dependensi. Menambah ikon: tambahkan satu baris di `ICONS` dalam skrip lalu jalankan ulang (`--check` memastikan
  berkas sesuai).
- **Nama aksesibel:** ikon yang menjadi satu-satunya isi tombol wajib `contentDescription` berbahasa lokal. Ikon
  dekoratif di samping teks boleh `null`. Ditinjau: tidak ada ikon tanpa nama di dalam tombol atau area yang bisa diketuk
  (pemeriksaan 3 baris ke atas; sampel, bukan bukti lengkap).
- **Target sentuh:** minimal 48dp untuk area sentuh; ikon kecil boleh asal area sentuhnya tetap 48dp.
- **Teks Arab:** tidak memotong teks, tidak mengunci tinggi tetap, dan memakai RTL bawaan Compose untuk teks Arab.
- **Kartu:** tidak membungkus setiap baris atau bagian; kartu hanya untuk unit yang memang terkandung.

- **Gerak:** `ReduceMotion` membaca `ANIMATOR_DURATION_SCALE` (0 bila sistem mematikan animasi). Bila aktif, `motionTween`
  dan `reducedMotion` (di `ui/Motion.kt`) mengganti animasi dengan lompatan instan. Opsi "Reduce motion" di Android 14+
  yang tidak mengubah skala animator belum tercakup; perlu dicek di perangkat.
- **Tafsir (0.7.0):** lembar ayat menampilkan bagian tafsir yang memuat ayat itu, di bawah terjemahan, dengan rentang
  ayatnya. Pilihan tafsir ada di Pengaturan > Terjemahan > Tafsir (tanpa tafsir, atau salah satu paket terpasang).
- **Sunnah sebelum tidur:** satu bacaan, satu sakelar, satu pengingat pukul 20.30. Ketuk membuka halaman kumpulan ayat
  berurutan: Ayat al-Kursi (2:255), 2:285–286, lalu tiga Qul. As-Sajdah dan Al-Mulk tidak termasuk.
- **Bahasa tema dan topik:** hanya Inggris dan Indonesia untuk sekarang; catatan di bawah pilihan menyebutkannya.
- **Tema:** satu tema untuk seluruh aplikasi, tanpa pilihan warna halaman terpisah. Pilihan Sistem, Terang, Gelap ada di
  Pengaturan > Tampilan, dan tombol di bilah atas pembaca memutarnya. Ketukan pertama dari Sistem langsung mengubah layar (ke
  kebalikan tema sistem), lalu ke yang sama dengan sistem, lalu Sistem; satu perpindahan tanpa perubahan layar tidak
  bisa dihindari. Label aksesibilitas menyebut mode sekarang dan berikutnya. Pilihan `page_dark` lama dibuang tanpa
  migrasi atas permintaan pemilik.
- **Pencarian:** hasil memakai daftar tersegmen berwadah tonal (sama dengan Indeks dan Beranda) dengan lencana nomor; keadaan
  kosong memakai ikon di wadah tonal. Audit kedalaman: hanya layar ini yang masih memakai `ListItem` polos; layar lain sudah
  memakai tangga `surfaceContainer*` dan wadah aksen.
- **Cari di Beranda:** ikon cari di bilah atas daftar isi (sebelum Lompat, Acak, Pengaturan) membuka layar pencarian yang
  sama dengan tombol cari di pembaca.
- **Pengaturan (tinjauan screenshot v0.7.7):**
  - Qur'an data: kartu status di atas (memeriksa, terbaru, ada pembaruan, gagal) dengan ikon dan warna kontainer; penjelasan
    di bawah dan lebih kecil. Saat data terbaru, tombol berubah jadi "Periksa lagi" yang aktif, bukan label nonaktif.
  - Izin notifikasi memakai ikon lonceng (bukan alarm). Subjudul ayat hari ini memakai spasi tanpa putus antara nama
    waktu dan jamnya supaya tidak patah di tengah.
  - Cadangan: Ekspor memakai ikon berkas-unggah, Pulihkan memakai ikon pulihkan dan meminta konfirmasi (mengganti pengaturan).
    Waktu "terakhir disimpan" memang jam bila hari ini dan tanggal bila lebih lama.
  - Terjemahan: kolom cari membulat penuh seperti kelompok lain. Tentang: semua baris punya ikon awal.
  - Bahasa aplikasi adalah halaman sendiri (`settings/language`, daftar radio penuh) yang dibuka dari kategori Bahasa di
    Pengaturan utama. Onboarding, yang belum punya halaman Pengaturan, memakai baris yang membuka
    pilihan di tempat.
  - Histori bacaan menulis "Baru saja" untuk bacaan di bawah semenit (DateUtils menulis "0 menit yang lalu").
  - Hapus paket terunduh (terjemahan atau tafsir) meminta konfirmasi, karena berkasnya harus diunduh ulang. Melepas
    terjemahan dari daftar aktif tidak perlu konfirmasi.
  - Belum dikerjakan: wadah untuk daftar unduhan terjemahan, kontras baris abu-abu
    pada latar terang (keputusan tampilan).
- **Carousel jelajah:** tinggi 120dp dikali skala font (minimal 1x), supaya judul dua baris dan jumlah tidak terpotong.

Belum diuji di perangkat dan belum dikompilasi lokal (Gradle tidak jalan di sandbox). Pemeriksaan belum dipasang di CI;
kedua baseline kini kosong, jadi `--strict` bisa langsung dipasang.

## Yang dibutuhkan dari pemilik

- Screenshot aplikasi saat ini: daftar, pembaca, lembar ayat, pengaturan.
- Keputusan urutan: merge PR #1 dulu, lalu prioritas 1 di PR baru.
- Transliterasi dipindah ke Pengaturan > Tampilan; Terjemahan hanya memuat bahasa tema dan daftar terjemahan. Tafsir punya halamannya sendiri di Pengaturan.
- Carousel Jelajahi dipindah dari atas tab ke Beranda (di bawah Ayat Hari Ini, meluber ke tepi layar). Ayat Hari Ini memakai gradien primer-tersier; koleksi ayat berupa kartu grid 2 kolom. Header collapsing dihapus.
