# Compatibility Limits

Date: 2026-05-27

This app is a device-sensitive Android system utility. Compatibility must be described conservatively.

## Correct Compatibility Statement

Recommended wording:

```text
Brightness Curve Controller is an experimental Android utility. It targets Android 8.0+ at the SDK level, but real support depends on the device's light sensor, vendor brightness implementation, foreground service policy, and battery restrictions. It has only been manually verified on a limited OPPO/ColorOS tablet environment so far.
```

Avoid wording that implies broad Android support.

## What `minSdk 26` Does And Does Not Mean

`minSdk 26` means the app can be installed on Android 8.0+ if the device and Android policy allow it.

It does not guarantee:

- a usable `TYPE_LIGHT` sensor exists
- the sensor returns values while the app is running
- the vendor allows reliable foreground sensor access
- `WRITE_SETTINGS` writes behave consistently
- system auto brightness can be safely overridden
- read-back values will exactly match requested values
- background service policy will allow stable long-running control
- boot restore will work after vendor restrictions

## Current Tested Matrix

Current documented tested device coverage is too narrow:

| Device / OS | Android | Sensor read | Brightness write | Foreground service | Status |
| --- | --- | --- | --- | --- | --- |
| OPPO / ColorOS tablet | Unknown | Pass | Pass | Pass | Limited original development test |

This is not enough to claim broad support.

## Device Families Needing Verification

Before claiming "works on Android," test at least:

- Pixel / AOSP-like Android
- Samsung One UI
- Xiaomi MIUI / HyperOS
- OPPO / ColorOS
- Vivo / OriginOS
- Honor / MagicOS
- Lenovo or other tablet vendor firmware
- one custom ROM if possible

## Compatibility Risk Areas

### Light Sensor

Some devices:

- have no ambient light sensor
- expose a sensor but return stale values
- throttle sensor delivery
- stop sensor events in low-power modes
- behave differently when the screen is off

Required app behavior:

- show `NoSensor` when missing
- show `SensorTimeout` when no first lux arrives
- show sensor name and registration state
- allow retry
- include sensor details in exported diagnostics

### Brightness Writes

Some devices:

- clamp brightness values
- round brightness values
- ignore `SCREEN_BRIGHTNESS` while system auto brightness is enabled
- rewrite brightness shortly after the app writes it
- restrict settings writes despite `WRITE_SETTINGS`

Required app behavior:

- switch to manual mode before writing
- read back immediately
- show read-back mismatch
- preserve the original mode/value where possible
- document that exact write/read-back may be vendor-dependent

### Foreground Service

Some vendors aggressively restrict background execution even with a foreground service.

Required app behavior:

- keep notification visible
- explain why the notification exists
- document battery optimization exceptions if needed
- include service running state in diagnostics

### Boot Restore

`RECEIVE_BOOT_COMPLETED` does not guarantee restart on all vendors.

Required documentation:

- "Start on boot is best effort."
- "Vendor battery/autostart settings may block this."

## Support Claim Levels

Use these labels in docs and issues:

### Unsupported

No test result or known failure.

### Installs

APK installs and launches, but core loop is not verified.

### Sensor Verified

`TYPE_LIGHT` registration and lux updates are verified.

### Brightness Write Verified

Manual slider or quick buttons write `SCREEN_BRIGHTNESS` and read back.

### Core Loop Verified

Automatic control completes lux -> target -> write -> read-back -> UI status.

### Stable Candidate

Core loop verified across restarts, screen off/on, permission refresh, and at least 24 hours of normal use.

## Documentation Status

Updated:

- `README.md`: broad Android 8.0+ language has been replaced with limited experimental wording.
- `docs/COMPATIBILITY.md`: support claim levels and core-loop verification requirements are documented.
- `docs/FAQ.md`: vendor-specific brightness and sensor failure notes are documented.
- Release notes: future releases are required to mention whether a physical-device core-loop test was performed.

## Minimum Data For A Compatibility Report

Require:

- app version
- APK source and checksum
- device model
- manufacturer
- Android version
- vendor skin/version
- `WRITE_SETTINGS` granted: true/false
- brightness mode before start
- brightness value before start
- light sensor name
- first lux received time
- target percent after lux change
- target 0-255 brightness value
- read-back brightness value
- last error/failure reason
- battery/autostart settings notes

Without this data, do not mark a device as supported.
