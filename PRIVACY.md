# Privacy policy

Quran (`io.zakkyhidayat.quran`) does not collect, sell, or share personal data. It has no account, no ads, no analytics,
no crash reporting, and no third-party tracking SDKs.

## Data on your device

Bookmarks, your last read position, and your settings are stored only on your device. You can export them to a JSON
file you choose (Settings → Backup), for example on the device or in a cloud storage app; the app does not read that file
unless you restore from it or turn on auto backup for it.

Android may include the app's data in your device's own backup to your Google account, as for any app that allows
backup. This is controlled by your device settings, not by the app.

## Network use

On first launch, the app downloads the required Qur'an database and all 604 page fonts from the project's stable
`quran-data` GitHub Release. You can manually download/repair this data again in Settings when needed. Files are stored
in private app storage (`noBackupFilesDir`): other apps and file managers cannot remove them, but clearing the app's
storage or uninstalling the app deletes them. After installation, reading works offline. The app does not check for
updates automatically; new app versions are downloaded by you from GitHub Releases or Google Play.

The internet is also used to download translations you add and the list of available translations.

Database, page-font, and translation downloads go to GitHub (`github.com`, `api.github.com`, and GitHub's download servers). Like
any web server, GitHub receives your IP address and basic request information; see GitHub's privacy statement. The app
sends no identifiers of its own.

## Permissions

| Permission | Why |
|------------|-----|
| Internet | Required first-run Qur'an database/page-font download, optional manual repair/update, and translation downloads and catalogue |
| Notifications (Android 13+, asked when you turn on the reminder) | Show the optional daily reading reminder |
| Run at startup | Set the reminder again after the device restarts or the app updates; only used when the reminder is on |

## Contact

Questions or concerns: open an issue at https://github.com/zakkyhidayat/quran/issues.

Last updated: 2026-10-12.
