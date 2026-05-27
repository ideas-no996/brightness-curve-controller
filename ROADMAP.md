# Roadmap

## Phase 1: Core Bug And State Reliability

Goal: make the app truthful before making it prettier.

Tasks:

1. Introduce `RuntimeStatus`.
2. Separate desired auto state from actual service state.
3. Replace boolean status combinations in UI with explicit runtime status.
4. Move all runtime state writes behind a single store/reducer.
5. Ensure no-sensor, timeout, permission missing, write failed, and paused states are mutually understandable.
6. Preserve diagnostic context after service stop.
7. Add unit tests for runtime status transitions.

Acceptance:

- UI never shows "waiting" when the app has already failed.
- Auto switch state matches user intent.
- A separate status label shows actual control state.
- Diagnostics show last meaningful failure until cleared or retried.

## Phase 2: UI And Diagnostic Experience

Goal: make the app understandable to ordinary users while keeping developer diagnostics available.

Tasks:

1. Redesign the home status card around one primary state and one next action.
2. Collapse diagnostics by default in Settings.
3. Add contextual actions for permission, retry sensor, stop control, and open diagnostics.
4. Add simple descriptions for presets.
5. Rework curve editing into simple and advanced sections.
6. Add persistent inline errors for important failures instead of snackbar-only messages.

Acceptance:

- A first-time user can tell whether the app is observing, controlling, blocked, or unsupported.
- Debug data is available but not visually dominant.
- Quick calibration remains reachable without scrolling into navigation chrome.

## Phase 3: Architecture And Tests

Goal: reduce maintenance risk.

Tasks:

1. Extract `BrightnessCurveEngine`.
2. Add tests for lux smoothing, log mapping, ramping, dead zone, and invalid inputs.
3. Add fake sensor and fake brightness writer for runtime tests.
4. Split `MainScreen.kt` into page files.
5. Move passive sensor preview out of `MainViewModel`.
6. Remove or delegate legacy domain wrappers.
7. Centralize tuning constants.

Acceptance:

- Brightness decision logic is testable without Android.
- ViewModel does not own Android sensor objects.
- Service orchestration is shorter and easier to audit.
- UI files can be reviewed independently.

## Phase 4: Open Source Release Polish

Goal: make the repository welcoming and self-explanatory.

Tasks:

1. Add `PRIVACY.md`.
2. Add issue templates.
3. Add screenshots/GIFs.
4. Add FAQ.
5. Add compatibility/test matrix.
6. Automate release notes from `CHANGELOG.md`.
7. Publish APK SHA-256 checksums.

Acceptance:

- A user can decide whether to install without reading code.
- A tester can report actionable issues.
- A maintainer can publish a release without manual note edits.

## Minimum Viable Repair Version

This should be delivered as small commits:

1. Add audit documents.
2. Add runtime status model.
3. Centralize runtime state mutations.
4. Extract brightness engine.
5. Move sensor ownership out of ViewModel.
6. Split UI files.
7. Add privacy and issue templates.
8. Update release workflow notes.

Do not add new user-facing features during this repair version unless they directly support reliability, diagnosis, or open-source readiness.
