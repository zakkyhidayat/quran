# ADR 0001: Qur'an text source

- Status: accepted
- Date: 2026-10-08

## Context

The app needs two forms of the text: per-word glyphs that reproduce the printed Madani mushaf page by page, and Unicode
text for search, copying, sharing, and screen readers. Both must be traceable to a recognised publisher and must never
be retyped or edited by hand (or by AI).

## Decision

Use the King Fahd Glorious Quran Printing Complex (KFGQPC) data as published by the Quranic Universal Library (QUL):

- KFGQPC V4 page fonts with tajweed colours and their per-word glyph codes, plus the 15-line page layout;
- QPC Hafs Unicode text for every ayah.

Files are downloaded by hand from QUL (it has no API yet), kept in `data-src/` with SHA-256 checksums in
`docs/data-manifest.json`, and converted by `tools/build_db.py` without altering the text. `tools/verify_quran.py`
checks counts and content fingerprints in CI and, with `--source`, compares every ayah with the original file.

## Alternatives

- **Tanzil:** widely used and well audited, but its Unicode text does not come with matching per-word page glyphs, so
  the page layout would have to be built separately.
- **Kemenag / LPMQ (Mushaf Standar Indonesia):** a different rasm and page layout; see ADR 0002.

## Consequences

- The page view matches the printed KFGQPC mushaf word for word.
- Updates depend on QUL; the V4 fonts were in proofreading when downloaded, so they must be re-checked periodically
  (DATA_SOURCES.md).
- KFGQPC's terms (no sale, no modification of the fonts) apply; see THIRD_PARTY_NOTICES.md.
