# Releasing

Releases are built by GitHub Actions ([`.github/workflows/release.yml`](../.github/workflows/release.yml)) when a tag
`vX.Y.Z` is pushed. The workflow publishes signed APKs to GitHub Releases (the `github` variant and Quran Lite) and keeps a signed AAB of the `play` variant as a workflow artifact for the Play Console.

## Build variants

None of the variants updates itself; users download new versions from GitHub Releases or get them from Google Play.

| Variant | App | Use |
|---------|-----|-----|
| `github` | Full app (`io.zakkyhidayat.quran`) | `Quran.apk` on GitHub Releases |
| `play` | Full app, same code as `github` | AAB for Google Play |
| `lite` | Reading-only app (`io.zakkyhidayat.quran.lite`) | `Quran-Lite.apk` on the same GitHub release, signed with the same key |

Both APKs omit `quran.db` and the 604 page fonts. On first launch, and whenever a user chooses to repair/update data in
Settings, the app downloads its flavor-specific database and shared page-font archive from the stable `quran-data`
release. It verifies sizes, SHA-256 hashes, database integrity, and all 604 fonts before storing them in private app
storage for offline reading. Reading remains blocked until both are installed. App version releases contain only the
two APKs; the Play AAB remains a workflow artifact.

Local builds: `./gradlew :app:assembleGithubDebug` (or `assemblePlayDebug`, `assembleLiteDebug`).

The README links straight to `releases/latest/download/Quran.apk` and `Quran-Lite.apk`, so the APK names carry no
version. Keep `quran-data` and `translations` marked as **pre-release** so neither interferes with the `latest` app
release. These stable, named releases are the download channels for Quran data and translation packs.

## One-time setup

### 1. Release keystore

Create it once and keep it safe (a password manager plus an offline copy). If it is lost, installed apps can no longer be
updated; users would have to uninstall and reinstall.

```bash
keytool -genkeypair -v -keystore quran-release.jks -alias quran -keyalg RSA -keysize 4096 -validity 36500
```

Add these repository secrets (Settings → Secrets and variables → Actions):

| Secret | Value |
|--------|-------|
| `RELEASE_KEYSTORE_BASE64` | `base64 -w0 quran-release.jks` (on Windows Git Bash: `base64 -w0`; PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("quran-release.jks"))`) |
| `RELEASE_KEYSTORE_PASSWORD` | the keystore password |
| `RELEASE_KEY_ALIAS` | `quran` (or the alias you chose) |
| `RELEASE_KEY_PASSWORD` | the key password |

Never commit the keystore; `*.jks` is in `.gitignore`.

### 2. Quran data release

The 604 page fonts are not in git. Create the stable `quran-data` pre-release once with the database archives,
`quran-data.json`, and the unchanged `page-fonts.zip`. The `Qur'an data` workflow replaces these assets in place
(`--clobber`) after a reviewed update to `app/src/main/assets/quran.db`; it downloads the existing font archive unless
the font asset is updated separately:

```bash
gh release create quran-data quran-full.db.gz quran-lite.db.gz page-fonts.zip quran-data.json \
  --prerelease --title "Qur'an data" --notes "Stable download channel for the Quran database and page fonts."
```

To refresh the databases after a QUL update, update and verify the committed source database, then run Actions →
`Qur'an data` → Run workflow on `main`. The workflow regenerates Lite, checks both databases, and overwrites the four
assets without creating another release entry. To update the page fonts, replace `page-fonts.zip` on the `quran-data`
release first, then run the workflow.

## Making a release

1. Make sure `main` builds and the data is up to date (see [DATA_SOURCES.md](DATA_SOURCES.md)).
2. Start the release, either way:
   - **From GitHub:** Actions → Release → Run workflow (branch `main`), enter the new version, e.g. `v0.3.0`. The
     workflow tags the latest `main` commit and builds.
   - **From a computer:**
     ```bash
     git tag v0.3.0
     git push origin v0.3.0
     ```
3. The workflow creates the GitHub release `v0.3.0` with `Quran.apk` and `Quran-Lite.apk`, with generated notes.
   Database and fonts remain in the stable `quran-data` release and can be updated without publishing new APKs.
4. Download the `play-bundle` artifact from the workflow run for the Play Console.

`versionCode` is `X*10000 + Y*100 + Z`, so every new tag must be higher than the previous one.
