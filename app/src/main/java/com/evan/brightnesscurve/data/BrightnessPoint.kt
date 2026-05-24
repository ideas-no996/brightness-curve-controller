package com.evan.brightnesscurve.data

import kotlinx.serialization.Serializable

@Serializable
data class BrightnessPoint(
    val lux: Float,
    val brightnessPercent: Float
)

@Serializable
data class BrightnessPresetSnapshot(
    val name: String,
    val points: List<BrightnessPoint>,
    val smoothingLevel: Float,
    val maxChangePerUpdate: Float,
    val minUpdateDelta: Float
)

data class BrightnessPreset(
    val id: Long,
    val name: String,
    val isBuiltIn: Boolean,
    val isActive: Boolean,
    val points: List<BrightnessPoint>,
    val smoothingLevel: Float,
    val maxChangePerUpdate: Float,
    val minUpdateDelta: Float,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun snapshot(nameOverride: String = name): BrightnessPresetSnapshot =
        BrightnessPresetSnapshot(
            name = nameOverride,
            points = points,
            smoothingLevel = smoothingLevel,
            maxChangePerUpdate = maxChangePerUpdate,
            minUpdateDelta = minUpdateDelta
        )
}
