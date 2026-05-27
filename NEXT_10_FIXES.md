# Next 10 Fixes

Date: 2026-05-27

This is the execution plan. The order is intentionally conservative: trust and core reliability before UI polish or new features.

## 1. Fix GitHub About Metadata

Type: repository metadata

Action:

- Add description.
- Add topics.

Suggested description:

```text
Experimental Android brightness curve controller using ambient light sensor and system brightness writes.
```

Suggested topics:

```text
android, kotlin, jetpack-compose, brightness, ambient-light-sensor, write-settings, android-app, open-source
```

Acceptance:

- `gh repo view --json description,repositoryTopics` shows non-empty values.

## 2. Rewrite Compatibility Claims

Type: documentation

Action:

- Update README and compatibility docs to say the project is experimental and manually verified only on limited hardware.
- Clarify that Android 8.0+ is a technical installation target, not a support guarantee.

Acceptance:

- README no longer implies broad Android support.
- Compatibility doc defines support levels.

## 3. Add Core Function Verification Guide

Type: documentation

Action:

- Convert `CORE_FUNCTION_VERIFICATION.md` into the canonical device acceptance checklist.
- Link it from README and issue templates.

Acceptance:

- A tester can follow one document to verify lux -> write -> read-back -> UI state.

## 4. Add Diagnostic Export

Type: app code

Action:

- Add "Copy diagnostic report" in Settings diagnostics.
- Include app version, device, runtime snapshot, last write result, permission state, sensor state, active preset, and update permission state.

Acceptance:

- User can paste a complete report into a GitHub issue without screenshots.
- No network upload occurs.

## 5. Add ADB Diagnostic Collection Script

Type: tooling

Action:

- Add `scripts/collect-core-verification.ps1`.
- Collect brightness settings, service state, sensor evidence, and recent logs.

Acceptance:

- Script runs on Windows PowerShell with adb available.
- Output file can be attached to a bug report.

## 6. Clarify Update Permissions In App And Docs

Type: app copy and documentation

Action:

- In Settings -> Software Update, state that network is only used for GitHub Release checks and APK downloads.
- State that update checking is optional and brightness control works offline.
- Update privacy and README with the same wording.

Acceptance:

- A user can understand why `INTERNET` and `REQUEST_INSTALL_PACKAGES` exist before using update features.

## 7. Harden APK Integrity Story

Type: release/security

Action:

- Add checksum verification commands to README and release docs.
- Publish release signing certificate fingerprint.
- Consider verifying `SHA256SUMS.txt` in the app before installer handoff.

Acceptance:

- Release page and docs explain how to verify APK integrity.
- Future app update flow either verifies checksum or clearly tells users how to verify manually.

## 8. Decide Versioning Policy

Type: project governance

Action:

- Decide whether future releases stay `v1.0.x` with explicit "experimental" labeling or move to `v0.2.x`.
- Document the policy in README and release docs.

Acceptance:

- Version numbers no longer imply more stability than the compatibility matrix proves.

## 9. Add Release Gate For Physical Device Verification

Type: CI/release process

Action:

- Add a manual release checklist requiring one current physical-device core-loop verification.
- Store release test notes in `docs/releases/` or release notes.

Acceptance:

- Every future release says what device passed the core loop, or explicitly says no physical device verification was performed.

## 10. Separate System Brightness From Window Preview

Type: app UX and reliability

Action:

- Make current-window brightness fallback clearly labeled as preview-only, or remove it.
- Ensure the home screen never implies system brightness control is active without `WRITE_SETTINGS` and read-back confirmation.

Acceptance:

- UI cannot be misread as controlling global brightness when it is only changing the app window.

## Suggested Work Batches

### Batch A: Documentation Trust Reset

Fixes 1, 2, 3, 6, 7, 8.

Risk: low.

Rollback: revert documentation and repository metadata changes.

### Batch B: Diagnostics And Verification

Fixes 4, 5, 9.

Risk: medium.

Rollback: revert diagnostic export code and script.

### Batch C: Runtime UX Accuracy

Fix 10.

Risk: medium.

Rollback: revert fallback labeling/removal changes.

## Do Not Do Yet

- Do not add new brightness algorithms.
- Do not redesign the UI visually.
- Do not add cloud sync, analytics, or telemetry.
- Do not broaden compatibility claims.
- Do not release another APK until the release notes say exactly what changed and whether a device core-loop test was performed.
