# Privacy Policy

Brightness Curve Controller is designed to work locally on your Android device.

## Data The App Uses

- Ambient light sensor readings, reported as lux values.
- Current brightness mode and brightness value from Android system settings.
- Your brightness presets, calibration points, and app settings.
- App version information when checking GitHub Releases for updates.

## Data Stored On Device

The app stores settings and brightness presets locally using Android DataStore and Room. This includes:

- active preset
- auto-control preference
- start-on-boot preference
- brightness bounds and response speed
- custom curve points and revision history
- original brightness settings saved temporarily while automatic control is running

These values stay on your device unless you manually share logs, screenshots, backups, or exported data in the future.

## Network Access

The app uses the internet only for the in-app update flow:

- checking the latest GitHub Release
- downloading the release APK after you choose to download it

The app does not upload sensor readings, brightness settings, presets, device identifiers, or usage analytics.

## Permissions

- `WRITE_SETTINGS`: lets the app write Android system brightness.
- `POST_NOTIFICATIONS`: lets Android 13+ show the foreground-service notification.
- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE`: keeps brightness control active while the app is not in the foreground.
- `RECEIVE_BOOT_COMPLETED`: optionally restarts control after boot.
- `INTERNET`: checks and downloads GitHub Release updates.
- `REQUEST_INSTALL_PACKAGES`: opens Android's installer for downloaded APK updates after user confirmation.

## Update Installation

The app cannot silently install updates. Android always shows the system installer, and you must confirm installation.

## Logs And Bug Reports

Diagnostics shown in the app may include sensor names, lux values, brightness mode, target brightness, applied brightness value, permission state, and last error. If you share screenshots or logs in an issue, review them first.

## Contact

For privacy concerns, open a GitHub issue or follow the security contact guidance in [SECURITY.md](SECURITY.md).
