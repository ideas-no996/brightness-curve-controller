package com.evan.brightnesscurve.sensor

import android.content.Context
import android.hardware.SensorManager
import com.evan.brightnesscurve.data.ResponseSpeed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class PassiveLightSensorSample(
    val rawLux: Float,
    val smoothedLux: Float,
    val receivedAtMillis: Long,
    val sensorName: String?
)

class PassiveLightSensorPreview(
    context: Context,
    private val scope: CoroutineScope,
    private val onSample: (PassiveLightSensorSample) -> Unit,
    private val onStatus: (LightSensorStatus) -> Unit,
    private val onUnavailable: (String) -> Unit,
    private val onTimeout: (Long) -> Unit,
    private val timeoutMillis: Long = 5_000L
) {
    private val sensorManager = context.applicationContext
        .getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val luxSmoother = LuxSmoother(ResponseSpeed.Standard.alpha)
    private var monitor: LightSensorMonitor? = null
    private var timeoutJob: Job? = null
    private var startedAtMillis: Long = 0L

    fun start() {
        stop()
        luxSmoother.reset()
        startedAtMillis = System.currentTimeMillis()
        monitor = LightSensorMonitor(
            sensorManager = sensorManager,
            onSample = ::handleSample,
            onStatus = onStatus,
            onUnavailable = onUnavailable
        )
        if (monitor?.start() == true) {
            scheduleTimeout()
        }
    }

    fun stop() {
        timeoutJob?.cancel()
        timeoutJob = null
        monitor?.stop()
        monitor = null
    }

    private fun handleSample(sample: LightSensorSample) {
        onSample(
            PassiveLightSensorSample(
                rawLux = sample.lux,
                smoothedLux = luxSmoother.onSample(sample.lux),
                receivedAtMillis = sample.receivedAtMillis,
                sensorName = sample.sensorName
            )
        )
    }

    private fun scheduleTimeout() {
        timeoutJob?.cancel()
        timeoutJob = scope.launch {
            val startedAt = startedAtMillis
            delay(timeoutMillis)
            onTimeout(startedAt)
        }
    }
}
