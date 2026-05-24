package com.evan.brightnesscurve.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [BrightnessPresetEntity::class, BrightnessRevisionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class BrightnessDatabase : RoomDatabase() {
    abstract fun brightnessPresetDao(): BrightnessPresetDao
}
