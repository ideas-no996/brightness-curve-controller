package com.evan.brightnesscurve.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

data class LightSensorSample(
    val lux: Float,
    val sensorName: String,
    val sensorTimestampNanos: Long,
    val receivedAtMillis: Long
)

data class LightSensorStatus(
    val hasLightSensor: Boolean,
    val sensorName: String?,
    val isRegistered: Boolean
)

class LightSensorMonitor(
    private val sensorManager: SensorManager,
    private val onSample: (LightSensorSample) -> Unit,
    private val onStatus: (LightSensorStatus) -> Unit,
    private val onUnavailable: (String) -> Unit
) : SensorEventListener {
    private val lightSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    private var registered = false

    fun start(): Boolean {
        val sensor = lightSensor
        if (sensor == null) {
            Log.w(TAG, "TYPE_LIGHT sensor unavailable")
            onStatus(LightSensorStatus(hasLightSensor = false, sensorName = null, isRegistered = false))
            onUnavailable("设备没有可用的光线传感器")
            return false
        }
        if (!registered) {
            registered = sensorManager.registerListener(this, sensor, SENSOR_SAMPLING_US)
            Log.i(
                TAG,
                "register TYPE_LIGHT sensor name=${sensor.name}, vendor=${sensor.vendor}, samplingUs=$SENSOR_SAMPLING_US, registered=$registered"
            )
            onStatus(
                LightSensorStatus(
                    hasLightSensor = true,
                    sensorName = sensor.name,
                    isRegistered = registered
                )
            )
            if (!registered) {
                onUnavailable("光线传感器注册失败")
                return false
            }
        }
        return true
    }

    fun stop() {
        if (registered) {
            Log.i(TAG, "unregister TYPE_LIGHT sensor")
            sensorManager.unregisterListener(this)
            registered = false
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_LIGHT) return
        val lux = event.values.firstOrNull()
        if (lux == null || lux.isNaN() || lux.isInfinite() || lux < 0f) {
            Log.w(TAG, "invalid light sample lux=$lux, sensor=${event.sensor.name}")
            return
        }
        Log.d(
            TAG,
            "lux=$lux, sensor=${event.sensor.name}, sensorTimestamp=${event.timestamp}, receivedAt=${System.currentTimeMillis()}"
        )
        onSample(
            LightSensorSample(
                lux = lux,
                sensorName = event.sensor.name,
                sensorTimestampNanos = event.timestamp,
                receivedAtMillis = System.currentTimeMillis()
            )
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        private const val TAG = "LightSensorMonitor"
        private const val SENSOR_SAMPLING_US = 250_000
    }
}
