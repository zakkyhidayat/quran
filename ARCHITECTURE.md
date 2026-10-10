# Architecture

## From source files to the screen

```
QUL files (data-src/, downloaded by hand, checksums in docs/data-manifest.json)
  │  tools/build_db.py              read-only build step, no hand-edited Qur'an text
  ▼
app/src/main/assets/quran.db        verified by tools/verify_quran.py (CI)
app/src/main/assets/fonts/          KFGQPC V4 page fonts p1..p604, header and Hafs fonts (copied unchanged)
  │  ContentDatabase                copies quran.db to no-backup storage once per app version, opens it read-only
  ▼
MushafRepository                    the only reader of quran.db (+ TranslationPacks for downloaded translations)
  ▼
AppViewModel                        app-wide state: surahs, markers, selection, translations, updates, auto backup
  ▼
Compose screens                     reader, index, search, surah info, settings, onboarding
```

Translations are not in `quran.db`. They are SQLite packs built by `tools/build_translation_packs.py`, published on
GitHub Releases (`translations`), downloaded into `filesDir/translations/`, and verified (SHA-256, 6,236 rows) before use.

## Packages (`app/src/main/java/io/zakkyhidayat/quran/`)

| Package | Responsibility |
|---------|----------------|
| `data/` | `ContentDatabase`, `MushafRepository`, `TranslationPacks`, `BookmarkStore` (user.db), `Backup` (JSON export/import), `Updater` (GitHub Releases), `Collections` (curated ayah collections), `ReadingHistory` (history.db), models |
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

The `lite` product flavor (`io.zakkyhidayat.quran.lite`) is a reading-only app built from the same code: mushaf pages,
the index lists, page and ayah bookmarks, and the theme setting. `BuildConfig.LITE` hides every other entry point, and
`app/src/lite/AndroidManifest.xml` removes the internet, notification and boot permissions. Its `quran.db` is a
slimmed copy built at build time by `tools/build_lite_db.py` (no thematic tables, surah info or transliteration).
See docs/FORK_LITE.md.

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
- `app/src/test`: JVM unit tests (CI).
- `baselineprofile/`: generates the Baseline Profile on a device.
