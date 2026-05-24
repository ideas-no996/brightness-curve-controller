$ErrorActionPreference = "Stop"

$apk = Resolve-Path ".\app\build\outputs\apk\debug\app-debug.apk"

adb devices
adb install -r $apk
adb shell monkey -p com.evan.brightnesscurve 1

Write-Host ""
Write-Host "Installed and launched: com.evan.brightnesscurve"
