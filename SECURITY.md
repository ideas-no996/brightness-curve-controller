# Security Policy

## Supported Versions

This project is currently in early development. Security fixes target the latest `main` branch.

## Reporting a Vulnerability

Please do not publicly disclose sensitive security issues before there is time to investigate.

Open a GitHub issue for general safety concerns. For issues that include private device details, logs, or security-sensitive behavior, contact the repository owner through the GitHub account `ideas-no996`.

## Safety Notes

This app can write Android system brightness after the user grants `WRITE_SETTINGS`. It does not require root and should not request root access.

Contributors should avoid changes that:

- Write system brightness without explicit user control.
- Disable system auto-brightness silently.
- Hide foreground service behavior.
- Collect or upload sensor data without clear user consent.
