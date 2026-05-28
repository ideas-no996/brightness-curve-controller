# Core Function Verification

Date: 2026-05-27

This document defines the acceptance standard for the app's most important promise: reading ambient light and writing system brightness.

## Core Loop

The app is not trustworthy until this loop is observable and repeatable:

```text
TYPE_LIGHT lux
-> smoothed lux
-> BrightnessCurveEngine target percent
-> 0-255 Android system brightness value
-> Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
-> Settings.System.SCREEN_BRIGHTNESS write
-> Settings.System.SCREEN_BRIGHTNESS read-back
-> runtime status and diagnostics update
```

## Current Implementation Evidence

### Permission

Required evidence:

- `Settings.System.canWrite(context)` is checked at runtime.
- Permission is refreshed after returning from Android settings.
- Missing permission becomes a visible runtime failure.

Current code evidence:

- `BrightnessController.canWrite()`
- `MainViewModel.refreshWritePermission()`
- `BrightnessControlService.onStartCommand()`
- `RuntimeEvent.ServicePermissionMissing`
- `RuntimeStatus.PermissionMissing`

Remaining gap:

- The copied diagnostic report includes permission state, but it still needs real issue-report feedback from failed devices.

### System Brightness Write

Required evidence:

- Target percent is converted to 0-255.
- System brightness mode is switched to manual.
- `SCREEN_BRIGHTNESS` is written.
- The value is read back.
- Failure is surfaced instead of silently ignored.

Current code evidence:

- `BrightnessController.percentToSystemValue(percent)`
- `BrightnessController.writeManualBrightness(percent)`
- `RuntimeEvent.ServiceBrightnessWritten`
- `RuntimeEvent.ServiceBrightnessWriteFailed`
- Settings diagnostics panel fields:
  - `currentSystemBrightness`
  - `currentBrightnessPercent`
  - `lastWriteTargetValue`
  - `lastWriteReadBackValue`
  - `lastWriteSucceeded`
  - `targetSystemBrightness`
  - `brightnessMode`
  - `lastError`

Remaining gaps:

- Exact read-back equality may fail on devices that clamp or transform brightness values. The current behavior correctly surfaces the mismatch, but compatibility docs need to explain this.
- The adb script and copied diagnostic report now capture write evidence, but they still need to be exercised on more physical devices.

### Ambient Light Sensor

Required evidence:

- `TYPE_LIGHT` sensor exists.
- Listener registration succeeds.
- First lux sample arrives within a timeout.
- Invalid lux values are rejected.
- Timeout and no-sensor states are visible.

Current code evidence:

- `LightSensorMonitor` uses `Sensor.TYPE_LIGHT`.
- It reports unavailable sensor and registration failure.
- Invalid, NaN, infinite, and negative lux are ignored.
- `BrightnessControlService` and passive preview dispatch timeout states.
- `RuntimeStatus.NoSensor` and `RuntimeStatus.SensorTimeout` exist.

Remaining gaps:

- Timeout threshold is fixed at 5 seconds and not documented as a compatibility assumption.
- Sensor name is exportable through the copied diagnostic report; sensor vendor is still only visible in adb `sensorservice` output.

### Curve Decision

Required evidence:

- raw lux becomes smoothed lux.
- smoothed lux maps through the active curve.
- min/max bounds are applied.
- ramping and throttling are applied.
- no-write reason is visible enough to debug.

Current code evidence:

- `BrightnessCurveEngine.observeLux`
- `BrightnessCurveEngine.decide`
- `BrightnessMapping.targetPercent`
- service logs `targetPercent`, `targetSystemBrightness`, `shouldWrite`, and `noWriteReason`

Remaining gap:

- Diagnostics now show target system brightness and no-write reason, but still do not show mapped percent before ramp.

### UI State

Required evidence:

- UI distinguishes permission missing, no sensor, waiting first lux, sensor ready but auto off, auto adjusting, write failed, and sensor timeout.

Current code evidence:

