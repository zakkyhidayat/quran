# Releasing

Releases are built by GitHub Actions ([`.github/workflows/release.yml`](../.github/workflows/release.yml)) when a tag
`vX.Y.Z` is pushed. The workflow publishes a signed APK to GitHub Releases (the `github` variant, which can update
itself from inside the app) and keeps a signed AAB of the `play` variant as a workflow artifact for the Play Console.

## Build variants

| Variant | In-app updates | Use |
|---------|----------------|-----|
| `github` | Yes: checks GitHub Releases at most once a day, downloads the APK, opens the system installer | APK on GitHub Releases |
| `play` | No (Play policy forbids self-updating apps); no `REQUEST_INSTALL_PACKAGES` permission | Google Play |

Local builds: `./gradlew :app:assembleGithubDebug` (or `assemblePlayDebug`).

The updater reads `releases/latest`, which skips pre-releases. Keep the `translations` and `build-assets` releases marked
as **pre-release** so they are never offered as app updates.

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

### 2. Page fonts for CI

The 604 page fonts are not in git. Upload them once as a zip to a pre-release named `build-assets`:

```bash
cd app/src/main/assets/fonts && zip -q ../../../../../page-fonts.zip p*.ttf && cd -
gh release create build-assets page-fonts.zip --prerelease --title "Build assets" --notes "Files needed by CI that are not in git (KFGQPC V4 page fonts)."
```

Update it with `gh release upload build-assets page-fonts.zip --clobber` when the fonts change.

## Making a release

1. Make sure `main` builds and the data is up to date (see [DATA_SOURCES.md](DATA_SOURCES.md)).
2. Tag and push:
   ```bash
   git tag v0.2.0
   git push origin v0.2.0
   ```
3. The workflow creates the GitHub release `v0.2.0` with `quran-v0.2.0.apk` and generated notes. Edit the notes on
   GitHub if needed; the app shows them in its update dialog.
4. Download the `play-bundle` artifact from the workflow run for the Play Console.

`versionCode` is `X*10000 + Y*100 + Z`, so every new tag must be higher than the previous one.
