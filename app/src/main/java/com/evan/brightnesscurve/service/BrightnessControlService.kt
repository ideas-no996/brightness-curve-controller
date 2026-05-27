package com.evan.brightnesscurve.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.evan.brightnesscurve.BrightnessCurveApp
import com.evan.brightnesscurve.MainActivity
import com.evan.brightnesscurve.R
import com.evan.brightnesscurve.brightness.BrightnessController
import com.evan.brightnesscurve.brightness.BrightnessMapping
import com.evan.brightnesscurve.brightness.BrightnessRamp
import com.evan.brightnesscurve.data.BrightnessPreset
import com.evan.brightnesscurve.data.ResponseSpeed
import com.evan.brightnesscurve.sensor.LightSensorSample
import com.evan.brightnesscurve.sensor.LightSensorMonitor
import com.evan.brightnesscurve.sensor.LightSensorStatus
import com.evan.brightnesscurve.sensor.LuxSmoother
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class BrightnessControlService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val writeMutex = Mutex()

    private lateinit var app: BrightnessCurveApp
    private lateinit var brightnessController: BrightnessController
    private var lightSensorMonitor: LightSensorMonitor? = null
    private var currentPreset: BrightnessPreset? = null
    private var allowOutdoorFull: Boolean = true
    private var minAllowedPercent: Float = 3f
    private var maxAllowedPercent: Float = 100f
    private var responseSpeed: ResponseSpeed = ResponseSpeed.Standard
    private var luxSmoother = LuxSmoother(responseSpeed.alpha)
    private var brightnessRamp = BrightnessRamp(responseSpeed.brightenStep, responseSpeed.darkenStep)
    private var lastWrittenPercent: Float? = null
    private var lastWriteElapsed: Long = 0L
    private var isScreenOn: Boolean = true
    private var luxTimeoutJob: Job? = null
    private var sensorStartedAtMillis: Long = 0L

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenOn = false
                    BrightnessRuntimeState.update {
                        it.copy(isPausedForScreenOff = true, autoEnabled = true, message = "屏幕关闭，暂停写入")
                    }
                }
                Intent.ACTION_SCREEN_ON -> {
                    isScreenOn = true
                    luxSmoother.reset()
                    lastWrittenPercent = brightnessController.readBrightnessPercent()
                    BrightnessRuntimeState.update {
                        it.copy(
                            isPausedForScreenOff = false,
                            autoEnabled = true,
                            brightnessMode = readModeOrNull(),
                            appliedBrightnessValue = brightnessController.readBrightness(),
                            message = "屏幕点亮，恢复控制"
                        )
                    }
                    scheduleLuxTimeout()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        app = application as BrightnessCurveApp
        brightnessController = BrightnessController(this)
        val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        lightSensorMonitor = LightSensorMonitor(
            sensorManager = sensorManager,
            onSample = ::handleLuxSample,
            onStatus = ::handleLightSensorStatus,
            onUnavailable = { reason ->
                BrightnessRuntimeState.update {
                    it.copy(
                        isRunning = false,
                        autoEnabled = false,
                        hasLightSensor = false,
                        lightSensorRegistered = false,
                        lightSensorTimedOut = false,
                        canWriteSettings = brightnessController.canWrite(),
                        brightnessMode = readModeOrNull(),
                        message = reason,
                        lastError = reason,
                        updatedAt = System.currentTimeMillis()
                    )
                }
                stopSelf()
            }
        )

        createNotificationChannel()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        )
        observeConfiguration()
        registerScreenReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ServiceActions.ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        serviceScope.launch(Dispatchers.IO) {
            if (!brightnessController.canWrite()) {
                BrightnessRuntimeState.update {
                    it.copy(
                        isRunning = false,
                        autoEnabled = false,
                        canWriteSettings = false,
                        brightnessMode = readModeOrNull(),
                        message = "缺少修改系统设置权限",
                        lastError = "缺少修改系统设置权限",
                        updatedAt = System.currentTimeMillis()
                    )
                }
                stopSelf()
                return@launch
            }

            app.presetRepository.ensureDefaults()
            captureOriginalSettings()
            app.preferencesRepository.setServiceEnabled(true)
            startSensor()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        luxTimeoutJob?.cancel()
        lightSensorMonitor?.stop()
        unregisterReceiver(screenReceiver)

        runBlocking(Dispatchers.IO) {
            restoreOriginalSettings()
            app.preferencesRepository.setServiceEnabled(false)
        }

        serviceScope.cancel()
        BrightnessRuntimeState.reset("服务已停止，已尝试恢复原亮度设置")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun handleLightSensorStatus(status: LightSensorStatus) {
        BrightnessRuntimeState.update {
            it.copy(
                hasLightSensor = status.hasLightSensor,
                lightSensorName = status.sensorName,
                lightSensorRegistered = status.isRegistered,
                canWriteSettings = brightnessController.canWrite(),
                brightnessMode = readModeOrNull(),
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    private fun handleLuxSample(sample: LightSensorSample) {
        val rawLux = sample.lux
        val preset = currentPreset

        val now = SystemClock.elapsedRealtime()
        val smoothedLux = luxSmoother.onSample(rawLux)
        val mode = readModeOrNull()
        val canWrite = brightnessController.canWrite()

        Log.d(
            TAG,
            "service lux=$rawLux, smoothed=$smoothedLux, sensor=${sample.sensorName}, preset=${preset?.name}, canWrite=$canWrite, mode=$mode"
        )

        if (preset == null) {
            BrightnessRuntimeState.update {
                it.copy(
                    isRunning = true,
                    autoEnabled = true,
                    rawLux = rawLux,
                    smoothedLux = smoothedLux,
                    lastLux = rawLux,
                    lastLuxUpdateTime = sample.receivedAtMillis,
                    targetPercent = null,
                    activePresetName = null,
                    hasLightSensor = true,
                    lightSensorName = sample.sensorName,
                    lightSensorRegistered = true,
                    lightSensorTimedOut = false,
                    canWriteSettings = canWrite,
                    brightnessMode = mode,
                    message = "已读取环境光，等待亮度曲线加载",
                    lastError = null,
                    updatedAt = System.currentTimeMillis()
                )
            }
            return
        }

        if (!isScreenOn) {
            BrightnessRuntimeState.update {
                it.copy(
                    isRunning = true,
                    autoEnabled = true,
                    rawLux = rawLux,
                    smoothedLux = smoothedLux,
                    lastLux = rawLux,
                    lastLuxUpdateTime = sample.receivedAtMillis,
                    hasLightSensor = true,
                    lightSensorName = sample.sensorName,
                    lightSensorRegistered = true,
                    lightSensorTimedOut = false,
                    canWriteSettings = canWrite,
                    brightnessMode = mode,
                    message = "已读取环境光，屏幕关闭时暂停写入",
                    lastError = null,
                    updatedAt = System.currentTimeMillis()
                )
            }
            return
        }

        val maxAllowed = if (allowOutdoorFull) maxAllowedPercent else maxAllowedPercent.coerceAtMost(85f)
        val mappedPercent = runCatching {
            BrightnessMapping.targetPercent(
                lux = smoothedLux,
                points = preset.points,
                minPercent = minAllowedPercent,
                maxPercent = maxAllowed
            )
        }.getOrElse { throwable ->
            val error = throwable.message ?: "亮度曲线计算失败"
            Log.e(TAG, "target mapping failed", throwable)
            BrightnessRuntimeState.update {
                it.copy(
                    isRunning = true,
                    autoEnabled = true,
                    rawLux = rawLux,
                    smoothedLux = smoothedLux,
                    lastLux = rawLux,
                    lastLuxUpdateTime = sample.receivedAtMillis,
                    hasLightSensor = true,
                    lightSensorName = sample.sensorName,
                    lightSensorRegistered = true,
                    lightSensorTimedOut = false,
                    canWriteSettings = canWrite,
                    brightnessMode = mode,
                    activePresetName = preset.name,
                    message = error,
                    lastError = error,
                    updatedAt = System.currentTimeMillis()
                )
            }
            return
        }

        if (mappedPercent.isNaN() || mappedPercent.isInfinite()) {
            val error = "亮度曲线结果无效"
            BrightnessRuntimeState.update {
                it.copy(
                    isRunning = true,
                    autoEnabled = true,
                    rawLux = rawLux,
                    smoothedLux = smoothedLux,
                    lastLux = rawLux,
                    lastLuxUpdateTime = sample.receivedAtMillis,
                    hasLightSensor = true,
                    lightSensorName = sample.sensorName,
                    lightSensorRegistered = true,
                    lightSensorTimedOut = false,
                    canWriteSettings = canWrite,
                    brightnessMode = mode,
                    activePresetName = preset.name,
                    message = error,
                    lastError = error,
                    updatedAt = System.currentTimeMillis()
                )
            }
            return
        }

        val (targetPercent, rampShouldWrite) = brightnessRamp.next(lastWrittenPercent, mappedPercent)
        val canWriteByTime = now - lastWriteElapsed >= MIN_WRITE_INTERVAL_MS
        val shouldWrite = rampShouldWrite && canWriteByTime

        BrightnessRuntimeState.update {
            it.copy(
                isRunning = true,
                autoEnabled = true,
                rawLux = rawLux,
                smoothedLux = smoothedLux,
                lastLux = rawLux,
                lastLuxUpdateTime = sample.receivedAtMillis,
                targetPercent = targetPercent,
                activePresetName = preset.name,
                hasLightSensor = true,
                lightSensorName = sample.sensorName,
                lightSensorRegistered = true,
                lightSensorTimedOut = false,
                canWriteSettings = canWrite,
                brightnessMode = mode,
                message = when {
                    !canWrite -> "已读取环境光，但缺少写入亮度权限"
                    shouldWrite -> "正在柔和调整亮度"
                    !canWriteByTime -> "已感知光线变化，等待节流窗口"
                    else -> "变化较小，保持当前亮度"
                },
                lastError = if (canWrite) null else "缺少修改系统设置权限",
                updatedAt = System.currentTimeMillis()
            )
        }

        if (!shouldWrite) return

        serviceScope.launch(Dispatchers.IO) {
            writeMutex.withLock {
                if (!brightnessController.canWrite()) {
                    BrightnessRuntimeState.update {
                        it.copy(
                            canWriteSettings = false,
                            brightnessMode = readModeOrNull(),
                            message = "写入前权限失效，已暂停亮度写入",
                            lastError = "写入前权限失效",
                            updatedAt = System.currentTimeMillis()
                        )
                    }
                    return@withLock
                }
                val appliedValue = brightnessController.writeManualBrightness(targetPercent)
                Log.d(TAG, "write brightness targetPercent=$targetPercent, systemValue=$appliedValue")
                lastWrittenPercent = targetPercent
                lastWriteElapsed = SystemClock.elapsedRealtime()
                BrightnessRuntimeState.update {
                    it.copy(
                        writtenPercent = targetPercent,
                        appliedBrightnessValue = appliedValue,
                        canWriteSettings = true,
                        brightnessMode = readModeOrNull(),
                        lastError = null,
                        updatedAt = System.currentTimeMillis()
                    )
                }
            }
        }
    }

    private fun observeConfiguration() {
        serviceScope.launch {
            app.presetRepository.observeActivePreset().collectLatest { preset ->
                currentPreset = preset
                if (preset != null) {
                    luxSmoother.reset()
                    lastWrittenPercent = brightnessController.readBrightnessPercent()
                    BrightnessRuntimeState.update {
                        it.copy(
                            activePresetName = preset.name,
                            appliedBrightnessValue = brightnessController.readBrightness(),
                            brightnessMode = readModeOrNull(),
                            canWriteSettings = brightnessController.canWrite(),
                            message = "已加载预设：${preset.name}",
                            updatedAt = System.currentTimeMillis()
                        )
                    }
                } else {
                    BrightnessRuntimeState.update {
                        it.copy(
                            activePresetName = null,
                            message = "没有启用的亮度曲线",
                            lastError = "没有启用的亮度曲线",
                            updatedAt = System.currentTimeMillis()
                        )
                    }
                }
            }
        }

        serviceScope.launch {
            app.preferencesRepository.settings.collectLatest { settings ->
                allowOutdoorFull = settings.allowOutdoorFull
                minAllowedPercent = settings.minAllowedPercent
                maxAllowedPercent = settings.maxAllowedPercent
                responseSpeed = settings.responseSpeed
                luxSmoother = LuxSmoother(responseSpeed.alpha)
                brightnessRamp = BrightnessRamp(
                    brightenStepPercent = responseSpeed.brightenStep,
                    darkenStepPercent = responseSpeed.darkenStep
                )
                BrightnessRuntimeState.update {
                    it.copy(autoEnabled = settings.serviceEnabled || it.isRunning)
                }
            }
        }
    }

    private suspend fun captureOriginalSettings() {
        val original = app.preferencesRepository.originalSettings()
        if (original != null) return
        app.preferencesRepository.saveOriginalSettings(
            mode = brightnessController.readMode(),
            brightness = brightnessController.readBrightness()
        )
    }

    private suspend fun restoreOriginalSettings() {
        val original = app.preferencesRepository.originalSettings() ?: return
        if (brightnessController.canWrite()) {
            brightnessController.restore(original.mode, original.brightness)
        }
        app.preferencesRepository.clearOriginalSettings()
    }

    private fun startSensor() {
        if (brightnessController.isAutoMode()) {
            BrightnessRuntimeState.update {
                it.copy(message = "当前系统为自动亮度；启动后会切换为手动亮度控制")
            }
        }
        lastWrittenPercent = brightnessController.readBrightnessPercent()
        val started = lightSensorMonitor?.start() == true
        sensorStartedAtMillis = System.currentTimeMillis()
        if (started) scheduleLuxTimeout()
        BrightnessRuntimeState.update {
            it.copy(
                isRunning = started,
                autoEnabled = started,
                lightSensorTimedOut = false,
                canWriteSettings = brightnessController.canWrite(),
                brightnessMode = readModeOrNull(),
                appliedBrightnessValue = brightnessController.readBrightness(),
                message = if (started) "亮度控制已启动，正在读取环境光" else "光线传感器启动失败",
                lastError = if (started) null else "光线传感器启动失败",
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    private fun scheduleLuxTimeout() {
        luxTimeoutJob?.cancel()
        luxTimeoutJob = serviceScope.launch {
            val startedAt = sensorStartedAtMillis
            delay(LUX_TIMEOUT_MS)
            val state = BrightnessRuntimeState.state.value
            if (state.isRunning && (state.lastLuxUpdateTime == null || state.lastLuxUpdateTime < startedAt)) {
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

    private fun registerScreenReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        ContextCompat.registerReceiver(
            this,
            screenReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification() =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_brightness)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .addAction(
                R.drawable.ic_stat_brightness,
                "停止",
                PendingIntent.getService(
                    this,
                    1,
                    Intent(this, BrightnessControlService::class.java)
                        .setAction(ServiceActions.ACTION_STOP),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .build()

    companion object {
        private const val TAG = "BrightnessControlService"
        private const val CHANNEL_ID = "brightness_curve_controller"
        private const val NOTIFICATION_ID = 20
        private const val MIN_WRITE_INTERVAL_MS = 220L
        private const val LUX_TIMEOUT_MS = 5_000L
    }
}
