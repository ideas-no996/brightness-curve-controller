package com.evan.brightnesscurve.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "brightness_revisions",
    foreignKeys = [
        ForeignKey(
            entity = BrightnessPresetEntity::class,
            parentColumns = ["id"],
            childColumns = ["presetId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("presetId")]
)
data class BrightnessRevisionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val presetId: Long,
    val presetName: String,
    val snapshotJson: String,
    val note: String,
    val createdAt: Long
) {
    fun snapshot(): BrightnessPresetSnapshot = PresetCodec.decodeSnapshot(snapshotJson)
}
