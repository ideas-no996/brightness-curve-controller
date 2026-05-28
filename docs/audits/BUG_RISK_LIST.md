# Bug Risk List

## Critical

### Runtime State Has Multiple Writers

- **Impact:** UI can show a state that does not match the actual service. For example, passive sensor reads can update lux while automatic control is stopped, and service reset can erase diagnostics after failure.
- **Likely cause:** `MainViewModel` and `BrightnessControlService` both call `BrightnessRuntimeState.update`.
- **Suggested fix:** Introduce `BrightnessRuntimeStore` with explicit event reducers. Only one owner should mutate runtime state; UI and service should dispatch typed events.

### Sensor Ownership Is Ambiguous

- **Impact:** The app may register two light sensor listeners: one passive foreground listener and one service listener. This can waste battery and produce confusing diagnostics.
- **Likely cause:** `MainViewModel.startPassiveLightSensor()` and `BrightnessControlService.startSensor()` both use `LightSensorMonitor`.
- **Suggested fix:** Create one `LightSensorManager` with a clear mode: `PassivePreview`, `AutoControl`, or `Stopped`. It should register exactly one listener per process.

## High

### Service Shutdown Uses Blocking Work

- **Impact:** `Service.onDestroy()` can block while restoring settings and writing DataStore values. Slow IO may delay shutdown or create lifecycle edge cases.
- **Likely cause:** `runBlocking(Dispatchers.IO)` inside `BrightnessControlService.onDestroy`.
- **Suggested fix:** Move restore into structured coroutine cleanup, make it idempotent, and keep emergency brightness restore small and synchronous only when required.

### Brightness Write Failures Are Not Typed

- **Impact:** The app cannot distinguish permission loss, system setting rejection, OEM override, invalid mode, or write race.
- **Likely cause:** `BrightnessController.writeManualBrightness` returns a value but not a result type.
- **Suggested fix:** Return `BrightnessWriteResult.Success` or typed failures. Record the failure in runtime diagnostics.

### App Update Flow Does Not Verify Download Integrity

- **Impact:** The app relies on GitHub HTTPS and Android installer signature checks, but does not verify that the downloaded asset matches an expected digest.
- **Likely cause:** `UpdateDownloader` downloads any matching APK asset without comparing SHA-256 from release metadata.
- **Suggested fix:** Add optional SHA-256 asset or release metadata check. At minimum, show expected signing fingerprint in docs and verify APK certificate before install handoff if feasible.

### Auto Switch Semantics Are Still Blurry

- **Impact:** The UI switch is checked using `serviceEnabled || runtime.isRunning`, which can mask differences between "desired enabled" and "actually running".
- **Likely cause:** Settings preference and runtime service state are merged in UI.
- **Suggested fix:** Model `desiredAutoEnabled` and `actualAutoState` separately. The switch reflects desired state, while status text reflects actual state.

## Medium

### `MainScreen.kt` Is Too Large

- **Impact:** UI changes are hard to review and easy to regress. A small wording change touches a large file that also contains curve editing and diagnostics.
- **Likely cause:** All tabs and dialogs are implemented in one file.
- **Suggested fix:** Split into `HomeScreen`, `PresetScreen`, `CurveScreen`, `SettingsScreen`, `DiagnosticsPanel`, and `TutorialOverlay`.

### Domain Logic Exists In Multiple Forms

- **Impact:** `domain/BrightnessSmoother.kt`, `brightness/BrightnessRamp.kt`, and service-level logic can diverge.
- **Likely cause:** Older compatibility wrappers remain after refactors.
- **Suggested fix:** Consolidate brightness decision logic in a single `BrightnessCurveEngine` and make legacy wrappers delegate to it or remove them.

### Magic Numbers Are Scattered

- **Impact:** Sampling interval, write throttle, timeout, smoothing alpha, outdoor cap, brightness bounds, and quick calibration delta are hard to tune coherently.
- **Likely cause:** Constants live in several files and UI functions.
- **Suggested fix:** Add `BrightnessTuningDefaults` or `RuntimeTuning` with named constants and documentation.

### Release Notes Require Manual Correction

- **Impact:** GitHub Release notes default to `Release vX.Y.Z` unless manually edited after workflow completion.
- **Likely cause:** Workflow does not read `CHANGELOG.md`.
- **Suggested fix:** Add a release note extraction step for the matching version section.

### Diagnostics Are Too Technical For Default Settings

- **Impact:** Useful for developers, intimidating for ordinary users.
- **Likely cause:** Diagnostics panel is placed directly on Settings page.
- **Suggested fix:** Default-fold diagnostics and split "user status" from "developer details".

## Low

### README Has No Screenshots Or GIFs

- **Impact:** New users cannot quickly understand what the app does before installing.
- **Suggested fix:** Add screenshots for home, calibration, curve, settings, and update flow.

### No Issue Templates

- **Impact:** Bug reports may omit device model, Android version, permission state, logs, and sensor details.
- **Suggested fix:** Add bug report and feature request templates.

### Privacy Story Is Implied, Not Explicit

- **Impact:** Users may worry about sensor data or update network access.
- **Suggested fix:** Add `PRIVACY.md` explaining local-only brightness/sensor use and GitHub-only update checks.

### Scripts Contain Local Development Assumptions

- **Impact:** New contributors may copy local paths or expect Windows-only scripts to be canonical.
- **Suggested fix:** Keep scripts documented as optional; prefer Gradle wrapper commands in docs.
