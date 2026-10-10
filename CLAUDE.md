# Panduan untuk Claude Code

Aplikasi Android (Kotlin, Jetpack Compose) untuk membaca mushaf Madani KFGQPC V4, 604 halaman. Detail ada di dokumen
yang sudah ada; baca hanya yang relevan dengan tugas:

- `ARCHITECTURE.md`: alur data, paket, tabel `quran.db`, rendering halaman.
- `docs/DATA_SOURCES.md`: sumber QUL, `tools/build_db.py`, paket terjemahan.
- `docs/adr/`: keputusan yang disengaja (sumber teks, rasm, framework). Jangan dibalik tanpa ADR baru.
- `docs/UI_UX.md`, `docs/LANGUAGE_DATA.md`: catatan kerja per area (keputusan, usulan berprioritas). Perbarui bila
  tugasmu menyelesaikan atau mengubah butirnya; jangan buat file catatan baru untuk serah-terima.
- `docs/AUDIT.md`: temuan terbuka (lisensi, rilis). `docs/CONTENT_ERRORS.md`: bila menemukan galat teks Qur'an.

## Cara kerja

- Satu tugas = satu sesi = satu branch = satu PR. Tugas datang dari GitHub Issues; pekerjaan di luar cakupan dicatat
  sebagai issue baru berlabel `area:*`, bukan dikerjakan sekalian dan bukan ditulis di file catatan.
- Bila tugas mengubah konvensi, arsitektur, atau alur data, perbarui dokumen terkait (dan file ini) di PR yang sama.

## Aturan keras

- **Teks Qur'an tidak pernah diedit tangan.** `app/src/main/assets/quran.db` hanya dihasilkan `tools/build_db.py` dari
  berkas QUL; koreksi hanya lewat `TEXT_OVERRIDES` (terjemahan) dan prosedur `docs/CONTENT_ERRORS.md`.
- `data-src/` dan font halaman `p1..p604.ttf` tidak ada di git. Build debug tetap jalan dengan font bawaan.
- Id paket terjemahan lama di `tools/translation_catalog.json` tidak boleh diubah (pilihan pengguna tersimpan dengan id itu).
- Varian `lite` (`BuildConfig.LITE`) hanya membaca: fitur baru harus disembunyikan di sana, dan izin baru dibuang di
  `app/src/lite/AndroidManifest.xml`. Internet di lite hanya untuk pembaru. Lihat `docs/FORK_LITE.md`.
- Teks antarmuka hanya lewat string resource; setiap kunci baru harus ada di semua bahasa (`check_strings.py`).

## Pemeriksaan sebelum push (sama dengan CI)

```bash
python tools/verify_quran.py
python tools/check_strings.py
./gradlew --no-daemon :app:testGithubDebugUnitTest :app:lintGithubDebug :app:assembleGithubDebug
```

## Bahasa

Pesan commit, komentar kode, dan dokumen internal (`docs/`) dalam bahasa Indonesia. `README.md`, `ARCHITECTURE.md`,
`THIRD_PARTY_NOTICES.md`, dan `PRIVACY.md` dalam bahasa Inggris.
