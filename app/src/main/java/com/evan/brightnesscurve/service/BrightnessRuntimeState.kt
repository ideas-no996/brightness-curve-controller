package com.evan.brightnesscurve.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RuntimeSnapshot(
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
    val autoEnabled: Boolean = false,
    val windowFallbackActive: Boolean = false,
    val activePresetName: String? = null,
    val message: String? = null,
    val lastError: String? = null,
    val updatedAt: Long? = null
)

object BrightnessRuntimeState {
    private val _state = MutableStateFlow(RuntimeSnapshot())
    val state: StateFlow<RuntimeSnapshot> = _state.asStateFlow()

    fun update(transform: (RuntimeSnapshot) -> RuntimeSnapshot) {
        _state.value = transform(_state.value)
    }

    fun reset(message: String? = null) {
        _state.value = RuntimeSnapshot(message = message, updatedAt = System.currentTimeMillis())
    }
}
