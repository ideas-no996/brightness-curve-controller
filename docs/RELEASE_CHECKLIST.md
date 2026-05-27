# Release Checklist

Use this checklist before publishing a new APK. The goal is to avoid shipping releases whose version number looks more confident than the evidence.

## Required Local Checks

- `git status --short --branch` is clean before tagging.
- `git diff --check` passes.
- `./gradlew testDebugUnitTest assembleDebug` passes.
- `CHANGELOG.md` contains the target `## vX.Y.Z` section.
- Release notes clearly state whether brightness control logic changed.
- Release notes include `Compatibility note:`, `Core brightness loop physical-device test:`, and `Install integrity:`. The release workflow enforces these strings.

## Required Trust Checks

- README compatibility wording remains conservative.
- Release notes do not claim broad Android support.
- If the APK changes runtime brightness behavior, run the core loop checklist in `CORE_FUNCTION_VERIFICATION.md`.
- If a physical-device core loop test was not run, the release notes must say so.
- If a physical-device test was run, record:
  - device model
  - Android version
  - vendor skin/version
  - app version
  - `WRITE_SETTINGS` state
  - sensor name
  - brightness mode before write
  - target 0-255 brightness
  - read-back brightness
  - final runtime status

## Release Integrity

GitHub Actions uploads:

- `BrightnessCurveController-<version>.apk`
- `SHA256SUMS.txt`

Current release signing certificate fingerprint, extracted from `v1.0.5` with `apksigner verify --print-certs`:

```text
SHA-256: d89f04a7fccd53db21ad3fba7167f0704af52c1792d53799d636259e6a104bd6
SHA-1:   dd23e4096437cdef0572acbe0e3c0ee4e19119b7
DN:      CN=ideas-no996, OU=BrightnessCurveController, O=ideas-no996, L=Zhengzhou, ST=Henan, C=CN
```

If this fingerprint changes unexpectedly, stop the release and investigate signing configuration before publishing.

## Suggested Release Note Block

```text
Compatibility note:
This remains an experimental Android brightness utility. Android 8.0+ is the technical minSdk, not a broad device support guarantee.

Verification:
- Unit tests: passed
- Debug build: passed
- Core brightness loop physical-device test: <performed/not performed>
- Tested device: <device / OS / vendor skin or "not tested">

Install integrity:
Verify the APK SHA-256 with SHA256SUMS.txt. The release APK should be signed with certificate SHA-256 d89f04a7fccd53db21ad3fba7167f0704af52c1792d53799d636259e6a104bd6.
```

## Do Not Release If

- Core brightness writes changed but no one can explain how they were tested.
- Release notes imply compatibility that the matrix does not prove.
- The release APK is unsigned or signed with an unexpected certificate.
- The APK asset exists but `SHA256SUMS.txt` is missing.
- GitHub Release asset digest is missing or does not match the APK.
- A known critical regression is unresolved and undocumented.
