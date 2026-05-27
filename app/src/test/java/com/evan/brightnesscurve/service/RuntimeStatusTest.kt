package com.evan.brightnesscurve.service

import org.junit.Assert.assertEquals
import org.junit.Test

class RuntimeStatusTest {
    @Test
    fun `permission missing beats waiting sensor state when auto is requested`() {
        val snapshot = RuntimeSnapshot(
            autoEnabled = true,
            lightSensorRegistered = true,
            canWriteSettings = false
        )

        assertEquals(RuntimeStatus.PermissionMissing, snapshot.resolvedRuntimeStatus())
    }

    @Test
    fun `sensor timeout beats generic running state`() {
        val snapshot = RuntimeSnapshot(
            isRunning = true,
            lightSensorTimedOut = true,
            canWriteSettings = true
        )

        assertEquals(RuntimeStatus.SensorTimeout, snapshot.resolvedRuntimeStatus())
    }

    @Test
    fun `screen off pause beats auto running`() {
        val snapshot = RuntimeSnapshot(
            isRunning = true,
            isPausedForScreenOff = true,
            canWriteSettings = true
        )

        assertEquals(RuntimeStatus.PausedScreenOff, snapshot.resolvedRuntimeStatus())
    }

    @Test
    fun `last lux without running is sensor ready`() {
        val snapshot = RuntimeSnapshot(
            isRunning = false,
            lastLux = 120f,
            canWriteSettings = true
        )

        assertEquals(RuntimeStatus.SensorReady, snapshot.resolvedRuntimeStatus())
    }

    @Test
    fun `write failure beats auto running`() {
        val snapshot = RuntimeSnapshot(
            isRunning = true,
            canWriteSettings = true,
            lastError = "写入前权限失效"
        )

        assertEquals(RuntimeStatus.WriteFailed, snapshot.resolvedRuntimeStatus())
    }

    @Test
    fun `service stopped keeps last error for diagnostics`() {
        val snapshot = reduceRuntimeSnapshot(
            RuntimeSnapshot(
                isRunning = true,
                autoEnabled = true,
                lightSensorRegistered = true,
                lastError = "5 秒内未收到环境光数据"
            ),
            RuntimeEvent.ServiceStopped("服务已停止，已尝试恢复原亮度设置")
        )

        assertEquals(false, snapshot.isRunning)
        assertEquals(false, snapshot.autoEnabled)
        assertEquals("5 秒内未收到环境光数据", snapshot.lastError)
        assertEquals("服务已停止，已尝试恢复原亮度设置", snapshot.message)
    }

    @Test
    fun `passive sensor status produces detecting state before first lux`() {
        val snapshot = reduceRuntimeSnapshot(
            RuntimeSnapshot(),
            RuntimeEvent.PassiveSensorStatusChanged(
                hasLightSensor = true,
                sensorName = "ALS",
                isRegistered = true,
                canWriteSettings = true,
                brightnessMode = 0
            )
        )

        assertEquals(RuntimeStatus.DetectingSensor, snapshot.status)
        assertEquals("正在读取环境光", snapshot.message)
    }

    @Test
    fun `paused service lux keeps prior target percent`() {
        val snapshot = reduceRuntimeSnapshot(
            RuntimeSnapshot(targetPercent = 42f),
            RuntimeEvent.ServiceLuxObserved(
                rawLux = 18f,
                smoothedLux = 20f,
                receivedAtMillis = 1000L,
                sensorName = "ALS",
                activePresetName = "Preset",
                targetPercent = null,
                preserveExistingTargetPercent = true,
                canWriteSettings = true,
                brightnessMode = 0,
                message = "已读取环境光，屏幕关闭时暂停写入",
                lastError = null,
                isPausedForScreenOff = true
            )
        )

        assertEquals(42f, snapshot.targetPercent)
        assertEquals(RuntimeStatus.PausedScreenOff, snapshot.status)
    }
}
