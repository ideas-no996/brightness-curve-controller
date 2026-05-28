# Open Source Readiness

## Current State

The project has a reasonable start:

- MIT License exists.
- README explains purpose, permissions, build, USB install, release APKs, and project structure.
- CHANGELOG exists.
- CONTRIBUTING exists.
- SECURITY exists.
- GitHub Actions builds debug APKs and signed release APKs.
- Release assets are published through tags.

It is not yet "comfortable" open source. A new user can install it, but a new contributor cannot easily report a high-quality bug, understand privacy behavior, inspect screenshots, or verify release notes.

## Must Complete Before A More Public Release

### Add `PRIVACY.md`

Explain clearly:

- Ambient light readings are used locally to calculate brightness.
- Brightness settings and calibration points are stored locally.
- The app contacts GitHub only when checking for updates.
- The app does not include analytics, ads, tracking SDKs, or telemetry.
- Logs may contain sensor values if the user captures logcat for debugging.

### Add Issue Templates

Add:

- `.github/ISSUE_TEMPLATE/bug_report.yml`
- `.github/ISSUE_TEMPLATE/feature_request.yml`

Bug reports should request:

- device model
- Android/OEM version
- app version
- permission state
- whether light sensor exists
- steps to reproduce
- screenshots
- diagnostics panel values
- relevant logcat lines

### Add Screenshots Or GIFs

Minimum screenshots:

- Home screen
- Permission prompt flow
- Quick calibration
- Curve editor
- Settings diagnostics
- In-app update flow

Add image links to README.

### Add FAQ

Cover:

- Why does the app need `WRITE_SETTINGS`?
- Why can it not install updates silently?
- What happens if the device has no light sensor?
- Why does the foreground service notification appear?
- Does the app upload sensor data?
- Why does the system auto-brightness mode change?

### Automate Release Notes

Update GitHub Actions so release notes come from the matching `CHANGELOG.md` section. This avoids manually editing release notes after each tag.

### Add Test Matrix

Document verified devices:

- device model
- Android version
- OEM skin
- light sensor status
- WRITE_SETTINGS behavior
- install update behavior

## Optional Enhancements

- `CODE_OF_CONDUCT.md`
- architecture diagram
- signed APK verification instructions
- GitHub Discussions
- roadmap badges
- Kotlin/Dokka API docs for engine modules after refactor
- a small troubleshooting script for collecting logcat and dumpsys output

## README Improvements

Add:

- screenshots near the top
- "Install latest APK" link
- short privacy note
- common failure states
- "How to report bugs" section
- link to `PRIVACY.md`
- link to issue templates

## CI/CD Improvements

- Add release notes extraction from `CHANGELOG.md`.
- Upload debug APK as artifact on main builds for testers.
- Add lint or static check once the code structure stabilizes.
- Add a workflow status badge to README.

## Release Safety

- Keep release keystore outside git.
- Keep secrets in GitHub Actions secrets.
- Document that users must install updates signed with the same key.
- Consider publishing SHA-256 checksums next to APK assets.

## Open Source Readiness Verdict

The repository is publishable for early testers. It is not yet polished enough for broad non-technical users. The biggest public-readiness gaps are privacy documentation, screenshots, issue templates, and a more understandable troubleshooting path.
