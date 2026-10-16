# Architecture

## From source files to the screen

```
QUL files (data-src/, downloaded by hand, checksums in docs/data-manifest.json)
  │  tools/build_db.py              read-only build step, no hand-edited Qur'an text
  ▼
app/src/main/assets/quran.db        source database, verified by tools/verify_quran.py (CI; excluded from APKs)
app/src/main/assets/fonts/          header, surah-name, common, and Hafs fonts; page fonts excluded from APK
  │  `quran-data` release            holds the compressed database, page-fonts.zip, and quran-data.json
  │ QuranDatabaseInstaller         downloads the database and all page fonts on first launch;
  │                                  verifies sizes, SHA-256, SQLite integrity, schema, and all 604 fonts
  ▼
noBackupFilesDir/quran.db            installed with noBackupFilesDir/quran-fonts/p1.ttf..p604.ttf for offline reading
  ▼
MushafRepository                    the only reader of quran.db (+ TranslationPacks for downloaded translations, ContentPacks for downloaded surah-info and tafsir packs)
  ▼
AppViewModel                        app-wide state: surahs, markers, selection, translations, updates, auto backup
  ▼
Compose screens                     reader, index, search, surah info, settings, onboarding
```

Translations are not in `quran.db`. They are SQLite packs built by `tools/build_translation_packs.py`, published on
GitHub Releases (`translations`), downloaded into `filesDir/translations/`, and verified (SHA-256, 6,236 rows) before use.
Surah info in languages other than English, and all tafsir, are optional gzip packs from `tools/build_content_packs.py` in the same
release and catalog (separate `surah_info` / `tafsir` arrays), installed into `filesDir/content/<kind>/` after a SHA-256 and content check.
The required Qur'an database and page-font archive are downloaded from the stable `quran-data` release during onboarding
or by a user-requested repair/update from Settings.
Reading is blocked until both are installed and is offline afterward; the APK does not contain `quran.db` or the 604 page fonts.

## Packages (`app/src/main/java/io/zakkyhidayat/quran/`)

| Package | Responsibility |
|---------|----------------|
| `data/` | `ContentDatabase`, `QuranDatabaseInstaller` (release catalog, verified database/font download), `DownloadHttp`, `MushafRepository`, `TranslationPacks`, `ContentPacks` (optional gzip surah-info/tafsir packs in `filesDir/content/`), `BookmarkStore` (user.db), `Backup` (JSON export/import), `Collections` (curated ayah collections), `ReadingHistory` (history.db), models |
| `reader/` | `ReaderScreen` (top bar, reader controls, pager), `MushafPage` (page rendering), `PalettedFonts` (font palettes), `AyahListReader` (ayah + translation modes), `AyahSheet` |
| `home/` | Home tab (`HomeTab`: ayah of the day, sunnah readings, reading history, collections) and `PassageScreen` |
| `explore/` | Thematic browsing: topic list and topic screen |
| `index/`, `search/`, `info/` | Surah/juz/hizb/rub'/manzil/ruku/bookmark lists, search, surah info |
| `settings/` | `AppSettings` (DataStore), `SettingsScreen`, `AddTranslationDialog` |
| `onboarding/` | First-run steps |
| `reminder/` | Daily reading reminder (AlarmManager + notification) |
| `ui/` | Theme, shared components, icons, HTML renderer for surah info, update dialog |
| `AppLanguage.kt` | Interface language (system per-app locales on Android 13+) |

## Distribution

The `github` flavor produces the downloadable APK and the `play` flavor produces the Google Play bundle. Both flavors
contain the same app features and use the same database and page-font archive. Bookmarks and settings are exported in
backup format v2 (see `data/Backup.kt`); automatic backup writes timestamped snapshots to a user-selected SAF folder
after debounced changes and retains at most three snapshots.

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

Tajweed colours are a second set of layouts measured with the plain layer's font size, so both have the same glyph size.
Switching tajweed on or off is instant, with no animation: the page draws either the coloured or the plain layouts, each
positioned from its own widths. The ayah highlight is drawn on its own layer beneath all lines.

## Tests and checks

- `tools/verify_quran.py`: Qur'an structure and content fingerprints (CI); `--source` compares every ayah with the
  original file.
- `tools/check_strings.py`: interface translations (CI).
- `app/src/test`: JVM unit tests and Android lint run for the `github` debug variant in CI; CI also checks that the APK
  does not embed the database or page fonts.
- `baselineprofile/`: generates the Baseline Profile on a device.
