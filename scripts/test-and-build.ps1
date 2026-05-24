$ErrorActionPreference = "Stop"

.\gradlew.bat testDebugUnitTest assembleDebug

Write-Host ""
Write-Host "APK:"
Write-Host (Resolve-Path ".\app\build\outputs\apk\debug\app-debug.apk")
Write-Host ""
Write-Host "Test report:"
Write-Host (Resolve-Path ".\app\build\reports\tests\testDebugUnitTest\index.html")
