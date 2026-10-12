<p align="center"><img src="docs/icon.png" width="128" alt="Quran app icon">&nbsp;&nbsp;&nbsp;<img src="docs/icon-lite.png" width="128" alt="Quran Lite app icon"></p>

# Quran

An Android app for reading the Qur'an in the **Uthmani script of the Madani mushaf**, page for page like the printed
King Fahd Complex edition: 604 pages, 15 lines, with tajweed colouring.

It comes in two editions: **Quran**, with translations and additional reading tools, and **Quran Lite**, focused on
reading the mushaf with bookmarks. Both share the same mushaf pages and tajweed display; the differences are described
below.

[![Support on Ko-fi](https://img.shields.io/badge/Ko--fi-support-FF5E5B?logo=ko-fi&logoColor=white)](https://ko-fi.com/zakkyhidayat)

## Download

<p align="center">
  <a href="https://github.com/zakkyhidayat/quran/releases/latest/download/Quran.apk"><img src="https://img.shields.io/badge/Download-Quran-176B4D?style=for-the-badge&logo=android&logoColor=white" alt="Download Quran"></a>
  &nbsp;
  <a href="https://github.com/zakkyhidayat/quran/releases/latest/download/Quran-Lite.apk"><img src="https://img.shields.io/badge/Download-Quran%20Lite-F2C14E?style=for-the-badge&logo=android&logoColor=176B4D&labelColor=176B4D" alt="Download Quran Lite"></a>
</p>

| App | For |
|-----|-----|
| **Quran** | Reading with translations, search, home tab, thematic browsing, reminders, backup |
| **Quran Lite** | Reading the mushaf only, with bookmarks; internet is used once to download its required database and page fonts |

**How to install (Android 8 or newer):**

1. Tap one of the buttons above on your phone. The APK file downloads.
2. Open the downloaded file. If Android asks, allow your browser or file manager to **install unknown apps**.
3. Tap **Install**.

Both apps can be installed side by side. Neither updates itself: to update, download again from the same button and
install over the old one. Your bookmarks and settings are kept. On first launch, choose the app language and download
the required Qur'an database and page fonts; they are stored privately on the device so reading works offline afterward.
You can redownload or update these files from Settings if needed. All versions and release notes:
[releases page](https://github.com/zakkyhidayat/quran/releases).

## Who it is for

This app focuses on one way of reading, done carefully: the Madani mushaf printed by the King Fahd Glorious Quran Printing
Complex (KFGQPC). If that is the mushaf you read, memorise from, or want to get used to, this app is made for you.

Other mushaf traditions include **IndoPak** and **Maghribi** scripts, as well as editions following other riwayat such
as **Warsh** or **Qalun**. They differ in letter shapes, recitation notation, pause signs, or page layout. These are
valid traditions; this app focuses on the Madani mushaf in Hafs 'an 'Asim and does not cover those alternatives.

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
- Translation packs are downloaded on demand in the full app, so the APK carries none. Packs are available in many
  languages, including Indonesian and English; browse and choose them in the app.
- The Qur'an database and all 604 page fonts are downloaded during first-run setup from the stable `quran-data` release;
  neither is bundled in the APK. You can manually redownload/update them in Settings, and reading works offline.
- Indexes by surah, juz, hizb, rub', manzil, and ruku; surah info; go to ayah; random ayah; continue reading from the last ayah.
- Home tab: ayah of the day, Sunnah readings with their hadith, reading history, and ayah collections.
- Thematic browsing: topics, ayah themes, similar ayahs, mutashabihat, and word roots.
- Optional daily reading reminder by juz, hizb, or manzil.
- Backup and restore of bookmarks and settings to a file (on the device or any cloud app with a file provider, such as
  Google Drive). Quran and Quran Lite can share one backup file, so saved pages and ayahs follow you between editions.
- Optional automatic backup to a user-selected folder; it keeps the three newest snapshots.
- Page and ayah bookmarks; search by Arabic text, translation, surah name, or a reference such as `2:255`.
- Reading modes for the mushaf page, ayahs with translation, or translation only; tajweed colours can be switched
  on or off, with a compact guide to the colour rules.
- Interface in 10 languages, following the device language by default: English, Indonesian, Arabic, Urdu, Bengali,
  Turkish, Persian, Malay, French, and Russian.
- Screen reader support: each ayah is read out as Unicode Arabic text.
- No ads, no account, no tracking. The internet is used for the required first-run Qur'an database and page-font download and,
  in the full app, translation downloads.

## Quran Lite

<img src="docs/icon-lite.png" width="72" alt="Quran Lite app icon" align="right">

Quran Lite is the reading-focused edition for people who only want to read the mushaf:

- Mushaf pages; the home screen is limited to Bookmark and Surah, with page and ayah bookmarks and continue reading.
- Tapping an ayah only offers a bookmark; no translations, search, home tab, or thematic browsing.
- First-run setup asks for the app language and downloads the required Qur'an database and page fonts, then opens the
  reader without the Full app's translation and appearance setup steps.
- Settings include the theme, Qur'an data maintenance, and app information.
- Internet is used only for the required first-run download or optional manual update of the Lite Qur'an database and page fonts; no notifications or other
  network features are included. Reading works offline after setup.

It is a separate app (`io.zakkyhidayat.quran.lite`) with its own icon, published on the same GitHub release as
`Quran-Lite.apk`. Its Qur'an text and page rendering are the same as the full app; its separately downloaded database
leaves out data only the full app uses. Details: [docs/FORK_LITE.md](docs/FORK_LITE.md).

## Roadmap

- [x] Reading modes for all surahs: mushaf page, ayahs with translation, or translation only
- [x] Onboarding for new users: language and required Qur'an database/page-font download, mushaf preview, translations (full), appearance
- [x] All translations downloadable, none bundled in the APK
- [ ] Automatic cloud backup (Google Drive, Dropbox, OneDrive)
- [x] Reading reminders to keep a daily portion by juz, hizb, or manzil
- [ ] Word-by-word meaning (tap a word to see its translation)
- [ ] Audio recitation
- [x] Ayah actions in the ayah + translation and translation-only modes: tap an ayah to show bookmark, copy, and share
      buttons; tap again to hide them
- [x] Animated transitions between the three reading modes
      - Between ayah + translation and translation only, the Arabic text expands/collapses instead of moving the page
- [x] A counter in the reader's top bar, between search and bookmark: ayah count within the surah, or progress through
      the current juz, hizb, rub', manzil, or ruku (tap to switch)
- [x] Remove the page number below the page in the mushaf view
- [x] Home tab: ayah of the day, Sunnah readings by time of day, reading history, and ayah collections (duas,
      solutions, etiquette, major sins)
- [x] Thematic browsing using QUL resources: topics and concepts (2,512 topics with semantic relations), ayah themes,
      similar ayahs, mutashabihat (similar phrases), and roots, lemmas, and stems per word
      - [ ] Parts of speech per word (the QUL source page returns an error, schema unknown)
- [x] Quran Lite: a reading-only app built from the same code

Bug reports and suggestions: [GitHub Issues](https://github.com/zakkyhidayat/quran/issues).

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
2. Copy the page fonts `data-src/ttf/p1.ttf` to `p604.ttf` into `app/src/main/assets/fonts/` (too large for git; excluded from APKs).
   `quran.db` is committed as build input and is excluded from APKs; rebuild it only when the data changes:
   `python tools/build_db.py`.
3. Choose the target:
   ```bash
   ./gradlew :app:assembleFullDebug       # Full APK
   ./gradlew :app:assembleLiteDebug       # Quran Lite APK
   ./gradlew :app:assembleBothDebug       # both APKs
   ./gradlew :app:bundleFullPlayRelease   # Full app bundle for Google Play
   ```

The `assembleBothDebug` task can be run with `--parallel` to enable Gradle's parallel execution where supported.
Outputs are under `app/build/outputs/`; Full and Lite use separate package IDs and can be installed side by side.
For release builds and the exact output mapping, see [docs/RELEASING.md](docs/RELEASING.md).

Minimum Android version: 8.0 (API 26).

Versions follow semantic versioning from git tags `vX.Y.Z`, with `versionCode = X*10000 + Y*100 + Z`. Releases are
built and signed by GitHub Actions; see [docs/RELEASING.md](docs/RELEASING.md).

## Checks

Run these before every release. CI checks Qur'an data and interface strings, runs the same JVM unit-test suite and
Android lint for both the full and Lite variants, and builds the full app and Quran Lite (debug, plus an unsigned Lite
release to test code shrinking). It also verifies the generated Lite database against the Qur'an source and ensures
neither APK bundles the database or page fonts.

```bash
python tools/verify_quran.py            # Qur'an: 114 surahs, 6,236 ayahs, pages, SHA-256 content fingerprints
python tools/check_strings.py           # interface translations
./gradlew :app:testGithubDebugUnitTest :app:testLiteDebugUnitTest
./gradlew :app:lintGithubDebug :app:lintLiteDebug
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
- [docs/FORK_LITE.md](docs/FORK_LITE.md): how Quran Lite is built from the same code (in Indonesian).
- [docs/UI_UX.md](docs/UI_UX.md) and [docs/LANGUAGE_DATA.md](docs/LANGUAGE_DATA.md): UI/UX decisions and language data
  licensing notes (in Indonesian).
- [docs/DATA_SOURCES.md](docs/DATA_SOURCES.md): QUL sources, how to update the data, and downloadable translation packs
  (written in Indonesian).
