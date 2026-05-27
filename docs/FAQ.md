# FAQ

## Does this app require root?

No. It uses Android's `WRITE_SETTINGS` permission, which the user must grant manually.

## Why does the app need "modify system settings"?

Android protects global brightness settings. The app needs this permission to write `Settings.System.SCREEN_BRIGHTNESS` and switch brightness mode to manual while automatic control is active.

## Why does the app run a foreground service?

Brightness control needs to continue while the app is not visible. Android requires a foreground service and notification for this kind of ongoing background work.

## Does the app collect or upload light sensor data?

No. Lux readings are used locally for brightness decisions and diagnostics. See [PRIVACY.md](../PRIVACY.md).

## Why does Android ask about installing unknown apps?

Only the in-app update flow needs this. The app downloads APKs from GitHub Releases and then opens Android's system installer. Installation is never silent.

## Why does brightness sometimes change slowly?

The app intentionally smooths lux readings, throttles writes, and ramps brightness changes to avoid flicker and sudden jumps.

## What happens when I stop automatic control?

The foreground service stops and the app attempts to restore the brightness mode and brightness value captured before control started.

## My device shows no light data. What should I report?

Please include device model, Android version, vendor skin, whether `WRITE_SETTINGS` is granted, whether the app shows a sensor name, and a screenshot of Settings -> Diagnostics.
