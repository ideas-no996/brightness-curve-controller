# Trust Risk Review

Date: 2026-05-27

This document lists the issues most likely to make users distrust the app before they ever inspect the code.

## Risk Summary

| Risk | Severity | Current Evidence | Required Fix |
| --- | --- | --- | --- |
| Empty GitHub About metadata | Medium | `gh repo view` reports empty description, empty homepage, no topics | Add description and topics |
| Broad compatibility impression | High | Original README implied Android 8.0+; only OPPO/ColorOS tablet is listed as tested | Support claims rewritten as limited and experimental |
| Suspicious update permissions | High | Manifest includes `INTERNET` and `REQUEST_INSTALL_PACKAGES` | Explain in UI and docs; verify checksums; publish signing fingerprint |
| Core loop not independently proven | High | Code has diagnostics, but no device-level verification script/report | Add core verification guide, script, and release gate |
| Fast `v1.0.x` release sequence | Medium | 6 releases across 18 commits, including recent core regression fixes | Keep history, label future `v1.0.x` releases experimental until compatibility evidence improves |
| No diagnostic export | High | Diagnostics panel exists, issue template asks for screenshots/paste | Add one-tap copy/export report |
| Window brightness fallback ambiguity | Medium | App can modify current window brightness when system write permission is missing | Label as preview-only or remove |
| Release integrity not easy for users | Medium | `SHA256SUMS.txt` exists, but no plain verification workflow or signature fingerprint | Add verification instructions and certificate fingerprint |

## 1. GitHub Metadata

Current GitHub metadata is blank:

```text
description: ""
homepageUrl: ""
repositoryTopics: null
```

This makes the repository look unfinished in search results, repo lists, and social previews.

Recommended values:

```text
Description:
Experimental Android brightness curve controller using ambient light sensor and system brightness writes.

Topics:
android
kotlin
jetpack-compose
brightness
ambient-light-sensor
write-settings
android-app
open-source
```

Do this before any cosmetic app work.

## 2. Permission Trust

The app asks for several sensitive permissions:

- `WRITE_SETTINGS`
- `FOREGROUND_SERVICE`
- `FOREGROUND_SERVICE_SPECIAL_USE`
- `RECEIVE_BOOT_COMPLETED`
- `INTERNET`
- `REQUEST_INSTALL_PACKAGES`

For a brightness app, `WRITE_SETTINGS` is expected. `INTERNET` and `REQUEST_INSTALL_PACKAGES` are harder to trust.

Current mitigation:

- README explains update use.
- Privacy policy says no upload, no analytics, and network is only for GitHub Releases.
- Android still requires user confirmation for installation.

Remaining trust gap:

- In-app Settings area does not strongly explain that network access is only for update checks.
- Downloaded APK checksum is not verified in app.
- Signing certificate fingerprint is not documented.
- Users are not told that update checking can be ignored and the brightness controller still works offline.

Recommended fix:

- Add a short in-app update privacy line before the update buttons.
- Add README and release docs sections: "Network and APK install permissions."
- Add checksum verification to updater or make manual verification prominent.
- Publish release signing certificate fingerprint.

## 3. Core Function Trust

The project cannot rely on "works on my device" for a system brightness controller. Users need proof of the exact chain:

```text
lux observed
target brightness calculated
system brightness target 0-255 generated
WRITE_SETTINGS true
manual brightness mode set
SCREEN_BRIGHTNESS written
SCREEN_BRIGHTNESS read back
UI reports success or failure
```

Current state:

- Code logs and diagnostics cover most fields.
- There is no single exported report.
- There is no script that captures system brightness before and after.

Recommended fix:

- Add `docs/verification/CORE_FUNCTION_VERIFICATION.md` or keep this audit document as the seed.
- Add `scripts/collect-diagnostics.ps1`.
- Add app-side "Copy diagnostic report."
- Require the report for compatibility claims.

## 4. Version Trust

`v1.0.x` implies maturity. The release history says the project is still finding basic reliability problems:

- `v1.0.2`: sensor diagnostics/fallback
- `v1.0.4`: restored system brightness control and auto light adjustment
- `v1.0.5`: fixed tutorial opt-out persistence

This is normal for early software, but it is not normal for a trusted `1.0` utility that modifies a global system setting.

Current policy:

- Keep existing `v1.0.x` history.
- Do not jump future updates backward to `v0.x`, because the in-app updater already compares against `v1.0.x`.
- State "experimental, limited device validation" in future `v1.0.x` release notes until the core loop passes multiple device families.

Do not hide this. Conservative versioning increases trust.

## 5. Release Trust

Release automation uploads:

- `BrightnessCurveController-<version>.apk`
- `SHA256SUMS.txt`

Remaining gaps:

- Release notes do not consistently include install risk notes.
- README does not show a simple checksum verification command.
- No signing certificate fingerprint is published.
- App updater does not verify checksum before installer handoff.

Recommended release note block:

```text
This APK is signed by the project release key. Verify SHA-256 with SHA256SUMS.txt before installing manually. The app cannot silently install updates; Android will always show the system installer.
```

## 6. Support Trust

Bug reports currently depend on users knowing how to screenshot or paste the right diagnostics. This will fail for non-developer users.

Required fix:

- Add a "Copy diagnostic report" button.
- Include device and app version metadata automatically.
- Include the last core-loop result.
- Keep it local and user-initiated.

## Trust Optimization Goal

The next project milestone should not be "more features." It should be:

```text
A new user can understand the risks, verify the APK, run the app on a device, and export proof of whether the brightness control loop succeeded or failed.
```
