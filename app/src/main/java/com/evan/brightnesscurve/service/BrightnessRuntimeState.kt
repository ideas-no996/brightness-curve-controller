package com.evan.brightnesscurve.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RuntimeSnapshot(
    val isRunning: Boolean = false,
    val isPausedForScreenOff: Boolean = false,
    val rawLux: Float? = null,
    val smoothedLux: Float? = null,
    val targetPercent: Float? = null,
    val writtenPercent: Float? = null,
    val activePresetName: String? = null,
    val message: String? = null,
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
