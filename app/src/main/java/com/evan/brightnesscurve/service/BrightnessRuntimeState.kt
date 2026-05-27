package com.evan.brightnesscurve.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class RuntimeSeverity {
    Info,
    Warning,
    Error
}

enum class RuntimeStatus(
    val title: String,
    val description: String,
    val recommendedAction: String?,
    val severity: RuntimeSeverity,
    val automaticWritesActive: Boolean
) {
    Idle(
        title = "未开启自动调节",
        description = "App 已打开，但当前没有自动写入屏幕亮度。",
        recommendedAction = null,
        severity = RuntimeSeverity.Info,
        automaticWritesActive = false
    ),
    DetectingSensor(
        title = "正在读取环境光",
        description = "已连接光线传感器，正在等待第一条环境光数据。",
        recommendedAction = null,
        severity = RuntimeSeverity.Info,
        automaticWritesActive = false
    ),
    SensorReady(
        title = "已读取环境光",
        description = "环境光数据可用，但自动亮度控制未开启。",
        recommendedAction = "开启自动调节",
        severity = RuntimeSeverity.Info,
        automaticWritesActive = false
    ),
    AutoRunning(
        title = "正在自动调节",
        description = "服务正在根据环境光和当前曲线调节屏幕亮度。",
        recommendedAction = null,
        severity = RuntimeSeverity.Info,
        automaticWritesActive = true
    ),
    PermissionMissing(
        title = "缺少亮度写入权限",
        description = "系统暂时不允许 App 修改全局屏幕亮度。",
        recommendedAction = "授予修改系统设置权限",
        severity = RuntimeSeverity.Error,
        automaticWritesActive = false
    ),
    NoSensor(
        title = "没有光线传感器",
        description = "这台设备没有可用的环境光传感器，无法自动感知光线变化。",
        recommendedAction = null,
        severity = RuntimeSeverity.Error,
        automaticWritesActive = false
    ),
    SensorTimeout(
        title = "未收到环境光数据",
        description = "传感器已启动，但一段时间内没有返回环境光读数。",
        recommendedAction = "重试检测",
        severity = RuntimeSeverity.Warning,
        automaticWritesActive = false
    ),
    WriteFailed(
        title = "亮度写入失败",
        description = "已经计算出目标亮度，但写入系统亮度时失败。",
        recommendedAction = "检查权限后重试",
        severity = RuntimeSeverity.Error,
        automaticWritesActive = false
    ),
    PausedScreenOff(
        title = "屏幕关闭，暂停写入",
        description = "自动服务仍在运行，但屏幕关闭时会暂停写入亮度。",
        recommendedAction = null,
        severity = RuntimeSeverity.Info,
        automaticWritesActive = false
    )
}

data class RuntimeSnapshot(
    val status: RuntimeStatus = RuntimeStatus.Idle,
    val isRunning: Boolean = false,
    val isPausedForScreenOff: Boolean = false,
    val hasLightSensor: Boolean? = null,
    val lightSensorName: String? = null,
    val lightSensorRegistered: Boolean = false,
    val lightSensorTimedOut: Boolean = false,
    val rawLux: Float? = null,
    val smoothedLux: Float? = null,
    val lastLux: Float? = null,
    val lastLuxUpdateTime: Long? = null,
    val targetPercent: Float? = null,
    val writtenPercent: Float? = null,
    val appliedBrightnessValue: Int? = null,
    val canWriteSettings: Boolean? = null,
    val brightnessMode: Int? = null,
    val autoControlDesired: Boolean = false,
    val windowFallbackActive: Boolean = false,
    val activePresetName: String? = null,
    val message: String? = null,
    val lastError: String? = null,
    val updatedAt: Long? = null
)

sealed interface RuntimeEvent {
    data class PermissionRefreshed(
        val canWriteSettings: Boolean,
        val brightnessMode: Int?
    ) : RuntimeEvent

