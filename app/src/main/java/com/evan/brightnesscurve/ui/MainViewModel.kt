package com.evan.brightnesscurve.ui

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.evan.brightnesscurve.BrightnessCurveApp
import com.evan.brightnesscurve.data.AppSettings
import com.evan.brightnesscurve.data.BrightnessPoint
import com.evan.brightnesscurve.data.BrightnessPreset
import com.evan.brightnesscurve.data.BrightnessRevisionEntity
import com.evan.brightnesscurve.data.ResponseSpeed
import com.evan.brightnesscurve.service.BrightnessRuntimeState
import com.evan.brightnesscurve.service.RuntimeSnapshot
import com.evan.brightnesscurve.service.ServiceController
import com.evan.brightnesscurve.system.BrightnessSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val presets: List<BrightnessPreset> = emptyList(),
    val activePreset: BrightnessPreset? = null,
    val editorPreset: BrightnessPreset? = null,
    val revisions: List<BrightnessRevisionEntity> = emptyList(),
    val runtime: RuntimeSnapshot = RuntimeSnapshot(),
    val settings: AppSettings = AppSettings(
        activePresetId = null,
        serviceEnabled = false,
        startOnBoot = false,
        allowOutdoorFull = true,
        lastComfortPercent = 20f,
        minAllowedPercent = 3f,
        maxAllowedPercent = 100f,
        responseSpeed = ResponseSpeed.Standard
    ),
    val canWriteSettings: Boolean = false,
    val message: String? = null
)

