# Rilis

Rilis dibangun oleh GitHub Actions ([`.github/workflows/release.yml`](../.github/workflows/release.yml)) saat tag
`vX.Y.Z` di-push. Workflow menerbitkan APK bertanda tangan ke GitHub Releases (varian `github` dan Quran Lite), serta
menyimpan AAB bertanda tangan dari varian `play` sebagai artefak workflow untuk Play Console.

## Varian build

Tidak ada varian yang memperbarui dirinya sendiri; pengguna mengunduh versi baru dari GitHub Releases atau Google Play.
Full dan Quran Lite dibangun dari modul `:app` yang sama dan memakai implementasi pembaca mushaf serta tajwid bersama.

| Varian | Aplikasi | Penggunaan |
|--------|----------|------------|
| `github` | Full (`io.zakkyhidayat.quran`) | `Quran.apk` di GitHub Releases |
| `play` | Full, kode yang sama dengan `github` | AAB untuk Google Play |
| `lite` | Hanya membaca (`io.zakkyhidayat.quran.lite`) | `Quran-Lite.apk` di rilis GitHub yang sama, ditandatangani dengan kunci yang sama |

Kedua APK tidak menyertakan `quran.db` dan 604 font halaman. Saat pertama dibuka, atau ketika pengguna memilih
perbaikan/pembaruan data di Pengaturan, aplikasi mengunduh database sesuai flavor dan arsip font halaman bersama dari
rilis stabil `quran-data`. Aplikasi memeriksa ukuran, SHA-256, integritas database, dan seluruh font sebelum menyimpan
data di penyimpanan privat agar dapat dibaca secara luring. Membaca belum tersedia sebelum database dan font terpasang.
Rilis versi aplikasi hanya berisi dua APK; AAB Play tetap menjadi artefak workflow.

### Target build lokal

Jalankan dari root repositori:

| Perintah | Output |
|----------|--------|
| `./gradlew :app:assembleFullDebug` | APK Full debug: `app/build/outputs/apk/github/debug/app-github-debug.apk` |
| `./gradlew :app:assembleLiteDebug` | APK Quran Lite debug: `app/build/outputs/apk/lite/debug/app-lite-debug.apk` |
| `./gradlew :app:assembleBothDebug` | Kedua APK debug |
| `./gradlew :app:assembleFullRelease` | APK Full release |
| `./gradlew :app:assembleLiteRelease` | APK Quran Lite release |
| `./gradlew :app:assembleBothRelease` | Kedua APK release |
| `./gradlew :app:bundleFullPlayRelease` | AAB Full Google Play: `app/build/outputs/bundle/playRelease/app-play-release.aab` |

Tambahkan `--parallel` pada perintah multi-target seperti `assembleBothDebug` untuk mengaktifkan eksekusi paralel Gradle
bila didukung. Gunakan task flavor standar (`assembleGithubDebug`, `assemblePlayDebug`, dan `assembleLiteDebug`) bila
ingin membangun flavor distribusi tertentu secara langsung.

README menautkan langsung ke `releases/latest/download/Quran.apk` dan `Quran-Lite.apk`, jadi nama APK tidak memuat
versi. Pastikan `quran-data` dan `translations` berstatus **pre-release** agar tidak mengubah rilis aplikasi `latest`.
Rilis bernama tersebut adalah kanal unduhan untuk data Qur'an dan paket terjemahan.

## Persiapan satu kali

### 1. Kunci penandatanganan rilis

Buat kunci sekali dan simpan dengan aman (pengelola kata sandi serta salinan luring). Jika hilang, aplikasi terpasang
tidak dapat diperbarui; pengguna harus menghapus lalu memasangnya kembali.

```bash
keytool -genkeypair -v -keystore quran-release.jks -alias quran -keyalg RSA -keysize 4096 -validity 36500
```

Tambahkan secret repositori berikut (Settings → Secrets and variables → Actions):

| Secret | Nilai |
|--------|-------|
| `RELEASE_KEYSTORE_BASE64` | `base64 -w0 quran-release.jks` (Windows Git Bash: `base64 -w0`; PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("quran-release.jks"))`) |
| `RELEASE_KEYSTORE_PASSWORD` | Kata sandi keystore |
| `RELEASE_KEY_ALIAS` | `quran` (atau alias yang dipilih) |
| `RELEASE_KEY_PASSWORD` | Kata sandi kunci |

Jangan pernah commit keystore; `*.jks` tercantum di `.gitignore`.

### 2. Rilis data Qur'an

604 font halaman tidak disimpan di Git. Buat pre-release stabil `quran-data` satu kali dengan arsip database,
`quran-data.json`, dan `page-fonts.zip` yang tidak dimodifikasi. Workflow `Qur'an data` mengganti aset di rilis tersebut
(`--clobber`) setelah pembaruan yang ditinjau pada `app/src/main/assets/quran.db`; arsip font yang tersedia diunduh ulang
kecuali aset font diperbarui secara terpisah:

```bash
gh release create quran-data quran-full.db.gz quran-lite.db.gz page-fonts.zip quran-data.json \
  --prerelease --title "Qur'an data" --notes "Stable download channel for the Quran database and page fonts."
```

Untuk memperbarui database setelah pembaruan QUL, perbarui dan verifikasi database sumber yang di-commit, lalu jalankan
Actions → `Qur'an data` → Run workflow pada `main`. Workflow membuat ulang database Lite, memeriksa kedua database, dan
mengganti empat aset tanpa membuat entri rilis baru. Untuk memperbarui font halaman, ganti `page-fonts.zip` di rilis
`quran-data` terlebih dahulu, lalu jalankan workflow.

## Membuat rilis

1. Pastikan `main` berhasil dibangun dan data sudah terbaru (lihat [DATA_SOURCES.md](DATA_SOURCES.md)).
2. Mulai rilis dengan salah satu cara:
   - **Dari GitHub:** Actions → Release → Run workflow (branch `main`), masukkan versi baru, misalnya `v0.3.0`.
     Workflow memberi tag pada commit `main` terbaru lalu membangun rilis.
   - **Dari komputer:**
     ```bash
     git tag v0.3.0
     git push origin v0.3.0
     ```
3. Workflow membuat rilis GitHub `v0.3.0` yang memuat `Quran.apk` dan `Quran-Lite.apk`, beserta catatan rilis otomatis.
   Database dan font tetap berada di rilis stabil `quran-data` dan dapat diperbarui tanpa menerbitkan APK baru.
4. Unduh artefak `play-bundle` dari workflow untuk diunggah ke Play Console.

`versionCode` dihitung sebagai `X*10000 + Y*100 + Z`, jadi setiap tag baru harus lebih tinggi daripada tag sebelumnya.
