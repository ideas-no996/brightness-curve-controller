# Compatibility And Test Matrix

Brightness Curve Controller targets Android 8.0+ (`minSdk 26`). Real behavior depends on the device's light sensor, vendor background restrictions, and system brightness implementation.

## Known Tested Devices

| Device / OS | Android | Sensor read | Brightness write | Foreground service | Notes |
| --- | --- | --- | --- | --- | --- |
| OPPO / ColorOS tablet | Unknown | Pass | Pass | Pass | Original development and manual testing device. |

## Manual Test Checklist

Run these checks before reporting a device as supported:

1. Fresh install starts without crash.
2. App can request and detect `WRITE_SETTINGS`.
3. Home screen shows ambient-light status within 5 seconds.
4. Diagnostics show sensor name, last lux, brightness mode, and target brightness.
5. Automatic control starts a foreground service.
6. Brightness changes gradually when lux changes.
7. Screen-off pause does not crash the service.
8. Stopping control restores the previous brightness mode and value when permission is still available.
9. App restart preserves presets and auto-control preference.
10. Optional: start-on-boot resumes control after reboot.

## Issue Report Data

For compatibility issues, include:

- device model
- Android version
- vendor skin, such as ColorOS, One UI, MIUI, HyperOS, Pixel Android, or LineageOS
- app version
- whether `WRITE_SETTINGS` is granted
- whether Android battery optimization is restricting the app
- screenshot of Settings -> Diagnostics
- relevant `adb logcat` lines if available

## Current Gaps

- The app has not yet been broadly verified across Samsung, Xiaomi, Pixel, Vivo, Honor, or custom ROM devices.
- Some vendors may throttle sensors or foreground services aggressively.
- Some devices may expose a light sensor but return no data while the screen is off or in low-power modes.
