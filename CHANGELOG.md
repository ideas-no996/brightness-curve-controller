# Changelog

## v1.0.3

This release focuses on making automatic brightness control more reliable, easier to diagnose, and easier to maintain.

### Runtime Reliability

- Added an explicit runtime status model for idle, detecting sensor, sensor ready, auto running, permission missing, no sensor, sensor timeout, write failed, and screen-off paused states.
- Centralized runtime state changes behind typed reducer events instead of scattered boolean updates across the UI and service.
- Separated the user's automatic-control preference from the foreground service's actual running state, so the home switch reflects intent while the status text reflects reality.
- Preserved the last meaningful diagnostic context after the service stops, so failures are still visible after shutdown.
- Added typed runtime failure reasons for missing permission, missing sensor, sensor timeout, and brightness write failure.
- Checked Android system brightness write results and surfaced write failures in diagnostics instead of silently assuming success.

### Brightness Engine

- Extracted brightness decision logic into a pure Kotlin `BrightnessCurveEngine`.
- Moved lux smoothing, curve mapping, ramping, write throttling, and no-write reasons out of the foreground service orchestration path.
- Added unit tests for brightness decisions, runtime status priority, diagnostic retention, and typed failure transitions.

### Architecture And UI Maintenance

- Moved passive foreground light-sensor preview logic out of `MainViewModel` into a dedicated sensor component.
- Split the large Compose screen into separate home, preset, curve editor, settings, diagnostics, shared UI, and tutorial files.
- Kept current behavior and saved settings compatible while reducing cross-module state coupling.

### Open Source Readiness

- Added a privacy policy explaining local sensor use, stored settings, update network access, and permissions.
- Added FAQ and compatibility/test matrix documentation.
- Added GitHub issue templates for bug reports and feature requests.
- Updated release automation to extract release notes from `CHANGELOG.md`.
- Added APK SHA-256 checksum publishing for GitHub Releases.

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
