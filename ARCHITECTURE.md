# Architecture

## From source files to the screen

```
QUL files (data-src/, downloaded by hand, checksums in docs/data-manifest.json)
  │  tools/build_db.py              read-only build step, no hand-edited Qur'an text
  ▼
app/src/main/assets/quran.db        source database, verified by tools/verify_quran.py (CI; excluded from APKs)
app/src/main/assets/fonts/          header, surah-name, common, and Hafs fonts; page fonts excluded from APK
  │  `quran-data` release            holds compressed databases, page-fonts.zip, and quran-data.json
  │  QuranDatabaseInstaller         downloads the flavor-specific database and all page fonts on first launch;
  │                                  verifies sizes, SHA-256, SQLite integrity, schema, and all 604 fonts
  ▼
noBackupFilesDir/quran.db            installed with noBackupFilesDir/quran-fonts/p1.ttf..p604.ttf for offline reading
  ▼
MushafRepository                    the only reader of quran.db (+ TranslationPacks for downloaded translations)
  ▼
AppViewModel                        app-wide state: surahs, markers, selection, translations, updates, auto backup
  ▼
Compose screens                     reader, index, search, surah info, settings, onboarding
```

Translations are not in `quran.db`. They are SQLite packs built by `tools/build_translation_packs.py`, published on
GitHub Releases (`translations`), downloaded into `filesDir/translations/`, and verified (SHA-256, 6,236 rows) before use.
The required Qur'an database and page-font archive are downloaded from the stable `quran-data` release during onboarding
or by a user-requested repair/update from Settings.
Reading is blocked until both are installed and is offline afterward; full and Lite receive separate database assets and
the same page-font archive, while neither APK contains `quran.db` or the 604 page fonts.

## Packages (`app/src/main/java/io/zakkyhidayat/quran/`)

| Package | Responsibility |
|---------|----------------|
| `data/` | `ContentDatabase`, `QuranDatabaseInstaller` (release catalog, verified database/font download), `DownloadHttp`, `MushafRepository`, `TranslationPacks`, `BookmarkStore` (user.db), `Backup` (JSON export/import), `Collections` (curated ayah collections), `ReadingHistory` (history.db), models |
| `reader/` | `ReaderScreen` (top bar, mode pill, pager), `MushafPage` (page rendering), `PalettedFonts` (font palettes), `AyahListReader` (ayah + translation modes), `AyahSheet` |
| `home/` | Home tab (`HomeTab`: ayah of the day, sunnah readings, reading history, collections) and `PassageScreen` |
| `explore/` | Thematic browsing: topic list and topic screen |
| `index/`, `search/`, `info/` | Surah/juz/hizb/rub'/manzil/ruku/bookmark lists, search, surah info |
| `settings/` | `AppSettings` (DataStore), `SettingsScreen`, `AddTranslationDialog` |
| `onboarding/` | First-run steps |
| `reminder/` | Daily reading reminder (AlarmManager + notification) |
| `ui/` | Theme, shared components, icons, HTML renderer for surah info, update dialog |
| `AppLanguage.kt` | Interface language (system per-app locales on Android 13+) |

## Lite variant

The Full app and Quran Lite are outputs of the same `:app` module. Shared reader, data, and tajweed code lives in
`app/src/main`; the `lite` source set supplies its manifest and app-specific resources. Product-flavor configuration
sets `BuildConfig.LITE`, which gates the different navigation, home/index, reading modes, onboarding, and settings
behavior without duplicating the shared mushaf reader.

The `lite` product flavor (`io.zakkyhidayat.quran.lite`) is a reading-only app: mushaf pages, index lists, page and
ayah bookmarks, backup, and theme settings. Backup is shared with Full: format v2 keeps bookmarks, deletion tombstones, and reading position edition-neutral, and
settings per `full`/`lite` profile (see `data/Backup.kt`). Auto-backup uses a SAF folder, writes timestamped snapshots
after debounced changes, and prunes older snapshots to retain at most three. Lite uses internet only during first-run
setup or a user-requested repair/update
to download its separate, slim database and shared page fonts; notifications and boot permissions remain removed by
`app/src/lite/AndroidManifest.xml`. After installation, reading works offline. `tools/build_lite_db.py` creates the
release database without thematic tables, surah info, or transliteration.

The `github` flavor produces the Full APK and the `play` flavor produces the Full Play bundle. The `lite` flavor
produces the Quran Lite APK. Short Gradle task aliases for building one or both APKs are documented in
[`docs/RELEASING.md`](docs/RELEASING.md). See [`docs/FORK_LITE.md`](docs/FORK_LITE.md) for the feature and output
comparison.

## Database (`quran.db`, read-only)

| Table | Content |
|-------|---------|
| `surahs` | id, Arabic and Latin names, ayah count, first page, surah-name glyph, revelation place and order |
| `ayahs` | Unicode QPC Hafs text per ayah, with its page |
| `words` | V4 glyph code per word, with surah, ayah, position, page, line, end-of-ayah flag |
| `page_lines` | 15 lines per page: type (ayah, surah name, basmalah), centred flag, first/last word |
| `juz`, `hizb`, `rub`, `manzil`, `ruku`, `sajda` | Start of each division |
| `surah_info`, `transliteration` | Surah descriptions (id, en) and Latin transliteration |
| `topics`, `topic_ayahs`, `ayah_themes`, `similar_ayahs`, `mutashabihat`, `mutashabihat_ayahs`, `morph_roots`, `morph_lemmas`, `morph_stems`, `word_morph` | Optional (thematic browsing): built only when the QUL sources are present (the committed `quran.db` has them); the app checks `sqlite_master` and hides the feature when they are missing |
| `translations`, `translation_texts`, `footnotes` | Kept empty for schema compatibility; translations are downloaded packs |

User data lives elsewhere: `user.db` (bookmarks), `history.db` (reading history, not backed up) and DataStore `settings` (preferences, last read position).

## Rendering a mushaf page

Each word is one glyph in that page's KFGQPC V4 colour font (COLR/CPAL). Android always uses a font's first palette,
so `PalettedFonts` writes a copy with another built-in palette marked as default (light or dark, with or without tajweed
colours) and loads it as a typeface on a background thread. `MushafPage` measures every word of the page on a
background thread too, then draws each line on a canvas, justified right to left. The pager prepares neighbouring pages
in advance, so swiping only draws ready-made layouts.

## Tests and checks

- `tools/verify_quran.py`: Qur'an structure and content fingerprints (CI); `--source` compares every ayah with the
  original file.
- `tools/check_strings.py`: interface translations (CI).
- `app/src/test`: the same JVM unit-test suite and Android lint run for both full (`github`) and `lite` debug variants
  in CI; source and generated Lite databases are verified, and CI checks that neither APK embeds the database.
- `baselineprofile/`: generates the Baseline Profile on a device.
