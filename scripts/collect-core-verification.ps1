param(
    [string]$PackageName = "com.evan.brightnesscurve",
    [string]$OutDir = "",
    [int]$LogLines = 1500,
    [switch]$Interactive
)

$ErrorActionPreference = "Stop"

$adb = Get-Command adb -ErrorAction SilentlyContinue
if ($null -eq $adb) {
    throw "adb was not found on PATH. Install Android platform-tools or add adb to PATH."
}

if ([string]::IsNullOrWhiteSpace($OutDir)) {
    $stamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $OutDir = Join-Path (Resolve-Path ".").Path "diagnostics\core-verification-$stamp"
}

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

function Write-TextFile {
    param(
        [string]$Name,
        [string[]]$Lines
    )
    $path = Join-Path $OutDir $Name
    $Lines | Out-File -FilePath $path -Encoding utf8
}

function Invoke-AdbText {
    param([string[]]$Arguments)
    (& adb @Arguments 2>&1) | ForEach-Object { $_.ToString() }
}

function Capture-Adb {
    param(
        [string]$Name,
        [string[]]$Arguments
    )
    Write-TextFile -Name $Name -Lines (Invoke-AdbText -Arguments $Arguments)
}

function Read-SystemSetting {
    param([string]$Key)
    $lines = Invoke-AdbText -Arguments @("shell", "settings", "get", "system", $Key)
    ($lines -join "`n").Trim()
}

$startedAt = Get-Date -Format "yyyy-MM-dd HH:mm:ss zzz"
$beforeMode = Read-SystemSetting -Key "screen_brightness_mode"
$beforeBrightness = Read-SystemSetting -Key "screen_brightness"

Write-TextFile -Name "summary.txt" -Lines @(
    "Brightness Curve Controller core verification snapshot",
    "Started at: $startedAt",
    "Package: $PackageName",
    "Output directory: $OutDir",
    "",
    "Before interaction:",
    "screen_brightness_mode=$beforeMode",
    "screen_brightness=$beforeBrightness",
    "",
    "If you are debugging the core loop, pair this bundle with the app's Settings -> Diagnostics screen.",
    "Pass requires lux -> target -> system write -> read-back -> UI status."
)

Capture-Adb -Name "adb-devices.txt" -Arguments @("devices", "-l")
$devicePropertyKeys = @(
    "ro.product.manufacturer",
    "ro.product.model",
    "ro.build.version.release",
    "ro.build.version.sdk",
    "ro.build.fingerprint"
)
$deviceProperties = foreach ($key in $devicePropertyKeys) {
    $value = (Invoke-AdbText -Arguments @("shell", "getprop", $key) -join "`n").Trim()
    "$key=$value"
}
Write-TextFile -Name "device-properties.txt" -Lines $deviceProperties
Capture-Adb -Name "package.txt" -Arguments @("shell", "dumpsys", "package", $PackageName)
Capture-Adb -Name "brightness-before.txt" -Arguments @("shell", "settings", "list", "system")
Capture-Adb -Name "sensorservice.txt" -Arguments @("shell", "dumpsys", "sensorservice")
Capture-Adb -Name "activity-services.txt" -Arguments @("shell", "dumpsys", "activity", "services", $PackageName)

$logPattern = "BrightnessControlService|BrightnessController|LightSensorMonitor|MainViewModel|AndroidRuntime|FATAL EXCEPTION|$PackageName"
$logLines = Invoke-AdbText -Arguments @("logcat", "-d", "-t", $LogLines.ToString())
$filteredLog = $logLines | Select-String -Pattern $logPattern | ForEach-Object { $_.ToString() }
Write-TextFile -Name "logcat-filtered.txt" -Lines $filteredLog

if ($Interactive) {
    Write-Host ""
    Write-Host "Trigger a manual brightness change or enable automatic control now."
    Write-Host "Then press Enter to capture the after snapshot."
    Read-Host | Out-Null

    $afterMode = Read-SystemSetting -Key "screen_brightness_mode"
    $afterBrightness = Read-SystemSetting -Key "screen_brightness"
    Write-TextFile -Name "brightness-after.txt" -Lines @(
        "screen_brightness_mode=$afterMode",
        "screen_brightness=$afterBrightness"
    )
    Capture-Adb -Name "activity-services-after.txt" -Arguments @("shell", "dumpsys", "activity", "services", $PackageName)

    $afterLogLines = Invoke-AdbText -Arguments @("logcat", "-d", "-t", $LogLines.ToString())
    $afterFilteredLog = $afterLogLines | Select-String -Pattern $logPattern | ForEach-Object { $_.ToString() }
    Write-TextFile -Name "logcat-filtered-after.txt" -Lines $afterFilteredLog
}

Write-Host ""
Write-Host "Core verification bundle written to:"
Write-Host $OutDir
