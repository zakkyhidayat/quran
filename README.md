<p align="center"><img src="docs/icon.png" width="128" alt="Quran app icon"></p>

# Quran

An Android app for reading the Qur'an in the **Uthmani script of the Madani mushaf**, matching the 604-page, 15-line
King Fahd Complex edition, with optional tajweed colouring, translations, and additional reading tools.

[![Support on Ko-fi](https://img.shields.io/badge/Ko--fi-support-FF5E5B?logo=ko-fi&logoColor=white)](https://ko-fi.com/zakkyhidayat)

## Download

<p align="center">
  <a href="https://github.com/zakkyhidayat/quran/releases/latest/download/Quran.apk"><img src="https://img.shields.io/badge/Download-Quran-176B4D?style=for-the-badge&logo=android&logoColor=white" alt="Download Quran"></a>
</p>

**How to install (Android 8 or newer):**

1. Tap the **Download Quran** button above on your phone. The APK file downloads.
2. Open the downloaded file. If Android asks, allow your browser or file manager to **install unknown apps**.
3. Tap **Install**.

The app does not update itself: download the new APK from the same button and install over the existing app. Bookmarks
and settings are kept. First-run setup guides you through the app language, required Qur'an data, translations, and
appearance.

All versions and release notes:
[releases page](https://github.com/zakkyhidayat/quran/releases).

## Shared details

- First-run setup downloads the Qur'an database and all 604 page fonts from the stable `quran-data` release; neither is
  bundled in the APK. Reading works offline after setup. Data can be repaired or downloaded again from Settings.
- Bookmarks and settings can be exported to a file; automatic backup keeps up to three snapshots. Use Android's file
  picker to choose local or cloud storage.
- Interface in 10 languages: English, Indonesian, Arabic, Urdu, Bengali, Turkish, Persian, Malay, French, and Russian.
- Screen readers announce each ayah as Unicode Arabic text.
- No ads, account, or tracking. Internet is used for required setup, optional data repair, and translation downloads.

## Features

- Reading modes: fixed mushaf page, ayahs with translation, or translation only. Tajweed colours can be toggled in the
  mushaf and ayah-list modes; ayah-list colouring uses page glyphs when the downloaded fonts are available.
- Reader controls include counters for ayah, juz, hizb, rub', manzil, and ruku. Tap an ayah to view translations with
  footnotes, transliteration, and its division positions; ayah actions include bookmark, copy, and share.
- Search by Arabic text, translation, surah name, or reference; browse surahs and divisions, jump to an ayah, or open a
  random ayah.
- Home features: a curated passage for the ayah of the day, Sunnah readings with hadith, reading history, and ayah
  collections.
- Translation settings show the active translations and searchable download catalogue together; the catalogue appears
  inline and can be browsed without opening a separate dialog.
- Thematic tools: topics, ayah themes, similar ayahs, mutashabihat, and word roots, lemmas, and stems.
- Optional daily reading reminders by juz, hizb, or manzil.
- Light, dark, and AMOLED themes; dynamic colour (Android 12+) or the original teal palette; three contrast levels.
- Translation packs are downloaded on demand in many languages, including Indonesian and English; none are bundled in
  the APK.
- Tafsir (105 sources in Arabic, Indonesian, English, and many other languages) and surah info in extra languages are
  optional downloads from Settings. The ayah sheet shows the selected tafsir for the open ayah.

## Who it is for

This app focuses on one way of reading, done carefully: the Madani mushaf printed by the King Fahd Glorious Quran Printing
Complex (KFGQPC). If that is the mushaf you read, memorise from, or want to get used to, this app is made for you.

This app focuses on the Madani mushaf in Hafs 'an 'Asim. Other valid mushaf layouts and scripts include traditions
such as **IndoPak** or **Shamarly**. Page layouts differ in script, line breaks, and page count; recitation traditions
such as Warsh and Qalun are a separate distinction. This app currently supports only the **KFGQPC V4 Madani layout**.

Choices made on purpose:

| Area | Choice | Why |
|------|--------|-----|
| Script | Uthmani, KFGQPC V4 (Madani) | The most widely printed mushaf; word glyphs match the printed page exactly |
| Riwayah | Hafs 'an 'Asim | The riwayah of the standard Madani mushaf |
| Layout | 604 fixed pages, 15 lines | Memorisation relies on where an ayah sits on the page, so text never reflows |
| Tajweed | V4 font colours (can be turned off) | Same colour source as the printed tajweed mushaf |
| Page style | One style only | Fewer settings, a consistent page |

## Roadmap

- [ ] Word-by-word meaning (tap a word to see its translation)
- [ ] Audio recitation
- [ ] Parts of speech per word (the QUL source page returns an error, schema unknown)

Bug reports and suggestions: [GitHub Issues](https://github.com/zakkyhidayat/quran/issues).

### Known issues

- Page turns can occasionally lose their animation: the page shrinks slightly and disappears instead of sliding.

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

## Building from source

Requires JDK 21, the Android SDK, and Python 3. The committed `quran.db` is a build input; the 604 page fonts are
downloaded at first launch and are not needed to build the APK. QUL source files under `data-src/` are only needed when
rebuilding the database.

Build a debug APK with:

```bash
./gradlew :app:assembleFullDebug
```

To rebuild the Qur'an database after updating its QUL source files, download the inputs listed in
[docs/DATA_SOURCES.md](docs/DATA_SOURCES.md) into `data-src/` (QUL downloads may require a login), then run
`python tools/build_db.py`. Qur'an text must never be edited by hand. For a Google Play app bundle, release signing, and
the release workflow, see [docs/RELEASING.md](docs/RELEASING.md).

Outputs are under `app/build/outputs/`.
For release builds and the exact output mapping, see [docs/RELEASING.md](docs/RELEASING.md).

Minimum Android version: 8.0 (API 26).

Versions follow semantic versioning from git tags `vX.Y.Z`, with `versionCode = X*10000 + Y*100 + Z`. Releases are
built and signed by GitHub Actions; see [docs/RELEASING.md](docs/RELEASING.md).

## Checks

CI runs the Qur'an data and interface-string checks, JVM unit tests, Android lint, debug and release APK builds, then
verifies that the APK does not bundle the database or page fonts. The release workflow also builds the Play bundle.

```bash
python tools/verify_quran.py            # Qur'an: 114 surahs, 6,236 ayahs, pages, SHA-256 content fingerprints
python tools/check_strings.py           # interface translations
./gradlew :app:testGithubDebugUnitTest
./gradlew :app:lintGithubDebug
./gradlew :app:assembleGithubDebug :app:assembleGithubRelease
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
- [docs/UI_UX.md](docs/UI_UX.md) and [docs/LANGUAGE_DATA.md](docs/LANGUAGE_DATA.md): UI/UX decisions and language data
  licensing notes (in Indonesian).
- [docs/DATA_SOURCES.md](docs/DATA_SOURCES.md): QUL sources, how to update the data, and downloadable translation packs
  (written in Indonesian).
