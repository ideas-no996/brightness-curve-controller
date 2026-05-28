# Project Documentation

This directory contains project documents that are useful for development,
testing, releases, and audits, but too detailed for the repository root.

## Placement Conventions

- Root directory: only keep public entry and community files, such as
  `README.md`, `CHANGELOG.md`, `LICENSE`, `CONTRIBUTING.md`, `SECURITY.md`,
  and `PRIVACY.md`.
- `docs/audits/`: project audits, risk reviews, open-source readiness notes,
  and UX reviews.
- `docs/diagnostics/`: runtime diagnosis notes and brightness-control
  investigation records.
- `docs/engineering/`: architecture plans, roadmaps, and implementation
  follow-up plans.
- `docs/verification/`: core-loop verification guides, compatibility limits,
  and acceptance checklists.
- `docs/` root: user-facing reference documents such as compatibility,
  releases, FAQ, and versioning policy.

## User And Release Docs

- [Compatibility and test matrix](COMPATIBILITY.md)
- [FAQ](FAQ.md)
- [Release APK instructions](RELEASE.md)
- [Release checklist](RELEASE_CHECKLIST.md)
- [Versioning policy](VERSIONING.md)

## Verification And Diagnostics

- [Core function verification](verification/CORE_FUNCTION_VERIFICATION.md)
- [Compatibility limits](verification/COMPATIBILITY_LIMITS.md)
- [Auto-adjustment diagnosis](diagnostics/AUTO_ADJUSTMENT_DIAGNOSIS.md)

## Engineering Plans

- [Roadmap](engineering/ROADMAP.md)
- [Architecture refactor plan](engineering/ARCHITECTURE_REFACTOR_PLAN.md)
- [Next 10 fixes](engineering/NEXT_10_FIXES.md)

## Audits And Reviews

- [Project audit](audits/PROJECT_AUDIT.md)
- [Bug risk list](audits/BUG_RISK_LIST.md)
- [UX review](audits/UX_REVIEW.md)
- [Open-source readiness](audits/OPEN_SOURCE_READINESS.md)
- [Strict review](audits/STRICT_REVIEW.md)
- [Trust risk](audits/TRUST_RISK.md)