- Runtime statuses include:
  - `PermissionMissing`
  - `NoSensor`
  - `DetectingSensor`
  - `SensorReady`
  - `AutoRunning`
  - `WriteFailed`
  - `SensorTimeout`
  - `PausedScreenOff`

Remaining gaps:

- The status enum is strong, but normal users still see a mix of friendly labels and raw diagnostic names.
- The copied diagnostic report gives maintainers raw data; the user-facing explanation still needs refinement from real support cases.

## Required Manual Acceptance Test

Use this checklist before claiming a device is supported.

1. Fresh install the APK.
2. Open app and do not grant `WRITE_SETTINGS`.
3. Confirm UI says permission is missing and does not claim system brightness control is active.
4. Grant `WRITE_SETTINGS`.
5. Return to app and confirm permission state updates without force-stopping the app.
6. Record current Android brightness mode and brightness value with adb.
7. Move the comfort slider or tap quick adjustment.
8. Confirm system brightness visibly changes outside the app.
9. Confirm adb read-back value changed.
10. Enable automatic control.
11. Confirm foreground service starts.
12. Cover and uncover the light sensor.
13. Confirm lux changes in diagnostics.
14. Confirm target percent changes.
15. Confirm target system value is written and read back.
16. Turn the screen off and on.
17. Confirm the service does not crash and resumes as documented.
18. Stop automatic control.
19. Confirm original brightness mode/value restore is attempted.
20. Export or capture diagnostics.

## Suggested ADB Commands

Windows PowerShell:

```powershell
.\scripts\collect-core-verification.ps1 -Interactive
```

The script writes a local `diagnostics/core-verification-*` bundle containing device properties, package details, brightness settings, sensor service output, service state, and filtered app logs. The `diagnostics/` directory is ignored by git.

Manual commands:

```powershell
adb shell settings get system screen_brightness_mode
adb shell settings get system screen_brightness
adb shell dumpsys sensorservice | Select-String "com.evan.brightnesscurve|Light|Ambient"
adb shell dumpsys activity services com.evan.brightnesscurve | Select-String "BrightnessControlService|foreground"
adb logcat -d -t 1000 | Select-String "AutoBrightness|BrightnessControlService|BrightnessController|LightSensorMonitor|MainViewModel"
```

These commands are not enough by themselves. They must be paired with UI diagnostics and real brightness changes.

## Pass Criteria

A device passes only if all of these are true:

- `WRITE_SETTINGS=true` after authorization.
- `TYPE_LIGHT` sensor registers.
- A valid lux sample arrives.
- Automatic control starts a foreground service.
- A target percent is computed.
- A target 0-255 system brightness value is computed.
- Brightness mode is manual at write time.
- `SCREEN_BRIGHTNESS` write succeeds.
- Read-back matches or the documented device-specific tolerance accepts it.
- UI shows auto adjusting or a specific failure state.
- If any step fails, the UI and copied diagnostic report name the failing step.

## Fail Criteria

A device fails if any of these happen:

- Permission appears granted in UI but `canWriteSettings=false`.
- Lux is never received and no timeout/failure appears.
- Target percent changes but system brightness is not written.
- Brightness write fails silently.
- App only changes current window brightness while presenting it as system brightness.
- User cannot produce enough diagnostics to explain the failure.

## In-App Diagnostic Report

Settings -> Diagnostics includes a "复制诊断报告" action. The report is copied locally to the clipboard and is not uploaded. It includes:

- app version and settings load state
- device manufacturer, model, Android version, SDK, and fingerprint
- permission and brightness mode
- current brightness, target percent, last write target, read-back value, and write success
- target system brightness and last no-write reason
- raw lux, smoothed lux, light sensor name, registration state, and timeout state
- service running state, active preset, runtime message, and last error
- update permission/status fields

## Verification Tool Status

The repository includes:

```text
scripts/collect-core-verification.ps1
```

It collects:

- app version
- device model
- Android version
- brightness mode before and after
- brightness value before and after
- service state
- sensor service evidence
- recent app logcat lines

Current status: script added. It still needs to be run on at least one physical device and refined from real output.
