# Third-party notices

The app's own source code is licensed under [GPL-3.0](LICENSE). The content and libraries below keep their own terms.

## Qur'an content (via Quranic Universal Library)

All Qur'an data is taken from the [Quranic Universal Library (QUL)](https://qul.tarteel.ai) by Tarteel, whose resources
were largely created or curated by the community ([credits](https://qul.tarteel.ai/credits)). Exact files, URLs, and
SHA-256 checksums are in [docs/DATA_SOURCES.md](docs/DATA_SOURCES.md) and [docs/data-manifest.json](docs/data-manifest.json).

| Content | Origin | Terms |
|---------|--------|-------|
| V4 tajweed page fonts, 15-line layout, word glyph codes | King Fahd Glorious Quran Printing Complex (KFGQPC) | KFGQPC terms: free use, no sale, no modification of the fonts. The app ships the font files unchanged; at runtime it stores copies with a different built-in colour palette selected (see below). |
| Surah header font, Hafs Uthmanic Script font, quran-common font, surah name font | KFGQPC / QUL | As above |
| Unicode Arabic text (QPC Hafs) | KFGQPC, via QUL | Used unchanged; verified by `tools/verify_quran.py` |
| Juz, hizb, rub', manzil, ruku, sajdah metadata; surah names, revelation place and order | QUL | QUL terms |
| Surah info (Indonesian, English) | QUL contributors | QUL terms |
| Transliteration | QUL contributors | QUL terms |
| Translations: Kemenag RI, The Sabiq Company, King Fahad Quran Complex (Indonesian); Saheeh International (English) | Their publishers, via QUL | Copyright of each publisher. **Permission for redistribution has not been confirmed yet** (see docs/AUDIT.md). |
| Downloadable translation packs (about 170, many languages; catalogue in `tools/translation_catalog.json`) | The publisher or translator named in each pack and in the app's translation list, via QUL (https://qul.tarteel.ai/resources/translation/<id>) | Copyright of each publisher/translator. Packs are downloaded on demand, not shipped in the APK. **Permission for redistribution has not been confirmed yet.** |

**Font palettes.** The V4 fonts contain six colour palettes (CPAL). Android always uses the first one, so the app writes
a copy of a font with a different existing palette marked as default (light/dark, with or without tajweed colours). No
glyph, outline, or colour value is changed. Whether this counts as a modification under KFGQPC's terms is being
confirmed (see docs/AUDIT.md).

## Ayah collections (via QuranApp)

`app/src/main/assets/collections.json` (duas, solutions, etiquette, and major sins in the Quran: ayah references and
their titles in 9 languages) is converted by `tools/build_collections.py` from
[QuranApp](https://github.com/AlfaazPlus/QuranApp) by AlfaazPlus, `app/src/main/assets/verses/`, licensed GPL-3.0 like
this app. Ayah text and translations are not taken from it.

## Libraries in the app

| Library | License |
|---------|---------|
| Kotlin standard library, kotlinx.coroutines | Apache-2.0 |
| AndroidX (Core, Activity, Lifecycle, Navigation, DataStore, Compose, Material 3, ProfileInstaller) | Apache-2.0 |
| Material Symbols icon paths (drawn in code) | Apache-2.0 |

## Build tools (not shipped in the app)

| Tool | License |
|------|---------|
| Gradle, Android Gradle Plugin | Apache-2.0 |
| AndroidX Benchmark (baseline profile generation) | Apache-2.0 |
| fontTools (`tools/build_db.py`) | MIT |
| materialyoucolor (`tools/gen_colors.py`) | MIT |
