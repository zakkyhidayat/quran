# Changelog

Format: [Keep a Changelog](https://keepachangelog.com). Versions follow semantic versioning (`vX.Y.Z` git tags).

## [Unreleased]

## [0.8.5] - 2026-11-11
### Fixed
- Deleting a downloaded row animates out (shrink and fade) instead of jumping; undo animates it back in. Uses the Material 3 motion scheme.

## [0.8.4] - 2026-11-11
### Changed
- Translations and Tafsir settings now list only what is downloaded. A new "Add" page holds search and the download catalog.
- Downloaded translations have an on/off switch. Tafsir keeps its radio buttons, since only one can be used.
- Swipe a downloaded row to delete it. A snackbar offers Undo for 5 seconds before the file is removed.
- "Language for themes and topics" moved from Translations to the Language page.

## [0.8.3] - 2026-11-11
### Changed
- Tafsir search field uses the same rounded shape as Translations.
- Installed tafsir rows are laid out like Translations, so the delete and update buttons line up with the download buttons below. Choose a tafsir with its radio button.

## [0.8.2] - 2026-11-11
### Changed
- Tafsir page laid out like the Translations page: installed entries in one grouped list, an "Add tafsir" heading, and a search field for downloadable tafsir.
- Installed tafsir shows an update button when the catalog has a newer version.
- Keyboard no longer covers the tafsir download list.
### Fixed
- Downloaded tafsir and translation lists are available offline: the catalog is bundled in the APK and used when the network fails.

## [0.8.1] - 2026-11-11
### Fixed
- Tafsir page: search box and downloadable list, matching the Translations page. The earlier layout had been lost in a merge.
- Choosing a tafsir after download only when none is selected, so a second download does not replace the current one.

## [0.8.0] - 2026-11-10
### Changed
- Settings main page is a full screen instead of a floating dialog.
- Language is a page of its own in Settings.
### Fixed
- Deleting a downloaded translation or tafsir asks for confirmation first, since the file must be downloaded again.

## [0.7.9] - 2026-11-10
### Fixed
- Settings and onboarding no longer fail when the system holds an app language outside the 10 supported ones.

### Changed
- Language choice is one radio list, shared by Settings and onboarding.

## [0.7.8] - 2026-11-09
### Changed
- Icons migrated to Material Symbols Rounded (generated; `ui/Icons.kt`).
- Settings: Qur'an data status card and "Check again" button, bell icon for notifications, restore asks for
  confirmation, Language opens its own page.
- Home: reading history shows "Just now" under a minute; the Delete button lines up with its title.

## [0.7.7] - 2026-11-08
### Added
- Search button in the Index top bar (Home and other tabs).
### Changed
- Search results use segmented rows with number badges, and an empty state with an icon.

## [0.7.6] - 2026-11-08
### Changed
- Removed the separate page-colour option; the reader's theme button cycles System, Light, Dark.

## [0.7.5] - 2026-11-07
### Added
- Tafsir page in Settings with the same layout as Translations; page colour toggle (later removed in 0.7.6).
### Fixed
- Juz, hizb, rub', manzil, and ruku counters follow the page heading when a division starts mid-page.
- Gap between cards on the Ayah of the day pager.

## [0.7.4] - 2026-11-06
### Changed
- Tafsir has its own Settings page; transliteration moved to Appearance.

## [0.7.3] - 2026-11-05
### Added
- Bedtime Sunnah reading as a single page with one switch and one reminder at 20:30.
- Tajweed control and per-file font download progress in onboarding.
### Changed
- Smooth transition from list to mushaf page.

## [0.7.2] - 2026-11-04
### Changed
- Qur'an data: fonts download only when they change; per-file progress; versions use the date (yyyymmdd).

## [0.7.1] - 2026-11-04
### Fixed
- Topic and theme translations table version bump (data version 11).

## [0.7.0] - 2026-11-03
### Added
- Tafsir in the ayah sheet (Ar-Tahrir pack), downloadable from the catalog.
- Theme and topic language: English or Indonesian.

## [0.6.4] - 2026-11-03
### Changed
- Theme and topic language: four choices; ayah-of-the-day notification expanded; UX/UI audit fixes.

## [0.6.3] - 2026-11-03
### Added
- Theme and topic language chosen separately from the app language.

## [0.6.2] - 2026-10-30
### Fixed
- Jump to ayah in list mode scrolls to and highlights the ayah.

## [0.6.1] - 2026-10-29
### Added
- Translation footnotes in the ayah list and the duas collection screen.

## [0.6.0] - 2026-10-28
### Added
- Reminder switch per night reading and ayah-of-the-day notification that opens an ayah with translation.

## [0.5.9] - 2026-10-22
### Changed
- Four collection tiles on Home; daily reading reminder hidden.

## [0.5.8] - 2026-10-22
### Changed
- Ayah of the day opens step by step; three notifications a day.

## [0.5.7] - 2026-10-20
### Changed
- Sunnah guidance from As-Sajdah to Al-Mulk, surah header margins, night reminder and Al-Kahf.

## [0.5.6] - 2026-10-16
### Added
- Tajweed in the duas collection; Back choice from the reader; draft option in the release workflow.

## [0.5.5] - 2026-10-16
### Changed
- Tajweed fade removed; colour changes instantly.

## [0.5.4] - 2026-10-16
### Fixed
- Faster mushaf page rendering; tajweed layer sized to its own width.

## [0.5.3] - 2026-10-16
### Fixed
- Cut-off letter tails in highlighted mushaf lines; reminder and mushaf layout.
### Added
- Ayah of the day settings and localisation of Explore.

## [0.5.2] - 2026-10-13
### Changed
- Notification permission flow and continue-reading button.

## [0.5.1] - 2026-10-13
### Added
- Sunnah reading and night reminder; translation catalogue shown directly.

## [0.5.0] - 2026-10-13
Summary of the app at this release (first public release).

### Removed
- In-app updates (added in 0.2.0). New versions are downloaded from GitHub Releases or Google Play.
- Separate reading-only app edition; its features are now part of the main app.

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
