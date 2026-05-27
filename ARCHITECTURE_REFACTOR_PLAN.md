# Architecture Refactor Plan

## Goal

Make the app easier to reason about by separating sensor input, brightness decision logic, brightness writing, permissions, runtime state, and UI. The refactor should keep existing behavior and data compatible while reducing state races.

## Recommended Modules

### `sensor/LightSensorManager`

- Owns Android `SensorManager` registration and unregistration.
- Emits typed events:
  - `SensorAvailable(name)`
  - `SensorUnavailable(reason)`
  - `SensorRegistered`
  - `SensorSample(lux, timestamp, sensorName)`
  - `SensorTimeout`
- Enforces one active listener mode per process:
  - `Stopped`
  - `PassivePreview`
  - `AutoControl`

### `brightness/BrightnessController`

- Remains the only module that calls `Settings.System`.
- Adds typed results:
  - `ReadSuccess`
  - `WriteSuccess(systemValue)`
  - `PermissionMissing`
  - `SystemRejected`
  - `UnknownFailure`
- Owns current-window fallback through a small interface, not directly from UI.

### `engine/BrightnessCurveEngine`

- Pure Kotlin, no Android dependencies.
- Input:
  - raw lux sample
  - active preset points
  - min/max bounds
  - response speed
  - previous engine state
- Output:
  - smoothed lux
  - mapped brightness percent
  - ramped target percent
  - should write
  - reason for not writing
- Replaces duplicated `BrightnessSmoother`, `BrightnessRamp`, and service-side decision branches.

### `runtime/BrightnessRuntimeStore`

- Single source of truth for runtime state.
- Exposes `StateFlow<BrightnessRuntimeSnapshot>`.
- Accepts typed events instead of free-form `copy` calls.
- Keeps last meaningful diagnostic after service stop unless explicitly cleared.

### `permissions/PermissionStateManager`

- Owns checks for:
  - `WRITE_SETTINGS`
  - notification permission
  - install unknown apps permission
- UI asks this module for state and launches system settings through Activity callbacks.

### `ui/*Screen.kt`

- Split current giant screen into:
  - `MainScreen.kt` for scaffold/navigation only
  - `HomeScreen.kt`
  - `PresetScreen.kt`
  - `CurveScreen.kt`
  - `SettingsScreen.kt`
  - `DiagnosticsPanel.kt`
  - `TutorialOverlay.kt`

## Data Flow

```text
LightSensorManager
  -> SensorSample
  -> BrightnessCurveEngine
  -> BrightnessCommand
  -> BrightnessController
  -> BrightnessRuntimeStore
  -> MainViewModel
  -> Compose UI
```

Important rule: UI should not infer core truth from several booleans. It should render a single runtime status plus diagnostics.

## Runtime State Machine

Use explicit statuses:

- `Idle`: app is open, not detecting, auto disabled.
- `DetectingSensor`: sensor registration attempted, waiting for first sample.
- `SensorReady`: lux is available, auto disabled.
- `AutoRunning`: service is active and writing when needed.
- `PermissionMissing`: auto requested but system brightness write is blocked.
- `NoSensor`: device has no light sensor.
- `SensorTimeout`: no sample after timeout.
- `WriteFailed`: brightness write failed after target calculation.
- `PausedScreenOff`: service running but screen is off.

Each status should define:

- user-facing title
- user-facing description
- recommended action
- diagnostic severity
- whether automatic writes are active

## File-Level Refactor Order

1. Add `RuntimeStatus` and make UI render it while preserving existing fields.
2. Add `BrightnessRuntimeStore` and move state mutations behind event functions.
3. Extract `BrightnessCurveEngine` and route service calculations through it.
4. Move passive sensor preview out of `MainViewModel`.
5. Split UI screens without changing behavior.
6. Delete or deprecate duplicated domain wrappers once tests point to the new engine.

## Compatibility Constraints

- Keep application ID `com.evan.brightnesscurve`.
- Do not change Room schema unless a migration is added.
- Preserve DataStore keys.
- Preserve release signing and GitHub Actions.
- Keep current presets compatible.

## Success Criteria

- A reader can trace the chain from sensor sample to brightness write without opening UI files.
- `MainViewModel` no longer owns Android sensor objects.
- `BrightnessControlService` no longer contains raw curve math.
- UI renders one explicit runtime status.
- Unit tests cover engine decisions without Android dependencies.
