package com.evan.brightnesscurve.ui

import android.os.Build
import android.provider.Settings
import com.evan.brightnesscurve.BuildConfig

internal data class DiagnosticDeviceInfo(
    val manufacturer: String,
    val model: String,
    val androidRelease: String,
    val sdkInt: Int,
    val fingerprint: String
)

internal fun currentDiagnosticDeviceInfo(): DiagnosticDeviceInfo =
    DiagnosticDeviceInfo(
        manufacturer = Build.MANUFACTURER.orEmpty(),
        model = Build.MODEL.orEmpty(),
        androidRelease = Build.VERSION.RELEASE.orEmpty(),
        sdkInt = Build.VERSION.SDK_INT,
        fingerprint = Build.FINGERPRINT.orEmpty()
    )

internal fun buildDiagnosticReport(
    state: MainUiState,
    deviceInfo: DiagnosticDeviceInfo = currentDiagnosticDeviceInfo(),
    generatedAtMillis: Long = System.currentTimeMillis()
): String {
    val runtime = state.runtime
    val settings = state.settings
    val update = state.updateState

    return buildString {
        appendLine("Brightness Curve Controller diagnostic report")
        appendLine("generatedAtMillis=$generatedAtMillis")
        appendLine()
        appendLine("[app]")
        appendLine("versionName=${BuildConfig.VERSION_NAME}")
        appendLine("versionCode=${BuildConfig.VERSION_CODE}")
        appendLine("settingsLoaded=${state.settingsLoaded}")
        appendLine()
        appendLine("[device]")
        appendLine("manufacturer=${deviceInfo.manufacturer.reportValue()}")
        appendLine("model=${deviceInfo.model.reportValue()}")
        appendLine("androidRelease=${deviceInfo.androidRelease.reportValue()}")
        appendLine("sdkInt=${deviceInfo.sdkInt}")
        appendLine("fingerprint=${deviceInfo.fingerprint.reportValue()}")
        appendLine()
        appendLine("[core-loop]")
        appendLine("runtimeStatus=${runtime.status.name}")
        appendLine("canWriteSettings=${state.canWriteSettings}")
        appendLine("runtimeCanWriteSettings=${runtime.canWriteSettings.reportValue()}")
        appendLine("brightnessMode=${formatDiagnosticBrightnessMode(runtime.brightnessMode)}")
        appendLine("currentSystemBrightness=${runtime.currentBrightnessValue.reportValue()}")
        appendLine("currentBrightnessPercent=${formatDiagnosticSystemPercent(runtime.currentBrightnessValue)}")
        appendLine("rawLux=${runtime.rawLux.reportValue()}")
        appendLine("smoothedLux=${runtime.smoothedLux.reportValue()}")
        appendLine("lastLux=${runtime.lastLux.reportValue()}")
        appendLine("lastLuxUpdateTime=${runtime.lastLuxUpdateTime.reportValue()}")
        appendLine("lastAutoEvaluateTime=${runtime.lastAutoEvaluateTime.reportValue()}")
        appendLine("lastBrightnessWriteTime=${runtime.lastBrightnessWriteTime.reportValue()}")
        appendLine("targetPercent=${runtime.targetPercent.reportValue()}")
        appendLine("targetSystemValue=${runtime.targetSystemValue.reportValue()}")
        appendLine("decisionCurrentPercent=${runtime.lastDecisionCurrentPercent.reportValue()}")
        appendLine("decisionDeltaPercent=${runtime.lastDecisionDeltaPercent.reportValue()}")
        appendLine("decisionSystemDelta=${runtime.lastDecisionSystemDelta.reportValue()}")
        appendLine("deadbandPercent=${runtime.lastDecisionDeadbandPercent.reportValue()}")
        appendLine("minSystemDelta=${runtime.lastDecisionMinSystemDelta.reportValue()}")
        appendLine("throttleMs=${runtime.lastDecisionThrottleMillis.reportValue()}")
        appendLine("elapsedSinceLastWriteMs=${runtime.lastDecisionElapsedMillis.reportValue()}")
        appendLine("writtenPercent=${runtime.writtenPercent.reportValue()}")
        appendLine("appliedBrightnessValue=${runtime.appliedBrightnessValue.reportValue()}")
        appendLine("lastWriteTargetValue=${runtime.lastWriteTargetValue.reportValue()}")
        appendLine("lastWriteReadBackValue=${runtime.lastWriteReadBackValue.reportValue()}")
        appendLine("lastWriteSucceeded=${runtime.lastWriteSucceeded.reportValue()}")
        appendLine("lastNoWriteReason=${runtime.lastNoWriteReason.reportValue()}")
        appendLine("failureReason=${runtime.failureReason?.name.reportValue()}")
        appendLine("lastError=${runtime.lastError.reportValue()}")
        appendLine()
        appendLine("[sensor]")
        appendLine("hasLightSensor=${runtime.hasLightSensor.reportValue()}")
        appendLine("lightSensorName=${runtime.lightSensorName.reportValue()}")
        appendLine("lightSensorRegistered=${runtime.lightSensorRegistered}")
        appendLine("lightSensorTimedOut=${runtime.lightSensorTimedOut}")
        appendLine()
        appendLine("[service]")
        appendLine("autoDesired=${settings.autoControlEnabled}")
        appendLine("serviceRunning=${runtime.isRunning}")
        appendLine("pausedForScreenOff=${runtime.isPausedForScreenOff}")
        appendLine("windowFallbackActive=${runtime.windowFallbackActive}")
        appendLine("activePreset=${runtime.activePresetName ?: state.activePreset?.name ?: "-"}")
        appendLine("message=${runtime.message.reportValue()}")
        appendLine("updatedAt=${runtime.updatedAt.reportValue()}")
        appendLine()
        appendLine("[settings]")
        appendLine("startOnBoot=${settings.startOnBoot}")
        appendLine("allowOutdoorFull=${settings.allowOutdoorFull}")
        appendLine("lastComfortPercent=${settings.lastComfortPercent}")
        appendLine("minAllowedPercent=${settings.minAllowedPercent}")
        appendLine("maxAllowedPercent=${settings.maxAllowedPercent}")
        appendLine("responseSpeed=${settings.responseSpeed.name}")
        appendLine("showTutorialOnStartup=${settings.showTutorialOnStartup}")
        appendLine()
        appendLine("[update]")
        appendLine("updateStatus=${update.statusText}")
        appendLine("latestVersion=${update.latest?.versionName.reportValue()}")
        appendLine("downloadedApkPath=${update.downloadedApkPath.reportValue()}")
        appendLine("canInstallPackages=${update.canInstallPackages}")
    }.trimEnd()
}

private fun Any?.reportValue(): String = this?.toString()?.takeIf { it.isNotBlank() } ?: "-"

private fun formatDiagnosticBrightnessMode(value: Int?): String {
    return when (value) {
        Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC -> "automatic($value)"
        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL -> "manual($value)"
        null -> "-"
        else -> "unknown($value)"
    }
}

private fun formatDiagnosticSystemPercent(value: Int?): String {
    val brightness = value ?: return "-"
    return "%.0f%%".format(brightness.coerceIn(0, 255) / 255f * 100f)
}
