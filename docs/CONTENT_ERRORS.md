# If an error is found in the Qur'an text

A wrong letter, diacritic, pause mark, or ayah number is not an ordinary bug. Handle it before anything else.

## 1. Confirm

1. Get the exact location: surah, ayah, mushaf page, and a screenshot. Note which view shows it: the mushaf page (V4
   glyph fonts) or the ayah sheet, search, or list modes (Unicode QPC Hafs text). They come from different files.
2. Compare with the printed Madani mushaf of the King Fahd Glorious Quran Printing Complex (KFGQPC), same page number,
   and with the same resource on [QUL](https://qul.tarteel.ai).
3. Run `python tools/verify_quran.py --source`. If it reports a difference, the app no longer matches its source
   (a pipeline or build problem). If it passes, the source itself carries the error.

## 2. Fix

Never edit Qur'an text by hand, in the database or in code.

- **App differs from source:** find what changed (`git log -p -- app/src/main/assets/quran.db tools/build_db.py`),
  rebuild from the untouched source with `python tools/build_db.py`, and confirm `verify_quran.py --source` passes.
- **Source is wrong:** report it to QUL ([GitHub issues](https://github.com/TarteelAI/quranic-universal-library/issues))
  and, for fonts or layout, to KFGQPC. Wait for the corrected resource, download it, and follow
  [DATA_SOURCES.md](DATA_SOURCES.md). If a release cannot wait, a temporary correction must come from KFGQPC's own data,
  be recorded here with the reference used, and be removed once the source is fixed.
- **Rendering only** (the data is right but a glyph or mark draws wrongly on some devices): treat it as a font or
  rendering bug, note the device and Android version, and check the same page in another KFGQPC V4 reader.

After an intentional data change, record the new fingerprints with `python tools/verify_quran.py --write` and commit
`docs/quran-content.json` together with `quran.db`, so the change is visible in review.

## 3. Release quickly

1. Bump the patch version: `git tag vX.Y.(Z+1)` and push the tag; the release workflow builds and publishes the APK
   (see [RELEASING.md](RELEASING.md)).
2. Users of the GitHub build are offered the update the next time they open the app; mention the correction in the
   release notes. Upload the Play bundle to the Play Console as well, if published there.
3. Reply to whoever reported the error, and add a line under "Corrections" below.

## Corrections

None so far.
