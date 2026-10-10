# Changelog

Format: [Keep a Changelog](https://keepachangelog.com). Versions follow semantic versioning (`vX.Y.Z` git tags).

## [Unreleased]

First public release candidate.

### Removed
- In-app updates (added in 0.2.0). New versions are downloaded from GitHub Releases or Google Play.

### Added
- Quran Lite: a separate reading-only app from the same code, without internet access (`quran-lite-vX.Y.Z.apk`).

### Added
- Madani mushaf (KFGQPC V4, 604 pages, 15 lines) with tajweed colours, light, dark, and AMOLED themes, dynamic colour,
  and three contrast levels.
- Reading modes: mushaf page, ayah with translation, translation only, one page at a time.
- Ayah sheet with translations, footnotes, transliteration, and ayah position; copy and share.
- Indexes by surah, juz, hizb, rub', manzil, and ruku; surah info; go to ayah; random ayah; continue reading.
- Page and ayah bookmarks; search by Arabic text, translation, surah name, or reference.
- Downloadable translations (Indonesian: Kemenag, The Sabiq Company, King Fahad Quran Complex; English: Saheeh
  International).
- Interface in 10 languages, following the device language.
- Onboarding for new users.
- Backup and restore to a JSON file, and auto backup to a chosen file.
- Daily reading reminder: a notification at a chosen time with the day's juz, hizb, or manzil and its ayah range; tapping
  it opens the start of that portion. Asks for notification permission (Android 13+) and survives reboot and updates.

- Home tab: ayah of the day (a curated 1-4 ayah passage, not a random fragment), Sunnah readings with their hadith
  (Al-Kahf on Friday, As-Sajdah and Al-Mulk, Ayat al-Kursi, the end of Al-Baqarah, and the three Quls at night, the
  first ten ayahs of Al-Kahf), reading history, and ayah collections from QuranApp (duas, solutions, etiquette, major
  sins).

### Quality
- Qur'an content checks in CI (counts, pages, SHA-256 fingerprints); all 6,236 ayahs verified against the source.
- Page rendering prepared off the main thread; Baseline Profile.
