package com.evan.brightnesscurve.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class LightSensorMonitor(
    private val sensorManager: SensorManager,
    private val onLux: (Float) -> Unit,
    private val onUnavailable: () -> Unit
) : SensorEventListener {
    private val lightSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    private var registered = false

    fun start() {
        val sensor = lightSensor
        if (sensor == null) {
            onUnavailable()
            return
        }
        if (!registered) {
            sensorManager.registerListener(this, sensor, SENSOR_SAMPLING_US)
            registered = true
        }
    }

    fun stop() {
        if (registered) {
            sensorManager.unregisterListener(this)
            registered = false
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        event.values.firstOrNull()?.let(onLux)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        private const val SENSOR_SAMPLING_US = 250_000
    }
}
