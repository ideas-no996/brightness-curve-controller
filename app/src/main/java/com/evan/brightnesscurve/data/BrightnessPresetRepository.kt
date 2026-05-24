package com.evan.brightnesscurve.data

import androidx.room.withTransaction
import com.evan.brightnesscurve.domain.BrightnessCurve
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BrightnessPresetRepository(
    private val database: BrightnessDatabase,
    private val preferencesRepository: AppPreferencesRepository
) {
    private val dao = database.brightnessPresetDao()

    fun observePresets(): Flow<List<BrightnessPreset>> =
        dao.observePresets().map { list -> list.map { it.toPreset() } }

    fun observeActivePreset(): Flow<BrightnessPreset?> =
        dao.observeActivePreset().map { it?.toPreset() }

    fun observeRevisions(presetId: Long): Flow<List<BrightnessRevisionEntity>> =
        dao.observeRevisions(presetId)

    suspend fun ensureDefaults() {
        if (dao.presetCount() > 0) {
            dao.getActivePreset()?.let { preferencesRepository.setActivePresetId(it.id) }
            return
        }

        val now = System.currentTimeMillis()
        database.withTransaction {
            dao.insertPresets(DefaultPresets.builtIns(now))
            val active = dao.getActivePreset()
            if (active != null) preferencesRepository.setActivePresetId(active.id)
        }
    }

    suspend fun activatePreset(id: Long) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            requireNotNull(dao.getPreset(id)) { "预设不存在" }
            dao.clearActivePreset()
            dao.markPresetActive(id, now)
            preferencesRepository.setActivePresetId(id)
        }
    }

    suspend fun copyPreset(id: Long, activateCopy: Boolean = false): Long {
        val now = System.currentTimeMillis()
        return database.withTransaction {
            val source = requireNotNull(dao.getPreset(id)) { "预设不存在" }.toPreset()
            val copyId = dao.insertPreset(
                BrightnessPresetEntity(
                    name = "${source.name} 副本",
                    isBuiltIn = false,
                    isActive = false,
                    pointsJson = PresetCodec.encodePoints(source.points),
                    smoothingLevel = source.smoothingLevel,
                    maxChangePerUpdate = source.maxChangePerUpdate,
                    minUpdateDelta = source.minUpdateDelta,
                    createdAt = now,
                    updatedAt = now
                )
            )
            if (activateCopy) {
                dao.clearActivePreset()
                dao.markPresetActive(copyId, now)
                preferencesRepository.setActivePresetId(copyId)
            }
            copyId
        }
    }

    suspend fun renamePreset(id: Long, name: String) {
        val trimmed = name.trim()
        require(trimmed.isNotBlank()) { "预设名称不能为空" }
        val now = System.currentTimeMillis()
        database.withTransaction {
            val entity = requireNotNull(dao.getPreset(id)) { "预设不存在" }
            require(!entity.isBuiltIn) { "内置预设不能重命名，请先复制" }
            saveRevision(entity, "重命名前自动备份")
            dao.updatePreset(entity.copy(name = trimmed, updatedAt = now))
        }
    }

    suspend fun savePreset(
        id: Long,
        points: List<BrightnessPoint>,
        smoothingLevel: Float,
        maxChangePerUpdate: Float,
        minUpdateDelta: Float,
        note: String = "保存前自动备份"
    ) {
        val sorted = BrightnessCurve.sortedValid(points)
        val now = System.currentTimeMillis()
        database.withTransaction {
            val entity = requireNotNull(dao.getPreset(id)) { "预设不存在" }
            require(!entity.isBuiltIn) { "内置预设不能直接修改，请先复制" }
            saveRevision(entity, note)
            dao.updatePreset(
                entity.copy(
                    pointsJson = PresetCodec.encodePoints(sorted),
                    smoothingLevel = smoothingLevel.coerceIn(0.05f, 0.95f),
                    maxChangePerUpdate = maxChangePerUpdate.coerceIn(1f, 30f),
                    minUpdateDelta = minUpdateDelta.coerceIn(1f, 20f),
                    updatedAt = now
                )
            )
        }
    }

    suspend fun deleteCustomPreset(id: Long) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            val entity = requireNotNull(dao.getPreset(id)) { "预设不存在" }
            require(!entity.isBuiltIn) { "内置预设不能删除" }
            val wasActive = entity.isActive
            dao.deletePreset(entity)
            if (wasActive) {
                dao.clearActivePreset()
                val defaultPreset = dao.getFirstBuiltInPreset()
                if (defaultPreset != null) {
                    dao.markPresetActive(defaultPreset.id, now)
                    preferencesRepository.setActivePresetId(defaultPreset.id)
                }
            }
        }
    }

    suspend fun calibrateActivePreset(lux: Float, brightnessPercent: Float): Long {
        val active = requireNotNull(dao.getActivePreset()) { "没有启用的预设" }.toPreset()
        val editableId = if (active.isBuiltIn) copyPreset(active.id, activateCopy = true) else active.id
        val editable = requireNotNull(dao.getPreset(editableId)) { "预设不存在" }.toPreset()
        val adjusted = adjustNearestPoint(
            points = editable.points,
            lux = lux,
            brightnessPercent = brightnessPercent
        )
        savePreset(
            id = editableId,
            points = adjusted,
            smoothingLevel = editable.smoothingLevel,
            maxChangePerUpdate = editable.maxChangePerUpdate,
            minUpdateDelta = editable.minUpdateDelta,
            note = "快速校准前自动备份"
        )
        preferencesRepository.setLastComfortPercent(brightnessPercent)
        return editableId
    }

    suspend fun restoreRevision(revisionId: Long) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            val revision = requireNotNull(dao.getRevision(revisionId)) { "版本不存在" }
            val preset = requireNotNull(dao.getPreset(revision.presetId)) { "预设不存在" }
            require(!preset.isBuiltIn) { "内置预设不能回滚" }
            saveRevision(preset, "回滚前自动备份")
            val snapshot = revision.snapshot()
            BrightnessCurve.validate(snapshot.points)
            dao.updatePreset(
                preset.copy(
                    name = snapshot.name,
                    pointsJson = PresetCodec.encodePoints(snapshot.points),
                    smoothingLevel = snapshot.smoothingLevel,
                    maxChangePerUpdate = snapshot.maxChangePerUpdate,
                    minUpdateDelta = snapshot.minUpdateDelta,
                    updatedAt = now
                )
            )
        }
    }

    private suspend fun saveRevision(entity: BrightnessPresetEntity, note: String) {
        val preset = entity.toPreset()
        dao.insertRevision(
            BrightnessRevisionEntity(
                presetId = entity.id,
                presetName = entity.name,
                snapshotJson = PresetCodec.encodeSnapshot(preset.snapshot()),
                note = note,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    private fun adjustNearestPoint(
        points: List<BrightnessPoint>,
        lux: Float,
        brightnessPercent: Float
    ): List<BrightnessPoint> {
        val sorted = points.sortedBy { it.lux }
        val nearest = sorted.minBy { kotlin.math.abs(it.lux - lux) }
        return sorted.map {
            if (it == nearest) it.copy(brightnessPercent = brightnessPercent.coerceIn(1f, 100f)) else it
        }
    }
}