    data class WindowBrightnessFallbackEnabled(
        val brightnessMode: Int?
    ) : RuntimeEvent

    data class PassiveSensorUnavailable(
        val reason: String,
        val canWriteSettings: Boolean,
        val brightnessMode: Int?
    ) : RuntimeEvent

    data class PassiveSensorStatusChanged(
        val hasLightSensor: Boolean,
        val sensorName: String?,
        val isRegistered: Boolean,
        val canWriteSettings: Boolean,
        val brightnessMode: Int?
    ) : RuntimeEvent

    data class PassiveLuxObserved(
        val rawLux: Float,
        val smoothedLux: Float,
        val receivedAtMillis: Long,
        val sensorName: String?,
        val targetPercent: Float?,
        val activePresetName: String?,
        val canWriteSettings: Boolean,
        val brightnessMode: Int?
    ) : RuntimeEvent

    data object SensorTimedOut : RuntimeEvent

    data object ScreenTurnedOff : RuntimeEvent

    data class ScreenTurnedOn(
        val brightnessMode: Int?,
        val appliedBrightnessValue: Int?
    ) : RuntimeEvent

    data class ServiceSensorUnavailable(
        val reason: String,
        val canWriteSettings: Boolean,
        val brightnessMode: Int?
    ) : RuntimeEvent

    data class ServicePermissionMissing(
        val brightnessMode: Int?
    ) : RuntimeEvent

    data class ServiceStopped(
        val message: String?
    ) : RuntimeEvent

    data class ServiceSensorStatusChanged(
        val hasLightSensor: Boolean,
        val sensorName: String?,
        val isRegistered: Boolean,
        val canWriteSettings: Boolean,
        val brightnessMode: Int?
    ) : RuntimeEvent

    data class ServiceLuxObserved(
        val rawLux: Float,
        val smoothedLux: Float,
        val receivedAtMillis: Long,
        val sensorName: String?,
        val activePresetName: String?,
        val targetPercent: Float?,
        val preserveExistingTargetPercent: Boolean,
        val canWriteSettings: Boolean,
        val brightnessMode: Int?,
        val message: String,
        val lastError: String?,
        val isPausedForScreenOff: Boolean
    ) : RuntimeEvent

    data class ServiceWritePermissionLost(
        val brightnessMode: Int?
    ) : RuntimeEvent

    data class ServiceBrightnessWritten(
        val writtenPercent: Float,
        val appliedBrightnessValue: Int?,
        val brightnessMode: Int?
    ) : RuntimeEvent

    data class ActivePresetLoaded(
        val presetName: String,
        val appliedBrightnessValue: Int?,
        val brightnessMode: Int?,
        val canWriteSettings: Boolean
    ) : RuntimeEvent

    data object NoActivePresetConfigured : RuntimeEvent

    data class AutoEnabledChanged(
        val desiredAutoControlEnabled: Boolean
    ) : RuntimeEvent

    data class ServiceStartAnnounced(
        val message: String
    ) : RuntimeEvent

    data class ServiceStartCompleted(
        val started: Boolean,
        val canWriteSettings: Boolean,
        val brightnessMode: Int?,
        val appliedBrightnessValue: Int?
    ) : RuntimeEvent
}

fun RuntimeSnapshot.resolvedRuntimeStatus(): RuntimeStatus {
    return when {
        canWriteSettings == false && (autoControlDesired || isRunning) -> RuntimeStatus.PermissionMissing
        hasLightSensor == false -> RuntimeStatus.NoSensor
        lightSensorTimedOut -> RuntimeStatus.SensorTimeout
        isWriteFailure() -> RuntimeStatus.WriteFailed
        isPausedForScreenOff && isRunning -> RuntimeStatus.PausedScreenOff
        isRunning -> RuntimeStatus.AutoRunning
        lastLux != null -> RuntimeStatus.SensorReady
        lightSensorRegistered -> RuntimeStatus.DetectingSensor
        else -> RuntimeStatus.Idle
    }
}