private data class BaseUiData(
    val presets: List<BrightnessPreset>,
    val activePreset: BrightnessPreset?,
    val editorPreset: BrightnessPreset?,
    val revisions: List<BrightnessRevisionEntity>,
    val runtime: RuntimeSnapshot
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(private val app: BrightnessCurveApp) : AndroidViewModel(app) {
    private val canWriteSettings = MutableStateFlow(BrightnessSettings.canWrite(app))
    private val selectedEditorPresetId = MutableStateFlow<Long?>(null)
    private val message = MutableStateFlow<String?>(null)

    private val editorPreset = combine(
        app.presetRepository.observePresets(),
        app.presetRepository.observeActivePreset(),
        selectedEditorPresetId
    ) { presets, active, selectedId ->
        val id = selectedId ?: active?.id
        presets.firstOrNull { it.id == id } ?: active ?: presets.firstOrNull()
    }

    private val revisions = editorPreset.flatMapLatest { preset ->
        if (preset == null) flowOf(emptyList()) else app.presetRepository.observeRevisions(preset.id)
    }

    private val baseUiData = combine(
        app.presetRepository.observePresets(),
        app.presetRepository.observeActivePreset(),
        editorPreset,
        revisions,
        BrightnessRuntimeState.state
    ) { presets, activePreset, editor, revisions, runtime ->
        BaseUiData(
            presets = presets,
            activePreset = activePreset,
            editorPreset = editor,
            revisions = revisions,
            runtime = runtime
        )
    }

    val uiState = combine(
        baseUiData,
        app.preferencesRepository.settings,
        canWriteSettings,
        message
    ) { base, settings, canWrite, message ->
        MainUiState(
            presets = base.presets,
            activePreset = base.activePreset,
            editorPreset = base.editorPreset,
            revisions = base.revisions,
            runtime = base.runtime,
            settings = settings,
            canWriteSettings = canWrite,
            message = message
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    init {
        viewModelScope.launch {
            runCatching { app.presetRepository.ensureDefaults() }
                .onFailure { message.value = it.message }
        }
    }

    fun refreshWritePermission() {
        canWriteSettings.value = BrightnessSettings.canWrite(app)
    }

    fun setServiceEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                if (!BrightnessSettings.canWrite(app)) {
                    canWriteSettings.value = false
                    message.value = "请先授予修改系统设置权限"
                    return@launch
                }
                app.presetRepository.ensureDefaults()
                ServiceController.start(app)
            } else {
                ServiceController.stop(app)
            }
        }
    }

    fun setStartOnBoot(enabled: Boolean) {
        viewModelScope.launch { app.preferencesRepository.setStartOnBoot(enabled) }
    }

    fun setAllowOutdoorFull(enabled: Boolean) {
        viewModelScope.launch { app.preferencesRepository.setAllowOutdoorFull(enabled) }
    }

    fun setComfortPercent(percent: Float) {
        viewModelScope.launch { app.preferencesRepository.setLastComfortPercent(percent) }
    }

    fun setMinAllowedPercent(percent: Float) {
        viewModelScope.launch { app.preferencesRepository.setMinAllowedPercent(percent) }
    }

    fun setMaxAllowedPercent(percent: Float) {
        viewModelScope.launch { app.preferencesRepository.setMaxAllowedPercent(percent) }
    }

    fun setResponseSpeed(speed: ResponseSpeed) {
        viewModelScope.launch { app.preferencesRepository.setResponseSpeed(speed) }
    }

    fun quickCalibrate(deltaPercent: Float) {
        val runtime = uiState.value.runtime
        val current = runtime.writtenPercent
            ?: runtime.targetPercent
            ?: uiState.value.settings.lastComfortPercent
        val next = (current + deltaPercent).coerceIn(
            uiState.value.settings.minAllowedPercent,
            uiState.value.settings.maxAllowedPercent
        )
        viewModelScope.launch {
            app.preferencesRepository.setLastComfortPercent(next)
            calibrateCurrentEnvironment(next)
        }
    }

    fun calibrateCurrentEnvironment() {
        calibrateCurrentEnvironment(uiState.value.settings.lastComfortPercent)
    }

    private fun calibrateCurrentEnvironment(comfortPercent: Float) {
        val snapshot = uiState.value.runtime
        val lux = snapshot.smoothedLux ?: snapshot.rawLux
        if (lux == null) {
            message.value = "还没有读取到环境光，稍等几秒再校准"
            return
        }

        viewModelScope.launch {
            runCatching {
                val id = app.presetRepository.calibrateActivePreset(
                    lux = lux,
                    brightnessPercent = comfortPercent
                )
                selectedEditorPresetId.value = id
            }.onSuccess {
                message.value = "已按当前环境光生成校准版本"
            }.onFailure {
                message.value = it.message ?: "校准失败"
            }
        }
    }

    fun activatePreset(id: Long) {
        viewModelScope.launch {
            runCatching { app.presetRepository.activatePreset(id) }
                .onSuccess { selectedEditorPresetId.value = id }
                .onFailure { message.value = it.message }
        }
    }

    fun copyPreset(id: Long) {
        viewModelScope.launch {
            runCatching { app.presetRepository.copyPreset(id, activateCopy = false) }
                .onSuccess {
                    selectedEditorPresetId.value = it
                    message.value = "已复制为自定义预设"
                }
                .onFailure { message.value = it.message }
        }
    }

    fun deletePreset(id: Long) {
        viewModelScope.launch {
            runCatching { app.presetRepository.deleteCustomPreset(id) }
                .onSuccess { message.value = "已删除自定义预设" }
                .onFailure { message.value = it.message }
        }
    }

    fun renamePreset(id: Long, name: String) {
        viewModelScope.launch {
            runCatching { app.presetRepository.renamePreset(id, name) }
                .onFailure { message.value = it.message }
        }
    }

    fun selectEditorPreset(id: Long) {
        selectedEditorPresetId.value = id
    }

    fun savePresetDraft(
        id: Long,
        points: List<BrightnessPoint>,
        smoothingLevel: Float,
        maxChangePerUpdate: Float,
        minUpdateDelta: Float
    ) {
        viewModelScope.launch {
            runCatching {
                app.presetRepository.savePreset(
                    id = id,
                    points = points,
                    smoothingLevel = smoothingLevel,
                    maxChangePerUpdate = maxChangePerUpdate,
                    minUpdateDelta = minUpdateDelta
                )
            }.onSuccess {
                message.value = "曲线已保存，并生成回滚版本"
            }.onFailure {
                message.value = it.message ?: "保存失败"
            }
        }
    }

    fun restoreRevision(id: Long) {
        viewModelScope.launch {
            runCatching { app.presetRepository.restoreRevision(id) }
                .onSuccess { message.value = "已恢复到所选版本" }
                .onFailure { message.value = it.message ?: "恢复失败" }
        }
    }

    fun dismissMessage() {
        message.value = null
    }
}

class MainViewModelFactory(private val app: BrightnessCurveApp) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(app) as T
    }
}
