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

- No exported diagnostic report shows the permission state at the moment of failure.

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
  - `brightnessMode`
  - `lastError`

Remaining gaps:

- Exact read-back equality may fail on devices that clamp or transform brightness values. The current behavior correctly surfaces the mismatch, but compatibility docs need to explain this.
- There is no adb script that proves the before/after system setting values.
- No in-app export captures the last write result.

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
- Sensor vendor/name is visible in diagnostics but not exportable.

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

Remaining gaps:

- UI does not show no-write reason directly.
- Diagnostics do not show mapped percent before ramp.
- No exported report includes response speed, min/max, throttle status, and no-write reason together.

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
- The app needs a copied diagnostic report that gives maintainers raw data while giving users a short explanation.

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
adb shell settings get system screen_brightness_mode
adb shell settings get system screen_brightness
adb shell dumpsys sensorservice | Select-String "com.evan.brightnesscurve|Light|Ambient"
adb shell dumpsys activity services com.evan.brightnesscurve | Select-String "BrightnessControlService|foreground"
adb logcat -d -t 1000 | Select-String "BrightnessControlService|BrightnessController|LightSensorMonitor|MainViewModel"
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
- If any step fails, the UI and exported report name the failing step.

## Fail Criteria

A device fails if any of these happen:

- Permission appears granted in UI but `canWriteSettings=false`.
- Lux is never received and no timeout/failure appears.
- Target percent changes but system brightness is not written.
- Brightness write fails silently.
- App only changes current window brightness while presenting it as system brightness.
- User cannot produce enough diagnostics to explain the failure.

## Required Next Artifact

Add a script:

```text
scripts/collect-core-verification.ps1
```

It should collect:

- app version
- device model
- Android version
- brightness mode before and after
- brightness value before and after
- service state
- sensor service evidence
- recent app logcat lines

The script should not be treated as complete until it has been run on at least one physical device.
