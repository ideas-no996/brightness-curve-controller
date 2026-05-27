# Versioning Policy

Brightness Curve Controller currently has public releases from `v1.0.0` to `v1.0.5`. Those tags will not be rewritten.

However, the project should be described by its evidence, not by the historical tag number. Current maturity is experimental because device coverage is limited and the core brightness loop still needs broader verification.

## Policy

- Existing `v1.0.x` tags remain as historical releases.
- Future releases must say whether a physical-device core loop test was performed.
- Release notes must not imply broad Android support unless the compatibility matrix proves it.
- If future work remains primarily experimental, prefer `v0.2.x` or explicitly label `v1.0.x` releases as experimental.
- A stable `1.x` claim requires multiple device families to reach `Core Loop Verified` or better in `docs/COMPATIBILITY.md`.

## Stable Release Bar

A release can be described as stable only when:

- core loop passes on multiple device families
- no known critical brightness write regression is open
- diagnostics export is available for bug reports
- release APK checksum and signing fingerprint are documented
- release notes include device verification evidence

Until then, use conservative language:

```text
experimental Android brightness utility
limited device validation
Android 8.0+ technical minSdk, not broad support
```
