package com.evan.brightnesscurve.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BrightnessPresetDao {
    @Query("SELECT COUNT(*) FROM brightness_presets")
    suspend fun presetCount(): Int

    @Query("SELECT * FROM brightness_presets ORDER BY isBuiltIn DESC, updatedAt DESC")
    fun observePresets(): Flow<List<BrightnessPresetEntity>>

    @Query("SELECT * FROM brightness_presets WHERE isActive = 1 LIMIT 1")
    fun observeActivePreset(): Flow<BrightnessPresetEntity?>

    @Query("SELECT * FROM brightness_presets WHERE id = :id LIMIT 1")
    suspend fun getPreset(id: Long): BrightnessPresetEntity?

    @Query("SELECT * FROM brightness_presets WHERE isActive = 1 LIMIT 1")
    suspend fun getActivePreset(): BrightnessPresetEntity?

    @Query("SELECT * FROM brightness_presets WHERE isBuiltIn = 1 ORDER BY createdAt ASC LIMIT 1")
    suspend fun getFirstBuiltInPreset(): BrightnessPresetEntity?

    @Insert
    suspend fun insertPreset(entity: BrightnessPresetEntity): Long

    @Insert
    suspend fun insertPresets(entities: List<BrightnessPresetEntity>)

    @Update
    suspend fun updatePreset(entity: BrightnessPresetEntity)

    @Delete
    suspend fun deletePreset(entity: BrightnessPresetEntity)

    @Query("UPDATE brightness_presets SET isActive = 0")
    suspend fun clearActivePreset()

    @Query("UPDATE brightness_presets SET isActive = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markPresetActive(id: Long, updatedAt: Long)

    @Query("SELECT * FROM brightness_revisions WHERE presetId = :presetId ORDER BY createdAt DESC")
    fun observeRevisions(presetId: Long): Flow<List<BrightnessRevisionEntity>>

    @Query("SELECT * FROM brightness_revisions WHERE id = :id LIMIT 1")
    suspend fun getRevision(id: Long): BrightnessRevisionEntity?

    @Insert
    suspend fun insertRevision(entity: BrightnessRevisionEntity): Long
}
