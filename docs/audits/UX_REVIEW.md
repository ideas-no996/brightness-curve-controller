# UX Review

## Overall UX Judgment

The app is now usable, but still feels like a useful engineering tool that is trying to become a daily utility. The core user question is simple: "Is my screen comfortable right now?" The UI still exposes too much machinery before it fully explains the current situation.

The next UX pass should not add decoration. It should reduce uncertainty. Every screen should answer:

- What is happening now?
- Is the app controlling brightness or only observing?
- If something is blocked, what is the next action?
- Can I safely stop this immediately?

## Home

### What Works

- The home screen surfaces environment category and current brightness feeling.
- Quick actions "too dark / just right / too bright" are understandable.
- The control switch is visible and central.
- Timeout fallback and retry are now present.

### Problems

- The main card mixes environment status, preset name, service state, and switch state.
- The user still has to infer the difference between "read ambient light" and "automatic control is active".
- Permission card appears separately from the switch, so the relationship between permission and control may not be obvious.
- "Current brightness feeling" depends on target/written brightness, not necessarily the user's actual perception.

### Recommended Improvements

- Use a single primary status phrase:
  - Detecting light
  - Light detected, auto control off
  - Auto adjusting
  - Permission required
  - Device has no light sensor
  - Sensor timed out
  - Brightness write failed
- Add a secondary line with the next action:
  - "Tap the switch to start automatic control."
  - "Grant system brightness permission."
  - "Try sensor detection again."
- Add a prominent "Pause" or "Stop auto control" action when auto mode is active.
- Move raw diagnostics out of the home flow unless expanded.

## Presets

### What Works

- Users can see built-in vs custom presets.
- Copy/edit/enable actions are available.
- Active preset is labeled.

### Problems

- "Preset" is not explained in plain language.
- The preview text "N control points / indoor point" is still technical.
- Copying a preset does not explain why a copy is needed before editing.
- Deleting custom presets lacks a confirmation dialog.

### Recommended Improvements

- Rename visible concept to "Brightness style" or explain "Preset = your brightness curve".
- Show a human summary:
  - "Soft indoors"
  - "Balanced"
  - "Outdoor priority"
- Add confirmation for delete.
- Add a one-line description per built-in preset.

## Curve

### What Works

- The page exposes real curve controls.
- Built-in presets are protected from direct editing.
- Revision history exists.

### Problems

- This page is too technical for ordinary users.
- Raw lux input is hard to understand.
- Smoothing, max change, and min update delta are algorithm terms.
- Adding a point by doubling the last lux is not intuitive.
- Invalid input is silently ignored by `mapNotNull`, which can produce confusing saves.

### Recommended Improvements

- Split into "Simple curve" and "Advanced editor".
- Simple mode should show named lighting zones:
  - Dark room
  - Dim indoor
  - Normal indoor
  - Bright indoor
  - Outdoor
- Advanced mode can keep lux and tuning parameters.
- Validate draft points before save and show exact errors.
- Preview the resulting curve visually.

## Settings

### What Works

- Required permissions are visible.
- Brightness min/max and response speed exist.
- Tutorial settings are available.
- Software update flow is discoverable.
- Diagnostics now expose important runtime fields.

### Problems

- Settings mixes user preferences, diagnostics, update flow, and permissions with equal visual weight.
- Diagnostics labels are developer-oriented (`hasLightSensor`, `appliedBrightnessValue`, `brightnessMode`).
- Users may not know whether "allow outdoor full" is safe at night.
- Response speed labels are simple, but do not explain tradeoffs.

### Recommended Improvements

- Group settings into:
  - Permissions
  - Comfort limits
  - Behavior
  - Updates
  - Diagnostics
- Collapse diagnostics by default.
- Use user-facing diagnostic labels first, with raw names only in expanded developer mode.
- Add helper text for response speed:
  - Gentle: fewer changes, slower reaction.
  - Standard: balanced.
  - Fast: reacts sooner, may change more often.

## Tutorial

### What Works

- Short steps exist.
- User can skip.
- Setting can show tutorial again.

### Problems

- Tutorial does not cover the most confusing real-world blockers: permission, no sensor, system brightness mode, and foreground service notification.
- The final choice "show every time" vs "do not auto show" may be too strong; most users want "show tips only when needed".

### Recommended Improvements

- Keep current tutorial short, but add contextual mini-explanations at failure points.
- Add a "Why this permission?" dialog for `WRITE_SETTINGS`.
- Add "What to test first" after onboarding: cover sensor, tap just right, enable control.

## Error Messages

### Problems

- Several messages are descriptive but not actionable.
- Snackbar-only errors can disappear before the user understands them.

### Recommended Improvements

- Persistent errors should live in the relevant card, not only snackbar.
- Every error should include an action:
  - Grant permission
  - Retry sensor
  - Open diagnostics
  - Stop control
  - Re-download update