internal fun reduceRuntimeSnapshot(current: RuntimeSnapshot, event: RuntimeEvent): RuntimeSnapshot {
    val next = when (event) {
        is RuntimeEvent.PermissionRefreshed -> current.copy(
            canWriteSettings = event.canWriteSettings,
            brightnessMode = event.brightnessMode,
            windowFallbackActive = if (event.canWriteSettings) false else current.windowFallbackActive
        )

        is RuntimeEvent.WindowBrightnessFallbackEnabled -> current.copy(
            windowFallbackActive = true,
            canWriteSettings = false,
            brightnessMode = event.brightnessMode,
            message = "缺少系统亮度权限，先用当前窗口亮度预览",
            lastError = "缺少修改系统设置权限"
        )

        is RuntimeEvent.PassiveSensorUnavailable -> current.copy(
            hasLightSensor = false,
            lightSensorRegistered = false,
            lightSensorTimedOut = false,
            canWriteSettings = event.canWriteSettings,
            brightnessMode = event.brightnessMode,
            message = event.reason,
            lastError = event.reason
        )

        is RuntimeEvent.PassiveSensorStatusChanged -> {
            if (current.isRunning) {
                current.copy(
                    hasLightSensor = event.hasLightSensor,
                    lightSensorName = event.sensorName,
                    lightSensorRegistered = event.isRegistered
                )
            } else {
                current.copy(
                    hasLightSensor = event.hasLightSensor,
                    lightSensorName = event.sensorName,
                    lightSensorRegistered = event.isRegistered,
                    canWriteSettings = event.canWriteSettings,
                    brightnessMode = event.brightnessMode,
                    message = if (event.isRegistered) "正在读取环境光" else current.message
                )
            }
        }

        is RuntimeEvent.PassiveLuxObserved -> {
            if (current.isRunning) {
                return current
            }
            current.copy(
                isRunning = false,
                rawLux = event.rawLux,
                smoothedLux = event.smoothedLux,
                lastLux = event.rawLux,
                lastLuxUpdateTime = event.receivedAtMillis,
                targetPercent = event.targetPercent,
                hasLightSensor = true,
                lightSensorName = event.sensorName,
                lightSensorRegistered = true,
                lightSensorTimedOut = false,
                activePresetName = event.activePresetName,
                canWriteSettings = event.canWriteSettings,
                brightnessMode = event.brightnessMode,
                message = if (event.targetPercent == null) {
                    "已读取环境光，但还没有可用亮度曲线"
                } else {
                    "已读取环境光，但未自动调节"
                },
                lastError = null
            )
        }

        RuntimeEvent.SensorTimedOut -> current.copy(
            lightSensorTimedOut = true,
            message = "未收到环境光数据",
            lastError = "5 秒内未收到环境光数据"
        )

        RuntimeEvent.ScreenTurnedOff -> current.copy(
            isPausedForScreenOff = true,
            message = "屏幕关闭，暂停写入"
        )

        is RuntimeEvent.ScreenTurnedOn -> current.copy(
            isPausedForScreenOff = false,
            brightnessMode = event.brightnessMode,
            appliedBrightnessValue = event.appliedBrightnessValue,
            message = "屏幕点亮，恢复控制"
        )

        is RuntimeEvent.ServiceSensorUnavailable -> current.copy(
            isRunning = false,
            hasLightSensor = false,
            lightSensorRegistered = false,
            lightSensorTimedOut = false,
            canWriteSettings = event.canWriteSettings,
            brightnessMode = event.brightnessMode,
            message = event.reason,
            lastError = event.reason
        )

        is RuntimeEvent.ServicePermissionMissing -> current.copy(
            isRunning = false,
            canWriteSettings = false,
            brightnessMode = event.brightnessMode,
            message = "缺少修改系统设置权限",
            lastError = "缺少修改系统设置权限"
        )

        is RuntimeEvent.ServiceStopped -> current.copy(
            isRunning = false,
            isPausedForScreenOff = false,
            lightSensorRegistered = false,
            windowFallbackActive = false,
            message = event.message
        )

        is RuntimeEvent.ServiceSensorStatusChanged -> current.copy(
            hasLightSensor = event.hasLightSensor,
            lightSensorName = event.sensorName,
            lightSensorRegistered = event.isRegistered,
            canWriteSettings = event.canWriteSettings,
            brightnessMode = event.brightnessMode
        )

        is RuntimeEvent.ServiceLuxObserved -> current.copy(
            isRunning = true,
            isPausedForScreenOff = event.isPausedForScreenOff,
            rawLux = event.rawLux,
            smoothedLux = event.smoothedLux,
            lastLux = event.rawLux,
            lastLuxUpdateTime = event.receivedAtMillis,
            targetPercent = if (event.preserveExistingTargetPercent) {
                current.targetPercent
            } else {
                event.targetPercent
            },
            activePresetName = event.activePresetName,
            hasLightSensor = true,
            lightSensorName = event.sensorName,
            lightSensorRegistered = true,
            lightSensorTimedOut = false,
            canWriteSettings = event.canWriteSettings,
            brightnessMode = event.brightnessMode,
            message = event.message,
            lastError = event.lastError
        )

        is RuntimeEvent.ServiceWritePermissionLost -> current.copy(
            canWriteSettings = false,
            brightnessMode = event.brightnessMode,
            message = "写入前权限失效，已暂停亮度写入",
            lastError = "写入前权限失效"
        )

        is RuntimeEvent.ServiceBrightnessWritten -> current.copy(
            writtenPercent = event.writtenPercent,
            appliedBrightnessValue = event.appliedBrightnessValue,
            canWriteSettings = true,
            brightnessMode = event.brightnessMode,
            lastError = null
        )

        is RuntimeEvent.ActivePresetLoaded -> current.copy(
            activePresetName = event.presetName,
            appliedBrightnessValue = event.appliedBrightnessValue,
            brightnessMode = event.brightnessMode,
            canWriteSettings = event.canWriteSettings,
            message = "已加载预设：${event.presetName}"
        )

        RuntimeEvent.NoActivePresetConfigured -> current.copy(
            activePresetName = null,
            message = "没有启用的亮度曲线",
            lastError = "没有启用的亮度曲线"
        )

        is RuntimeEvent.AutoEnabledChanged -> current.copy(
            autoControlDesired = event.desiredAutoControlEnabled
        )

        is RuntimeEvent.ServiceStartAnnounced -> current.copy(
            message = event.message
        )

        is RuntimeEvent.ServiceStartCompleted -> current.copy(
            isRunning = event.started,
            lightSensorTimedOut = false,
            canWriteSettings = event.canWriteSettings,
            brightnessMode = event.brightnessMode,
            appliedBrightnessValue = event.appliedBrightnessValue,
            message = if (event.started) {
                "亮度控制已启动，正在读取环境光"
            } else {
                "光线传感器启动失败"
            },
            lastError = if (event.started) null else "光线传感器启动失败"
        )
    }
    return next.copy(updatedAt = System.currentTimeMillis()).withResolvedStatus()
}

object BrightnessRuntimeState {
    private val _state = MutableStateFlow(RuntimeSnapshot())
    val state: StateFlow<RuntimeSnapshot> = _state.asStateFlow()

    fun dispatch(event: RuntimeEvent) {
        _state.value = reduceRuntimeSnapshot(_state.value, event)
    }
}

private fun RuntimeSnapshot.withResolvedStatus(): RuntimeSnapshot =
    copy(status = resolvedRuntimeStatus())

private fun RuntimeSnapshot.isWriteFailure(): Boolean {
    if (lastError.isNullOrBlank()) return false
    val text = "$lastError ${message.orEmpty()}"
    return listOf("写入", "亮度曲线计算失败", "亮度曲线结果无效").any { token ->
        text.contains(token)
    }
}
