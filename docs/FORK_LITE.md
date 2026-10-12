# Varian Full dan Quran Lite

Quran Full dan Quran Lite adalah dua jenis output dari satu modul Android, `:app`. Keduanya memakai sumber mushaf,
render halaman, dan tajwid yang sama; Lite bukan fork dan tidak memiliki salinan kode pembaca sendiri. Pemisahan
perilaku dilakukan melalui flavor Gradle, `BuildConfig.LITE`, manifest Lite, dan resource khusus Lite.

## Perbedaan produk

| Area | Quran Full | Quran Lite |
|------|------------|------------|
| Tujuan | Membaca mushaf dan fitur pendamping | Membaca mushaf dengan fitur minimal |
| Halaman utama | Beranda, bookmark, indeks, Jelajahi, dan fitur Full lainnya | Bookmark, Surah, Juz, Hizb, Rub', Manzil, dan Ruku (tanpa Beranda) |
| Pembaca | Mushaf, ayat + terjemahan, atau terjemahan saja | Mushaf saja; toggle warna tajwid tetap tersedia |
| Fitur ayat | Terjemahan, transliterasi, pencarian, berbagi, dan fitur terkait | Bookmark ayat |
| Pengaturan | Bahasa, tema, data, terjemahan, cadangan, pengingat, dan pengaturan Full lain | Tema, pemeliharaan data, cadangan, dan informasi aplikasi |
| Data Qur'an | Database Full | Database Lite yang lebih kecil; teks mushaf sama |
| Jaringan | Unduh database/font, paket terjemahan, dan fitur Full tertentu | Hanya unduh atau perbarui database/font |
| Izin | Izin yang diperlukan fitur Full | Izin notifikasi dan boot tidak disertakan |

Saat pertama dibuka, kedua aplikasi meminta bahasa dan mengunduh database serta 604 font halaman. Di Full, penyiapan
dilanjutkan dengan pratinjau mushaf, pilihan terjemahan, dan tampilan. Lite langsung masuk ke aplikasi setelah bahasa
dan data siap. Setelah data siap, keduanya dapat membaca secara luring. Lite menyimpan bookmark dan pengaturan di
ruang data aplikasinya sendiri.

## Cadangan bersama Full dan Lite

Kedua aplikasi punya `applicationId` berbeda, jadi data lokalnya terpisah. Bookmark berpindah lewat berkas cadangan
JSON v2 yang netral edisi:

- Bookmark, penanda hapus (tombstone), dan posisi baca dipakai bersama. Gabungan berbasis waktu: yang terbaru menang,
  sehingga bookmark yang dihapus di satu edisi tidak muncul lagi dari berkas.
- Pengaturan disimpan per profil (`full`, `lite`). Tiap edisi hanya menerapkan profilnya sendiri dan mempertahankan
  profil edisi lain saat menulis ulang berkas.
- Berkas v1 lama tetap bisa diimpor (di Lite hanya bookmark dan posisi baca).
- Cadangan otomatis aktif di kedua edisi. Pilih folder yang sama di keduanya melalui pemilih folder Android; setiap
  perubahan tersimpan sebagai snapshot baru bernama `quran-autobackup-<timestamp>.json`. Folder mempertahankan maksimal
  tiga snapshot terbaru dan menghapus yang lebih lama.
- Data dari snapshot yang ada digabungkan sebelum snapshot baru dibuat, agar perubahan dari Full dan Lite ikut terbawa.
  Snapshot tetap bisa dipulihkan melalui menu Impor dengan memilih salah satu berkas JSON di folder tersebut.
- Jika sebelumnya auto-backup memakai satu berkas dari versi lama, matikan auto-backup lalu pilih folder. Berkas lama
  tidak dihapus dan masih dapat dipulihkan lewat Impor manual.
- Impor manual selalu memulihkan bookmark aktif di berkas, meski pernah dihapus secara lokal.

## Struktur kode dan flavor

- `app/src/main`: implementasi bersama, termasuk pembaca mushaf, model/data inti, tajwid, dan string umum.
- `app/src/lite`: manifest, nama aplikasi, dan ikon khusus Lite.
- `BuildConfig.LITE`: mengatur titik perilaku yang berbeda, seperti tab/fitur yang tersedia dan mode pembacaan.
- `github`: Full APK untuk GitHub Releases, dengan ID `io.zakkyhidayat.quran`.
- `play`: Full AAB untuk Play Store, dengan kode aplikasi yang sama dengan Full.
- `lite`: Quran Lite APK dengan ID `io.zakkyhidayat.quran.lite`.

Semua flavor memakai satu modul dan berbagi implementasi mushaf. Perubahan fitur baru perlu ditinjau terhadap Full dan
Lite: fitur pembacaan yang sama sebaiknya tetap di jalur bersama, sedangkan fitur Full harus disembunyikan atau tidak
diaktifkan di Lite. Jangan menambahkan izin Lite tanpa memastikan izin tersebut memang diperlukan.

Database Full berasal dari `app/src/main/assets/quran.db`. `tools/build_lite_db.py` membuat database Lite tanpa tabel
penjelajahan, info surah, dan transliterasi. Database dan font halaman diunduh saat penyiapan, bukan dibundel di APK.
Katalog unduhan memilih database berdasarkan flavor; kedua aplikasi memakai arsip font halaman yang sama. Lihat
[`DATA_SOURCES.md`](DATA_SOURCES.md) untuk sumber dan proses pembaruan data.

## Memilih output lokal

Dari root repositori, gunakan task Gradle berikut:

| Perintah | Output |
|----------|--------|
| `./gradlew :app:assembleFullDebug` | APK Full debug |
| `./gradlew :app:assembleLiteDebug` | APK Lite debug |
| `./gradlew :app:assembleBothDebug` | Kedua APK debug |
| `./gradlew :app:assembleFullRelease` | APK Full release |
| `./gradlew :app:assembleLiteRelease` | APK Lite release |
| `./gradlew :app:assembleBothRelease` | Kedua APK release |
| `./gradlew :app:bundleFullPlayRelease` | AAB Full untuk Google Play |

APK ada di `app/build/outputs/apk/<flavor>/<build-type>/`; AAB Play ada di
`app/build/outputs/bundle/playRelease/`. Task alias mempertahankan nama task flavor standar, jadi CI dan workflow rilis
tetap dapat memilih `github`, `play`, dan `lite` secara eksplisit. Pada build beberapa target, `--parallel` dapat
diaktifkan bila didukung oleh Gradle dan lingkungan build.

Detail artefak rilis dan penandatanganan: [`RELEASING.md`](RELEASING.md).
