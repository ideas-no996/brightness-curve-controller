# Strict Open Source Review

Date: 2026-05-27

Reviewer stance: strict, trust-first, and core-function-first. This review intentionally does not praise the project. It focuses on what can make users distrust the app, what can make the core brightness loop fail, and what makes the project look less mature than its version number implies.

## Target

Make Brightness Curve Controller credible as a tool-type open source Android app by proving, not merely claiming, that it can complete this loop on real devices:

```text
ambient lux -> curve decision -> target brightness -> system brightness write -> read-back confirmation -> UI state
```

Until this loop is repeatably verifiable, the project should describe itself as an experimental device-tested tool, not a stable Android-wide replacement for system auto brightness.

## Evidence Checked

- Local branch: `main`, clean before this audit.
- Commit count: 18 commits.
- Releases: `v1.0.0` through `v1.0.5`.
- Latest release: `v1.0.5`, published 2026-05-27.
- GitHub repository metadata checked with `gh repo view`: empty description, empty homepage, no topics.
- Core files reviewed:
  - `README.md`
  - `PRIVACY.md`
  - `docs/COMPATIBILITY.md`
  - `docs/RELEASE.md`
  - `app/src/main/AndroidManifest.xml`
  - `app/src/main/java/com/evan/brightnesscurve/brightness/BrightnessController.kt`
  - `app/src/main/java/com/evan/brightnesscurve/service/BrightnessControlService.kt`
  - `app/src/main/java/com/evan/brightnesscurve/service/BrightnessRuntimeState.kt`
  - `app/src/main/java/com/evan/brightnesscurve/ui/HomeScreen.kt`
  - `app/src/main/java/com/evan/brightnesscurve/ui/SettingsScreen.kt`
  - `app/src/main/java/com/evan/brightnesscurve/update/*`

## Current Verdict

The repository now has the surface area of an open source project: README, docs, license, security policy, contribution guide, release automation, signed APK assets, and checksums. That is not enough for a tool that modifies global screen brightness.

The strongest remaining problem is verification. The code has moved closer to a real closed loop: it checks `WRITE_SETTINGS`, writes `SCREEN_BRIGHTNESS_MODE_MANUAL`, writes `SCREEN_BRIGHTNESS` in the 0-255 range, reads brightness back, records runtime status, and shows diagnostics. But the project still lacks a repeatable device-level acceptance path that proves this behavior outside the developer's machine.

## High-Severity Findings

### 1. The Core Loop Is Not Externally Verifiable Enough

The README claims continuous light-sensor reading, curve mapping, smoothing, throttling, dead-zone, ramping, and system brightness writes. The code implements many of these pieces, but a user or maintainer cannot run one clear acceptance script that proves the end-to-end loop on a connected Android device.

What exists:

- `BrightnessControlService` reads lux samples and calls `BrightnessCurveEngine.decide`.
- `BrightnessController.writeManualBrightness` writes manual mode and system brightness, then reads back.
- Runtime diagnostics expose lux, target percent, current system brightness, target system value, read-back value, permission, mode, and last error.
- Unit tests cover curve decisions, runtime status, and percent-to-system-value conversion.

What is missing:

- An adb-driven verification script for a real device.
- A documented pass/fail matrix for the exact core loop.
- A way to export diagnostics as text from the app.
- A release gate requiring at least one successful physical-device run before marking a release stable.

Trust impact: high. For a brightness controller, "the code path exists" is weaker than "a user can prove it changed system brightness and show the proof."

### 2. GitHub About Metadata Is Empty

GitHub still reports an empty description, no homepage, and no repository topics. This is a small fix with a large first-impression cost.

Observed state:

```text
description: ""
homepageUrl: ""
repositoryTopics: null
```

Recommended metadata:

```text
description: Experimental Android brightness curve controller using ambient light sensor and system brightness writes.
topics: android, kotlin, jetpack-compose, brightness, ambient-light-sensor, write-settings, android-app, open-source
```

Trust impact: medium. Empty metadata makes the project look unfinished even when the repository contents are present.

### 3. Compatibility Language Is Too Broad For The Evidence

`README.md` says "theoretically Android 8.0+" and `docs/COMPATIBILITY.md` says the app targets Android 8.0+. The real tested matrix currently lists only an OPPO/ColorOS tablet with unknown Android version.

This is too optimistic for an app that depends on:

- `WRITE_SETTINGS`
- vendor brightness implementation
- foreground service policy
- `TYPE_LIGHT` sensor availability
- battery/background restrictions
- system auto-brightness behavior

Recommended tone: "tested on a limited ColorOS tablet environment; Android 8.0+ is the technical minSdk, not a support guarantee."

Trust impact: high. Overstating compatibility creates bad first-run experiences and low-quality bug reports.

### 4. The Update Flow Adds Suspicious Permissions For A Brightness Tool

