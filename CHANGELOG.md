# Changelog

## Unreleased

### Trust And Verification

- Added strict trust, compatibility, and core-loop audit documents.
- Rewrote project documentation to describe the app as an experimental, limited-device Android brightness utility.
- Added GitHub issue guidance for core-loop verification evidence.
- Added a PowerShell adb collection script for core brightness verification bundles.
- Added a copyable in-app diagnostic report covering device, permission, sensor, target brightness, write, read-back, and error state.
- Added release checklist, signing certificate fingerprint, checksum verification guidance, and versioning policy.
- Clarified in-app update permissions and current-window brightness preview behavior.

## v1.0.5

This release fixes the tutorial overlay so the "do not show again" choice is respected after restarting the app.

### Fixes

- Waits for persisted settings to load before deciding whether to auto-show the tutorial on startup.
- Prevents the ViewModel's initial default tutorial settings from triggering the tutorial before DataStore emits the saved opt-out value.
- Records the tutorial as handled when the startup tutorial switch is changed, so disabling it in Settings also prevents first-run auto-show.
- Adds tests for first-run tutorial visibility, saved opt-out behavior, and explicit startup tutorial opt-in.

## v1.0.4

This release fixes a critical regression in the core system-brightness control path. It restores the full ambient-light to curve calculation to system brightness write-back loop.

### Critical Fixes

- Restored real system brightness writes for manual controls and automatic ambient-light adjustment.
- Ensured brightness writes use Android's 0-255 `Settings.System.SCREEN_BRIGHTNESS` range.
- Switched Android to `SCREEN_BRIGHTNESS_MODE_MANUAL` before writing system brightness, then read back the mode and brightness value for confirmation.
- Fixed the comfort slider and quick adjustment buttons so they write system brightness directly instead of only updating preferences or calibration state.
- Added explicit diagnostics for target system brightness, read-back system brightness, write success, brightness mode, and write errors.
- Added brightness-chain logs for lux, target percent, target system value, write permission, brightness mode, write success, and read-back brightness.
- Added tests for percent-to-system-brightness conversion and runtime write diagnostics.

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
