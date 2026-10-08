# Changelog

Format: [Keep a Changelog](https://keepachangelog.com). Versions follow semantic versioning (`vX.Y.Z` git tags).

## [Unreleased]

First public release candidate.

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
- In-app updates for the GitHub build.
- Daily reading reminder: a notification at a chosen time with the day's juz, hizb, or manzil and its ayah range; tapping
  it opens the start of that portion. Asks for notification permission (Android 13+) and survives reboot and updates.

### Quality
- Qur'an content checks in CI (counts, pages, SHA-256 fingerprints); all 6,236 ayahs verified against the source.
- Page rendering prepared off the main thread; Baseline Profile.
