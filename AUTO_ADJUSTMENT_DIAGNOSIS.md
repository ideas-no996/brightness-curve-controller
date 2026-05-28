# Auto Adjustment Diagnosis

Date: 2026-05-28

Bug class: core background auto-adjustment failure.

This is not the same as a generic system brightness write failure. Manual brightness control already working means the likely failure area is:

```text
background light sensor -> foreground service -> auto decision -> write strategy -> system brightness write
```

## Reported Symptom

Manual controls can change system brightness, but automatic brightness does not react to environmental light changes while the app is in the background. The user has to reopen the app or tap a manual quick action before brightness changes.

## Current Chain Diagram

```text
Home switch enabled
-> MainViewModel.setAutoControlEnabled(true)
-> AppPreferencesRepository.setAutoControlEnabled(true)
-> ServiceController.start(context)
-> ContextCompat.startForegroundService(...)
-> BrightnessControlService.onCreate()
-> ServiceCompat.startForeground(...)
-> BrightnessControlService.onStartCommand()
-> permission check with BrightnessController.canWrite()
-> preset defaults ensured
-> original brightness captured
-> LightSensorMonitor.start()
-> SensorManager.registerListener(TYPE_LIGHT)
-> LightSensorMonitor.onSensorChanged(lux)
-> BrightnessControlService.handleLuxSample(...)
-> BrightnessCurveEngine.decide(...)
-> shouldWrite / noWriteReason
-> BrightnessController.writeManualBrightness(targetPercent)
-> Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
-> Settings.System.SCREEN_BRIGHTNESS
-> read back Settings.System.SCREEN_BRIGHTNESS
-> RuntimeEvent.ServiceBrightnessWritten / ServiceBrightnessWriteFailed
-> UI diagnostics update
```

## Diagnosis From Current Code

### 1. Is background automatic adjustment completely missing?

Judgment: no. The code has a real foreground service path.

Evidence:

- `ServiceController.start()` uses `ContextCompat.startForegroundService`.
- `BrightnessControlService.onCreate()` calls `ServiceCompat.startForeground`.
- `BrightnessControlService.startSensor()` calls `LightSensorMonitor.start()`.
- `LightSensorMonitor.start()` registers `Sensor.TYPE_LIGHT`.
- `LightSensorMonitor.onSensorChanged()` passes lux samples to the service.
- `BrightnessControlService.handleLuxSample()` calls `BrightnessCurveEngine.decide(...)`.
- If `decision.shouldWrite == true`, the service calls `brightnessController.writeManualBrightness(targetPercent)`.
- `BrightnessController.writeManualBrightness()` switches to manual brightness mode, writes `SCREEN_BRIGHTNESS`, and reads it back.

### 2. Could it be reading lux only while the app UI is open?

Judgment: the main auto path is not UI-bound, but there is also a passive UI preview path.

Evidence:

- Auto path: `BrightnessControlService` owns `LightSensorMonitor` and should keep listening in a foreground service.
- UI preview path: `MainViewModel` owns `PassiveLightSensorPreview`, which is foreground/UI-oriented.

Risk:

- Users may see lux updates in the UI preview and assume the background service is alive.
- Current diagnostics distinguish `serviceRunning` and `sensorRegistered`, but logs are split across tags and do not provide one end-to-end proof line.

### 3. Could the service fail to start or stop in background?

Judgment: possible on real devices, especially vendor ROMs, but the code intends to use a foreground service correctly.

Evidence:

- Manifest declares `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_SPECIAL_USE`.
- Service calls `startForeground`.
- Notification strings say it is adjusting brightness.
- `onDestroy()` unregisters sensor and restores original brightness.

Missing evidence before the fix:

- No single `AutoBrightness` lifecycle log for service start, sensor registration, service destroy, and restart.
- No device-level proof that `onSensorChanged` continues while the app is backgrounded.

### 4. Could lux be received but not trigger brightness writes?

Judgment: yes, this is plausible and must be separated from "no background sensor" by logs.

Current decision gates:

- no active preset: lux is observed but write is skipped.
- screen off: lux is observed but write is paused.
- invalid curve result: write is skipped.
- write permission lost: write is skipped.
- throttle: write is skipped.
- delta too small: write is skipped.

Current strategy values:

- Sensor sampling: `250_000` microseconds, about 250 ms.
- Engine minimum write interval: 220 ms.
- Minimum percent delta: 1%.
- Minimum system brightness delta: 2 steps.
- Standard EMA alpha: 0.35.
- Standard ramp: brighten by 4%, darken by 3% per decision.

This is not "too slow"; if anything, the current write interval is shorter than the recommended 3-10 seconds. The reported "no change until app opens" is more likely service/sensor lifecycle, state sync, permission loss, or skipped-decision opacity than a long throttle interval.

### 5. Could the default curve make changes look like no change?

Judgment: yes, in some indoor-to-bright-indoor cases.

Evidence:

The default `护眼室内 20%` curve holds brightness at 20% from 100 lux to 500 lux:

```text
100 lux -> 20%
500 lux -> 20%
1000 lux -> 35%
5000 lux -> 65%
```

If the sensor reports only a modest lux increase, target brightness may remain unchanged or move slowly. That must be logged as `ChangeTooSmall` or as a target value that did not cross the write threshold.

## Current Gaps

The app needs logs that prove which of these happened while backgrounded:

1. Service is not running.
2. Service is running but sensor is not registered.
3. Sensor is registered but no lux arrives.
4. Lux arrives but no active preset exists.
5. Lux arrives and target is computed, but write is skipped by throttle/deadband.
6. Write is attempted but fails.
7. Write succeeds and read-back confirms it.

## Required Logs

Use a single tag so the chain is visible in `adb logcat`:

```text
AutoBrightness
```

Required fields:

```text
serviceRunning
autoEnabled
sensorRegistered
lux
targetPercent
currentPercent
delta
deadband
throttleMs
shouldApply
reason
writeSuccess
readBackBrightness
```

## Minimum Fix Plan

1. Add `AutoBrightness` lifecycle logs for service create/start/destroy and sensor registration.
2. Add `AutoBrightness` decision logs in `handleLuxSample`.
3. Add runtime fields for:
   - last automatic evaluation time
   - last automatic write time
   - current percent before decision
   - delta percent
   - throttle interval
   - deadband/min delta
   - no-write reason
4. Show those fields in Settings -> Diagnostics and copied diagnostic report.
5. Keep algorithm changes conservative until logs identify whether the issue is lifecycle, sensor delivery, state sync, or decision gating.

## Implemented Minimal Fix

Implemented in the follow-up patch:

- Added the unified `AutoBrightness` log tag across service lifecycle, sensor registration, lux samples, decision checks, and write results.
- The foreground service now reads persisted `autoControlEnabled` on start. If Android restarts the sticky service while auto control is actually disabled, it logs `reason=AutoDisabled` and stops instead of running with stale state.
- The service keeps an internal `autoControlEnabled` value from DataStore and logs it on every lux path.
- Runtime diagnostics now include:
  - last automatic evaluation time
  - last brightness write time
  - current percent used for the decision
  - target system brightness
  - delta percent
  - system delta
  - deadband percent
  - minimum system delta
  - throttle interval
  - elapsed time since last write
  - last no-write reason
- The copied diagnostic report includes the same decision fields.
- `scripts/collect-core-verification.ps1` now captures `AutoBrightness` logcat lines.

No broad refactor was done. The algorithm parameters were left conservative until a real failing-device log proves whether the issue is service lifecycle, sensor delivery, or decision gating.

## Current Verdict

The current code has implemented background auto-adjustment, and the latest patch makes the implementation substantially more provable on a failing device. The most likely categories to confirm with logs are:

1. Foreground service or sensor listener stops while backgrounded on the user's device.
2. Lux is received but the automatic decision is skipped, and the user cannot see why.
3. The default curve or ramp makes the target change smaller than expected.

This is not currently proven to be "manual buttons are the only trigger." The code says automatic lux-triggered writes exist. The next fix must add enough logs and diagnostics to prove the exact failing segment on-device.
