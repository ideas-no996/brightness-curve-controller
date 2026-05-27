package com.evan.brightnesscurve.ui

import android.content.Context
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.evan.brightnesscurve.BrightnessCurveApp
import com.evan.brightnesscurve.BuildConfig
import com.evan.brightnesscurve.brightness.BrightnessController
import com.evan.brightnesscurve.brightness.BrightnessMapping
import com.evan.brightnesscurve.data.AppSettings
import com.evan.brightnesscurve.data.BrightnessPoint
import com.evan.brightnesscurve.data.BrightnessPreset
import com.evan.brightnesscurve.data.BrightnessRevisionEntity
import com.evan.brightnesscurve.data.ResponseSpeed
import com.evan.brightnesscurve.service.BrightnessRuntimeState
import com.evan.brightnesscurve.service.RuntimeSnapshot
import com.evan.brightnesscurve.service.ServiceController
import com.evan.brightnesscurve.system.BrightnessSettings
import com.evan.brightnesscurve.sensor.LightSensorSample
import com.evan.brightnesscurve.sensor.LightSensorMonitor
import com.evan.brightnesscurve.sensor.LightSensorStatus
import com.evan.brightnesscurve.sensor.LuxSmoother
import com.evan.brightnesscurve.update.AppUpdateState
import com.evan.brightnesscurve.update.UpdateChecker
import com.evan.brightnesscurve.update.UpdateDownloader
import com.evan.brightnesscurve.update.UpdateInstaller
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

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
        responseSpeed = ResponseSpeed.Standard,
        hasSeenTutorial = false,
        showTutorialOnStartup = true
    ),
    val canWriteSettings: Boolean = false,
    val updateState: AppUpdateState = AppUpdateState(
        currentVersionName = BuildConfig.VERSION_NAME,
        canInstallPackages = true
    ),
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
    private val brightnessController = BrightnessController(app)
    private val updateChecker = UpdateChecker()
    private val updateDownloader = UpdateDownloader(app)
    private val updateState = MutableStateFlow(
        AppUpdateState(
            currentVersionName = BuildConfig.VERSION_NAME,
            canInstallPackages = UpdateInstaller.canRequestPackageInstalls(app)
        )
    )
    private val passiveLuxSmoother = LuxSmoother(ResponseSpeed.Standard.alpha)
    private var passiveLightSensorMonitor: LightSensorMonitor? = null
    private var passiveLuxTimeoutJob: Job? = null
    private var passiveSensorStartedAtMillis: Long = 0L

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
        updateState,
        message
    ) { base, settings, canWrite, updateState, message ->
        MainUiState(
            presets = base.presets,
            activePreset = base.activePreset,
            editorPreset = base.editorPreset,
            revisions = base.revisions,
            runtime = base.runtime,
            settings = settings,
            canWriteSettings = canWrite,
            updateState = updateState,
            message = message
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    init {
        viewModelScope.launch {
            runCatching { app.presetRepository.ensureDefaults() }
                .onFailure { message.value = it.message }
        }
        startPassiveLightSensor()
    }

    fun refreshWritePermission() {
        val canWrite = BrightnessSettings.canWrite(app)
        canWriteSettings.value = canWrite
        BrightnessRuntimeState.update {
            it.copy(
                canWriteSettings = canWrite,
                brightnessMode = readModeOrNull(),
                windowFallbackActive = if (canWrite) false else it.windowFallbackActive,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    fun retryLightSensorDetection() {
        startPassiveLightSensor()
        val state = uiState.value
        if ((state.settings.serviceEnabled || state.runtime.isRunning) && BrightnessSettings.canWrite(app)) {
            ServiceController.start(app)
        }
    }

    fun enableWindowBrightnessFallback() {
        BrightnessRuntimeState.update {
            it.copy(
                windowFallbackActive = true,
                canWriteSettings = false,
                brightnessMode = readModeOrNull(),
                message = "缺少系统亮度权限，先用当前窗口亮度预览",
                lastError = "缺少修改系统设置权限",
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    fun refreshInstallPermission() {
        updateState.update {
            it.copy(canInstallPackages = UpdateInstaller.canRequestPackageInstalls(app))
        }
    }

    fun checkForUpdate() {
        if (updateState.value.isChecking) return

        viewModelScope.launch {
            updateState.update {
                it.copy(
                    isChecking = true,
                    statusText = "正在检查更新",
                    downloadedApkPath = null,
                    downloadProgressPercent = null
                )
            }

            runCatching {
                updateChecker.checkLatest(BuildConfig.VERSION_NAME)
            }.onSuccess { latest ->
                updateState.update {
                    if (latest == null) {
                        it.copy(
                            latest = null,
                            isChecking = false,
                            lastCheckedAt = System.currentTimeMillis(),
                            statusText = "已是最新版本"
                        )
                    } else {
                        it.copy(
                            latest = latest,
                            isChecking = false,
                            lastCheckedAt = System.currentTimeMillis(),
                            statusText = "发现新版本 ${latest.versionName}"
                        )
                    }
                }
            }.onFailure { throwable ->
                val text = throwable.message ?: "暂时无法检查更新"
                updateState.update {
                    it.copy(
                        isChecking = false,
                        lastCheckedAt = System.currentTimeMillis(),
                        statusText = text
                    )
                }
                message.value = text
            }
        }
    }

    fun downloadLatestUpdate() {
        if (updateState.value.isDownloading) return

        val latest = updateState.value.latest
        if (latest == null) {
            message.value = "请先检查更新"
            return
        }

        viewModelScope.launch {
            updateState.update {
                it.copy(
                    isDownloading = true,
                    downloadProgressPercent = 0,
                    downloadedApkPath = null,
                    statusText = "正在下载更新"
                )
            }

            runCatching {
                updateDownloader.download(latest) { progress ->
                    updateState.update { current ->
                        current.copy(downloadProgressPercent = progress)
                    }
                }
            }.onSuccess { apkFile ->
                updateState.update {
                    it.copy(
                        isDownloading = false,
                        downloadProgressPercent = 100,
                        downloadedApkPath = apkFile.absolutePath,
                        statusText = "下载完成，可以安装"
                    )
                }
            }.onFailure { throwable ->
                val text = throwable.message ?: "下载更新失败"
                updateState.update {
                    it.copy(
                        isDownloading = false,
                        statusText = text
                    )
                }
                message.value = text
            }
        }
    }

    fun installDownloadedUpdate(): Boolean {
        val canInstall = UpdateInstaller.canRequestPackageInstalls(app)
        updateState.update { it.copy(canInstallPackages = canInstall) }
        if (!canInstall) {
            message.value = "请先允许安装此来源的应用"
            return false
        }

        val path = updateState.value.downloadedApkPath
        if (path.isNullOrBlank()) {
            message.value = "请先下载更新"
            return false
        }

        val apkFile = File(path)
        if (!apkFile.exists()) {
            message.value = "安装包已失效，请重新下载"
            updateState.update { it.copy(downloadedApkPath = null, statusText = "安装包已失效") }
            return false
        }

        return runCatching {
            UpdateInstaller.installApk(app, apkFile)
        }.onFailure {
            message.value = it.message ?: "无法打开系统安装器"
        }.isSuccess
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

    fun setShowTutorialOnStartup(enabled: Boolean) {
        viewModelScope.launch { app.preferencesRepository.setShowTutorialOnStartup(enabled) }
    }

    fun finishTutorial(showOnStartup: Boolean) {
        viewModelScope.launch { app.preferencesRepository.finishTutorial(showOnStartup) }
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

    override fun onCleared() {
        passiveLuxTimeoutJob?.cancel()
        passiveLightSensorMonitor?.stop()
        super.onCleared()
    }

    private fun startPassiveLightSensor() {
        passiveLuxTimeoutJob?.cancel()
        passiveLightSensorMonitor?.stop()
        passiveLuxSmoother.reset()
        passiveSensorStartedAtMillis = System.currentTimeMillis()

        val sensorManager = app.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        passiveLightSensorMonitor = LightSensorMonitor(
            sensorManager = sensorManager,
            onSample = ::handlePassiveLuxSample,
            onStatus = ::handlePassiveLightSensorStatus,
            onUnavailable = { reason ->
                BrightnessRuntimeState.update {
                    it.copy(
                        hasLightSensor = false,
                        lightSensorRegistered = false,
                        lightSensorTimedOut = false,
                        canWriteSettings = BrightnessSettings.canWrite(app),
                        brightnessMode = readModeOrNull(),
                        message = reason,
                        lastError = reason,
                        updatedAt = System.currentTimeMillis()
                    )
                }
            }
        )

        if (passiveLightSensorMonitor?.start() == true) {
            schedulePassiveLuxTimeout()
        }
    }

    private fun handlePassiveLightSensorStatus(status: LightSensorStatus) {
        BrightnessRuntimeState.update {
            if (it.isRunning) {
                it.copy(
                    hasLightSensor = status.hasLightSensor,
                    lightSensorName = status.sensorName,
                    lightSensorRegistered = status.isRegistered,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                it.copy(
                    hasLightSensor = status.hasLightSensor,
                    lightSensorName = status.sensorName,
                    lightSensorRegistered = status.isRegistered,
                    canWriteSettings = BrightnessSettings.canWrite(app),
                    brightnessMode = readModeOrNull(),
                    message = if (status.isRegistered) "正在读取环境光" else it.message,
                    updatedAt = System.currentTimeMillis()
                )
            }
        }
    }

    private fun handlePassiveLuxSample(sample: LightSensorSample) {
        val runtime = BrightnessRuntimeState.state.value
        if (runtime.isRunning) return

        val smoothedLux = passiveLuxSmoother.onSample(sample.lux)
        val state = uiState.value
        val preset = state.activePreset
        val target = preset?.let {
            runCatching {
                val maxAllowed = if (state.settings.allowOutdoorFull) {
                    state.settings.maxAllowedPercent
                } else {
                    state.settings.maxAllowedPercent.coerceAtMost(85f)
                }
                BrightnessMapping.targetPercent(
                    lux = smoothedLux,
                    points = it.points,
                    minPercent = state.settings.minAllowedPercent,
                    maxPercent = maxAllowed
                )
            }.getOrNull()
        }?.takeUnless { it.isNaN() || it.isInfinite() }

        BrightnessRuntimeState.update {
            if (it.isRunning) {
                it
            } else {
                it.copy(
                    isRunning = false,
                    autoEnabled = false,
                    rawLux = sample.lux,
                    smoothedLux = smoothedLux,
                    lastLux = sample.lux,
                    lastLuxUpdateTime = sample.receivedAtMillis,
                    targetPercent = target,
                    hasLightSensor = true,
                    lightSensorName = sample.sensorName,
                    lightSensorRegistered = true,
                    lightSensorTimedOut = false,
                    activePresetName = preset?.name,
                    canWriteSettings = BrightnessSettings.canWrite(app),
                    brightnessMode = readModeOrNull(),
                    message = if (target == null) {
                        "已读取环境光，但还没有可用亮度曲线"
                    } else {
                        "已读取环境光，但未自动调节"
                    },
                    lastError = null,
                    updatedAt = System.currentTimeMillis()
                )
            }
        }
    }

    private fun schedulePassiveLuxTimeout() {
        passiveLuxTimeoutJob?.cancel()
        passiveLuxTimeoutJob = viewModelScope.launch {
            val startedAt = passiveSensorStartedAtMillis
            delay(LUX_TIMEOUT_MS)
            val runtime = BrightnessRuntimeState.state.value
            if (!runtime.isRunning && (runtime.lastLuxUpdateTime == null || runtime.lastLuxUpdateTime < startedAt)) {
                BrightnessRuntimeState.update {
                    it.copy(
                        lightSensorTimedOut = true,
                        message = "未收到环境光数据",
                        lastError = "5 秒内未收到环境光数据",
                        updatedAt = System.currentTimeMillis()
                    )
                }
            }
        }
    }

    private fun readModeOrNull(): Int? =
        runCatching { brightnessController.readMode() }.getOrNull()

    companion object {
        private const val LUX_TIMEOUT_MS = 5_000L
    }
}

class MainViewModelFactory(private val app: BrightnessCurveApp) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(app) as T
    }
}
