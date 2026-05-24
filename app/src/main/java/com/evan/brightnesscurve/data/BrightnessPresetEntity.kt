package com.evan.brightnesscurve.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "brightness_presets")
data class BrightnessPresetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val isBuiltIn: Boolean,
    val isActive: Boolean,
    val pointsJson: String,
    val smoothingLevel: Float,
    val maxChangePerUpdate: Float,
    val minUpdateDelta: Float,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toPreset(): BrightnessPreset =
        BrightnessPreset(
            id = id,
            name = name,
            isBuiltIn = isBuiltIn,
            isActive = isActive,
            points = PresetCodec.decodePoints(pointsJson),
            smoothingLevel = smoothingLevel,
            maxChangePerUpdate = maxChangePerUpdate,
            minUpdateDelta = minUpdateDelta,
            createdAt = createdAt,
            updatedAt = updatedAt
        )

    companion object {
        fun fromPreset(
            preset: BrightnessPreset,
            isActive: Boolean = preset.isActive,
            updatedAt: Long = preset.updatedAt
        ): BrightnessPresetEntity =
            BrightnessPresetEntity(
                id = preset.id,
                name = preset.name,
                isBuiltIn = preset.isBuiltIn,
                isActive = isActive,
                pointsJson = PresetCodec.encodePoints(preset.points),
                smoothingLevel = preset.smoothingLevel,
                maxChangePerUpdate = preset.maxChangePerUpdate,
                minUpdateDelta = preset.minUpdateDelta,
                createdAt = preset.createdAt,
                updatedAt = updatedAt
            )
    }
}
