<p align="center"><img src="docs/icon.png" width="128" alt="Quran app icon"></p>

# Quran

An Android app for reading the Qur'an in the **Uthmani script of the Madani mushaf**, page for page like the printed
King Fahd Complex edition: 604 pages, 15 lines, with tajweed colouring.

[![Support on Ko-fi](https://img.shields.io/badge/Ko--fi-support-FF5E5B?logo=ko-fi&logoColor=white)](https://ko-fi.com/zakkyhidayat)

## Who it is for

This app focuses on one way of reading, done carefully: the Madani mushaf printed by the King Fahd Glorious Quran Printing
Complex (KFGQPC). If that is the mushaf you read, memorise from, or want to get used to, this app is made for you.

Many readers, especially in Indonesia and South Asia, grew up with the **IndoPak** script or the **Indonesian Standard
Mushaf** (Mushaf Standar Indonesia). Those editions differ in letter shapes, vowel marks, pause signs, and page layout.
Both are valid traditions; this app simply does not cover them, and there are good apps that do.

Choices made on purpose:

| Area | Choice | Why |
|------|--------|-----|
| Script | Uthmani, KFGQPC V4 (Madani) | The most widely printed mushaf; word glyphs match the printed page exactly |
| Riwayah | Hafs 'an 'Asim | The riwayah of the standard Madani mushaf |
| Layout | 604 fixed pages, 15 lines | Memorisation relies on where an ayah sits on the page, so text never reflows |
| Tajweed | V4 font colours (can be turned off) | Same colour source as the printed tajweed mushaf |
| Page style | One style only | Fewer settings, a consistent page |

## Features

- Mushaf pages in B5 paper proportions, with calligraphic surah and juz titles above each page.
- Light, dark, and AMOLED themes; dynamic colour (Android 12+) or the original palette; three contrast levels.
- Tap an ayah for its translations (with footnotes), Latin transliteration, and position (juz, hizb, rub', manzil, ruku,
  sajdah).
- Translations are downloaded in the app, so the APK carries none; the active ones download automatically on first
  launch. Available now: Indonesian (Kemenag, The Sabiq Company, King Fahad Quran Complex) and English (Saheeh
  International), with more languages to come.
- Indexes by surah, juz, hizb, rub', manzil, and ruku; surah info; go to ayah; random ayah; continue reading from the last ayah.
- Backup and restore of bookmarks and settings to a file (on the device or any cloud app with a file provider, such as
  Google Drive).
- Page and ayah bookmarks; search by Arabic text, translation, surah name, or a reference such as `2:255`.
- Interface in 10 languages, following the device language by default: English, Indonesian, Arabic, Urdu, Bengali,
  Turkish, Persian, Malay, French, and Russian.
- Screen reader support: each ayah is read out as Unicode Arabic text.
- No ads, no account, no tracking. The internet is only used to download translations.

## Roadmap

- [x] Reading modes for all surahs: mushaf page, ayahs with translation, translation only (switched from a pill at the
      bottom of the screen)
- [x] Onboarding for new users: language, about this mushaf, translations, appearance
- [x] In-app updates for builds from GitHub Releases
- [x] All translations downloadable, none bundled in the APK
- [ ] Automatic cloud backup (Google Drive, Dropbox, OneDrive)
- [ ] Reading reminders to keep a daily portion by juz, hizb, or manzil (in progress, hidden for now)
- [ ] Word-by-word meaning (tap a word to see its translation)
- [ ] Audio recitation
- [ ] Ayah actions in the ayah + translation and translation-only modes: tap an ayah to show bookmark, copy, and share
      buttons; tap again to hide them
- [ ] Animated transitions between the three reading modes
      - Between ayah + translation and translation only, try animating the Arabic text in and out (expand/collapse)
        instead of moving the whole page; needs testing first
- [ ] A counter in the reader's top bar, between search and bookmark: ayah count within the surah, or progress through
      the current juz, hizb, rub', manzil, or ruku (tap to switch)
- [ ] Remove the page number below the page in the mushaf view
- [ ] Thematic browsing on the main screen, using QUL resources: topics and concepts in the Quran (2,512 topics with
      semantic relations), ayah themes, similar ayahs, mutashabihat (similar phrases), and grammar/morphology (roots,
      lemmas, parts of speech per word)

### Known issues

- Page turns sometimes lose their animation: the page shrinks slightly and then disappears instead of sliding. Likely
  caused by one of the rendering optimisations.

## Data, credits, and licence

All Qur'an data comes from the [Quranic Universal Library (QUL)](https://qul.tarteel.ai) by Tarteel. As the
[QUL credits page](https://qul.tarteel.ai/credits) notes, most of its resources were created or curated by the community
rather than by Tarteel. This app uses:

| Part | Original work by |
|------|------------------|
| V4 tajweed page fonts, 15-line layout, surah header font, Hafs Uthmanic Script font | King Fahd Glorious Quran Printing Complex (KFGQPC), used unmodified |
| Unicode Arabic text; juz, hizb, manzil, ruku, and sajdah metadata | KFGQPC and Tanzil, curated by QUL |
| Surah info, transliteration | Quran.com and QUL contributors |
| Translations | Their translators and publishers (Kemenag RI, The Sabiq Company, King Fahad Quran Complex, Saheeh International, and the downloadable packs) |
| QUL itself | Naveed Ahmad and the QUL team and contributors |

May Allah reward everyone involved. The full list of contributors is on the QUL credits page, which is also linked from
the About section in the app. Font copyright remains with KFGQPC; each translation is subject to its publisher's terms.

The app's source code is licensed under [GPL-3.0](LICENSE). That licence does not cover the third-party fonts and data
listed above.

## Support

The app is free and has no ads. If it helps you, you can support its development on
[Ko-fi](https://ko-fi.com/zakkyhidayat).

## Building from source

Requires JDK 17+, the Android SDK, and Python 3 with `fonttools`.

1. Download the QUL files listed in [docs/DATA_SOURCES.md](docs/DATA_SOURCES.md) into `data-src/` (not in git; QUL
   downloads need a login).
2. Copy the page fonts `data-src/ttf/p1.ttf` to `p604.ttf` into `app/src/main/assets/fonts/` (too large for git).
   `quran.db` is committed; rebuild it only when the data changes: `python tools/build_db.py`.
3. Build:
   ```bash
   ./gradlew :app:assembleGithubDebug
   ```

Minimum Android version: 8.0 (API 26).

Versions follow semantic versioning from git tags `vX.Y.Z`, with `versionCode = X*10000 + Y*100 + Z`. Releases are
built and signed by GitHub Actions; see [docs/RELEASING.md](docs/RELEASING.md).

## Checks

Run these before every release; CI runs the first three on every push.

```bash
python tools/verify_quran.py            # Qur'an: 114 surahs, 6,236 ayahs, pages, SHA-256 content fingerprints
python tools/check_strings.py           # interface translations
./gradlew :app:testGithubDebugUnitTest  # unit tests
python tools/verify_quran.py --source   # compare every ayah with the original QUL file (needs data-src/)
```

If an error in the Qur'an text is ever found, follow [docs/CONTENT_ERRORS.md](docs/CONTENT_ERRORS.md).

## Translating the interface

Interface text lives in standard Android string resources, one file per language. See
[docs/TRANSLATING.md](docs/TRANSLATING.md) for how to add a language or fix a translation.

## More documentation

- [ARCHITECTURE.md](ARCHITECTURE.md): layers, database, and how the text reaches the screen.
- [CHANGELOG.md](CHANGELOG.md), [PRIVACY.md](PRIVACY.md), [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
- [docs/AUDIT.md](docs/AUDIT.md): release audit checklist and open findings (in Indonesian).
- [docs/RELEASING.md](docs/RELEASING.md): signing, CI, and making a release.

- [docs/DATA_SOURCES.md](docs/DATA_SOURCES.md): QUL sources, how to update the data, and downloadable translation packs
  (written in Indonesian).
