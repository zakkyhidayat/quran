# Translating the interface

Interface text uses standard Android string resources. Each language is one file, and that file is the only place its
text lives:

| Language | File |
|----------|------|
| English (reference) | `app/src/main/res/values/strings.xml` |
| Indonesian | `app/src/main/res/values-in/strings.xml` (Android's legacy folder code for Indonesian) |
| Arabic, Urdu, Bengali, Turkish, Persian, Malay, French, Russian | `app/src/main/res/values-ar/`, `-ur/`, `-bn/`, `-tr/`, `-fa/`, `-ms/`, `-fr/`, `-ru/` |

English is the reference: every key starts there. A key missing from another language falls back to English, so a
partial translation never breaks the app.

After any change, run the checker:

```bash
python tools/check_strings.py            # summary per language
python tools/check_strings.py --missing  # also list the untranslated keys
```

It exits with an error for problems that would break the build or garble text (placeholder mismatches, unescaped
apostrophes, a language not registered in the app), and lists missing or unused keys as information.

## Fixing or revising a translation

Edit the `<string>` in that language's file. Keep the key (`name="..."`) unchanged.

## Adding a new key

1. Add it to `values/strings.xml` (English) and use it from Kotlin with `stringResource(R.string.key)`.
2. Add it to the other languages you can translate; the rest show English until someone translates them.
3. Run `python tools/check_strings.py --missing` to see which languages still need it.

## Adding a new language

1. Copy `values/strings.xml` to `values-<code>/strings.xml` (ISO 639-1 code, for example `values-de`) and translate it.
   Remove the `app_name` line, which is not translatable.
2. Add the code to `supported` and its own name to `endonyms` in
   `app/src/main/java/io/zakkyhidayat/quran/AppLanguage.kt`.
3. Add `<locale android:name="<code>" />` to `app/src/main/res/xml/locales_config.xml` (use `id` for Indonesian there,
   not `in`).
4. Add a `lang_name_<code>` key (the language's name, used for translation labels) to English and, if possible, the other
   languages, and map it in `translationLabel()` in `settings/SettingsScreen.kt`.
5. Run `python tools/check_strings.py`. It confirms that steps 2 and 3 match the folders.

## Writing rules

- Placeholders: keep `%1$s`, `%2$d` and so on exactly as in English. They may move within the sentence; the number says
  which value goes there.
- Apostrophes: write `\'` (for example `Rub\'`) or use the typographic `’`. A bare `'` breaks the build.
- Abbreviations for "page" (`p.` in English, `Hal.` in Indonesian) appear in several keys; keep them consistent within a
  language.
- Quranic terms (juz, hizb, rub', manzil, ruku, sajdah) should use the form readers of that language already know.
- Language names in the language picker are written in their own language and live in `AppLanguage.endonyms`, not in
  string resources.