The manifest includes `INTERNET` and `REQUEST_INSTALL_PACKAGES`. The README and privacy policy explain that they are for GitHub Release checks and APK installation, but the app UI does not give enough trust context before the user sees or reasons about these permissions.

Current risk:

- A brightness controller asking for network access looks suspicious.
- A brightness controller asking to install APKs looks more suspicious.
- The APK checksum exists in releases, but the in-app updater does not verify it before installation.
- There is no signature fingerprint shown in README, release notes, or the app.

Trust impact: high. Even if the implementation is benign, the permission set can make users uninstall.

### 5. Version Number Looks More Mature Than The Evidence

The project has 18 commits and six releases from `v1.0.0` to `v1.0.5`. The code has recently fixed critical regressions in system brightness control and tutorial persistence.

Problem: `v1.0.x` suggests a stable public contract. The project evidence says "early experimental Android utility with limited device coverage."

Recommended options:

- Prefer future versions such as `v0.2.x` until multiple device families pass the core loop.
- If keeping `v1.0.x`, explicitly mark releases and README as "experimental" and "limited device validation."

Trust impact: medium to high. Fast `1.0.x` iteration after core regressions reads as version inflation.

### 6. Diagnostics Are Visible But Not Portable

The Settings diagnostics panel is useful, but users cannot export a single diagnostic report. Issue templates ask for screenshots or pasted diagnostics, which is fragile.

Required diagnostic report fields:

- app version and build type
- device model and Android version
- vendor/manufacturer
- `canWriteSettings`
- current brightness mode
- current system brightness 0-255
- current brightness percent
- light sensor available/registered/name
- last raw lux and smoothed lux
- active preset
- target percent
- target system brightness
- last write target
- last write read-back
- last write success
- last error/failure reason
- auto desired/running/service state

Trust impact: high. Without exportable diagnostics, support quality depends on screenshots.

## Medium-Severity Findings

### 7. Window Brightness Fallback Can Confuse The Product Promise

The project goal is system brightness control. `MainActivity` still has a current-window brightness fallback when system write permission is missing.

This may be useful as a preview, but it can blur the line between:

- system brightness control, requiring `WRITE_SETTINGS`
- current app window brightness, using `window.attributes.screenBrightness`

The UI must make this distinction impossible to miss. If not, users may report that brightness "works in the app but not outside it," which damages trust.

### 8. Release Integrity Exists But Is Not User-Centered

Release automation uploads `SHA256SUMS.txt`. That is good infrastructure, but the README does not yet give a plain user workflow for verifying the APK, and the app updater does not check the checksum asset.

Required next step:

- Document APK checksum verification for Windows/macOS/Linux.
- Publish signing certificate fingerprint.
- Consider checking release checksum before handing APK to Android installer.

### 9. Tests Are Mostly Unit-Level

Unit tests are valuable but cannot prove Android system setting behavior. The risky parts are platform behavior and vendor behavior, not only pure Kotlin logic.

Missing test/gate types:

- Device smoke test with adb.
- Manual release checklist requiring screenshots or exported diagnostics.
- Test report stored per release.
- Negative-path tests for no sensor, no permission, automatic brightness mode, write rejection, and read-back mismatch on device.

### 10. Failure Copy Is Better Than Before But Still Too Developer-Labeled

Diagnostics use raw keys such as `runtimeStatus`, `hasLightSensor`, `lastWriteTargetValue`, and `lastWriteReadBackValue`. That is useful for developers but not enough for normal users.

Recommended direction:

- Keep raw diagnostics for bug reports.
- Add a short "What this means" line for the current blocker.
- Add "Copy diagnostic report."

## Immediate Goal For Optimization

Do not start with UI polish. Do not add new feature breadth. Make the project trustworthy by doing these in order:

1. Make the README and compatibility docs more conservative.
2. Add a core function verification guide and adb checklist.
3. Add a diagnostic export feature.
4. Add release gates requiring the core loop to be verified.
5. Clarify update permissions and release integrity.
6. Fix GitHub About metadata.
7. Decide whether to continue `v1.0.x` or reset future development to `v0.2.x`.

## Phase Plan

### Phase 1: Audit Only

Create strict audit documents. No Kotlin code changes.

Deliverables:

- `STRICT_REVIEW.md`
- `TRUST_RISK.md`
- `CORE_FUNCTION_VERIFICATION.md`
- `COMPATIBILITY_LIMITS.md`
- `NEXT_10_FIXES.md`

### Phase 2: Trust Documentation And Repository Metadata

Update README, privacy notes, compatibility docs, release docs, and GitHub About metadata. No runtime code changes unless required by documentation accuracy.

### Phase 3: Core Verification Tooling

Add an adb/manual verification guide and a script that captures logs, dumps brightness settings, and produces a verification bundle.

### Phase 4: Diagnostic Export

Add a user-facing "Copy diagnostic report" action that exports the current runtime snapshot and relevant app/device metadata.

### Phase 5: Release Hardening

Add release checklist gates, checksum/signature guidance, and decide versioning policy before the next public APK.
