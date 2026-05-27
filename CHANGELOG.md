# Changelog

## v1.0.2

- Fixed the light-sensor status path so valid lux samples are surfaced even while the active brightness preset is still loading.
- Added passive foreground lux monitoring for the app UI, so the home screen can show ambient-light status even when automatic brightness control is off.
- Added a 5-second no-lux timeout with a retry action instead of leaving the app stuck on “waiting for ambient light”.
- Expanded diagnostics with light sensor availability, last lux, lux update time, auto state, target brightness, applied brightness value, write permission, brightness mode, and last error.
- Added sensor and brightness-chain logs for lux samples, sensor registration, target calculation, and brightness writes.
- Added a current-window brightness fallback preview when system brightness write permission is missing.

## v1.0.1

- Added in-app GitHub Release update checking, APK download, and system installer handoff.

## v1.0.0

- First public release with productized brightness curve control, tutorial overlay, adaptive icon, and signed Release APK automation.
