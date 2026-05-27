# Project Audit

## Overall Assessment

Brightness Curve Controller is no longer a toy demo. It can read ambient light, control system brightness with `WRITE_SETTINGS`, publish signed APKs through GitHub Releases, and expose basic diagnostics. That is a real foundation.

The project is still fragile as open source software. Its biggest weakness is not one isolated bug, but a blurry runtime model: sensor reads, service state, UI state, permission state, update state, and diagnostics are spread across `MainActivity`, `MainViewModel`, `BrightnessControlService`, and a global `BrightnessRuntimeState`. This makes the app harder to reason about than the feature itself requires.

The next stage should prioritize reliability and readability over more features. The app should make one promise: when it says it is detecting light, adjusting brightness, paused, unsupported, or blocked by permission, that statement must match the actual Android state.

## Current Strengths

- The project has a clear practical purpose: personal brightness control without root.
- Core pieces already exist: light sensor monitor, brightness controller, curve mapping, ramping, presets, diagnostics, tutorial, update flow, release CI.
- GitHub release automation and signed APK publication are working.
- README, License, Changelog, Contributing, and Security docs exist.
- The recent diagnostics work made hidden sensor failures more visible.

## Biggest Problems

1. Runtime state is not a single source of truth.
   `MainViewModel` and `BrightnessControlService` both write to `BrightnessRuntimeState`. This can make UI status depend on update timing rather than actual truth.

2. `MainViewModel` has too many responsibilities.
   It handles permission refresh, passive sensor monitoring, update checking/download/install, preset editing, calibration, service control, and UI aggregation.

3. `BrightnessControlService` mixes orchestration and domain logic.
   It registers sensors, manages foreground service state, observes preferences, calculates target brightness, ramps output, writes settings, records diagnostics, restores original brightness, and manages timeout behavior.

4. UI is too concentrated.
   `MainScreen.kt` contains multiple full screens, tutorial, diagnostics, curve editor, formatting helpers, and status wording. This makes UX iteration risky.

5. The status model is boolean-heavy.
   `isRunning`, `autoEnabled`, `serviceEnabled`, `lightSensorTimedOut`, `canWriteSettings`, and nullable lux fields are combined in the UI. A sealed status model would make invalid combinations harder.

6. Open source readiness is incomplete.
   The project lacks privacy documentation, issue templates, screenshots/GIFs, a true test matrix, a FAQ, and automatic release notes from `CHANGELOG.md`.

## Potential Hidden Risks

- Passive foreground sensor monitoring and service sensor monitoring can overlap or race.
- Service shutdown resets global runtime state, potentially removing useful diagnostic context.
- `Service.onDestroy()` uses `runBlocking`, which may block cleanup during slow storage operations.
- The update downloader trusts the GitHub release asset name and HTTPS transport but does not verify expected SHA-256 or APK signing certificate before install handoff.
- Logs may become noisy because every sensor sample can emit debug logs.
- Brightness write calls do not expose detailed failure reasons.
- The app has unit tests for curve/ramp-like behavior, but little coverage for runtime states, permissions, service lifecycle, and update failure modes.

## Priority Ranking

### P0: Runtime Truth And Safety

- Define a single runtime state machine.
- Make auto-control switch state match actual foreground service state.
- Ensure sensor timeout, no-sensor, permission missing, screen-off pause, and write-failed states cannot conflict.
- Keep diagnostics visible long enough to debug failures.

### P1: Architecture Boundaries

- Move passive sensor monitoring out of `MainViewModel`.
- Extract pure brightness engine logic.
- Make the service orchestrate components instead of owning all logic.
- Replace direct scattered `BrightnessRuntimeState.update` calls with a store/reducer.

### P2: UI Information Architecture

- Split `MainScreen.kt` into page-level files.
- Reduce engineering terms on the default UI.
- Keep diagnostics available but folded behind a clear "Status diagnostics" section.
- Improve curve editing so ordinary users do not need to think in raw lux.

### P3: Open Source Maturity

- Add privacy policy and issue templates.
- Add screenshots or GIFs.
- Add FAQ and compatibility matrix.
- Automate release notes from `CHANGELOG.md`.

## Minimum Viable Repair Version

The next credible repair version should not add a new user feature. It should:

- Introduce explicit runtime statuses.
- Centralize sensor state ownership.
- Extract a testable brightness engine.
- Split the largest UI file.
- Add privacy/issue templates and documentation links.

This can be done in small commits without changing the package name, signing key, release workflow, or existing user data.
