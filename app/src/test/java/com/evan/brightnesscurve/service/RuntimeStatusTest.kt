package com.evan.brightnesscurve.service

import org.junit.Assert.assertEquals
import org.junit.Test

class RuntimeStatusTest {
    @Test
    fun `permission missing beats waiting sensor state when auto is requested`() {
        val snapshot = RuntimeSnapshot(
            autoControlDesired = true,
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
            failureReason = RuntimeFailureReason.WriteFailed
        )

        assertEquals(RuntimeStatus.WriteFailed, snapshot.resolvedRuntimeStatus())
    }

    @Test
    fun `service stopped keeps last error for diagnostics`() {
        val snapshot = reduceRuntimeSnapshot(
            RuntimeSnapshot(
                isRunning = true,
                autoControlDesired = true,
                lightSensorRegistered = true,
                lastError = "5 秒内未收到环境光数据",
                failureReason = RuntimeFailureReason.SensorTimeout
            ),
            RuntimeEvent.ServiceStopped("服务已停止，已尝试恢复原亮度设置")
        )

        assertEquals(false, snapshot.isRunning)
        assertEquals(true, snapshot.autoControlDesired)
        assertEquals("5 秒内未收到环境光数据", snapshot.lastError)
        assertEquals(RuntimeFailureReason.SensorTimeout, snapshot.failureReason)
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
                targetSystemValue = null,
                preserveExistingTargetPercent = true,
                canWriteSettings = true,
                brightnessMode = 0,
                noWriteReason = null,
                message = "已读取环境光，屏幕关闭时暂停写入",
                lastError = null,
                isPausedForScreenOff = true
            )
        )

        assertEquals(42f, snapshot.targetPercent)
        assertEquals(RuntimeStatus.PausedScreenOff, snapshot.status)
    }

    @Test
    fun `service lux records target system value and no write reason`() {
        val snapshot = reduceRuntimeSnapshot(
            RuntimeSnapshot(isRunning = true, canWriteSettings = true),
            RuntimeEvent.ServiceLuxObserved(
                rawLux = 18f,
                smoothedLux = 20f,
                receivedAtMillis = 1000L,
                sensorName = "ALS",
                activePresetName = "Preset",
                targetPercent = 50f,
                targetSystemValue = 128,
                preserveExistingTargetPercent = false,
                canWriteSettings = true,
                brightnessMode = 0,
                noWriteReason = "Throttled",
                message = "已感知光线变化，等待节流窗口",
                lastError = null,
                isPausedForScreenOff = false
            )
        )

        assertEquals(50f, snapshot.targetPercent)
        assertEquals(128, snapshot.targetSystemValue)
        assertEquals("Throttled", snapshot.lastNoWriteReason)
    }

    @Test
    fun `write failure event produces write failed status when permission remains available`() {
        val snapshot = reduceRuntimeSnapshot(
            RuntimeSnapshot(isRunning = true, canWriteSettings = true),
            RuntimeEvent.ServiceBrightnessWriteFailed(
                error = "系统拒绝写入亮度设置",
                targetSystemValue = 128,
                canWriteSettings = true,
                brightnessMode = 0,
                currentBrightnessValue = 120
            )
        )

        assertEquals(RuntimeFailureReason.WriteFailed, snapshot.failureReason)
        assertEquals(RuntimeStatus.WriteFailed, snapshot.status)
    }

    @Test
    fun `write failure event produces permission status when permission was lost`() {
        val snapshot = reduceRuntimeSnapshot(
            RuntimeSnapshot(isRunning = true, canWriteSettings = true),
            RuntimeEvent.ServiceBrightnessWriteFailed(
                error = "写入失败",
                targetSystemValue = 128,
                canWriteSettings = false,
                brightnessMode = 0,
                currentBrightnessValue = 120
            )
        )

        assertEquals(RuntimeFailureReason.PermissionMissing, snapshot.failureReason)
        assertEquals(RuntimeStatus.PermissionMissing, snapshot.status)
    }

    @Test
    fun `successful brightness write records target and read back values`() {
        val snapshot = reduceRuntimeSnapshot(
            RuntimeSnapshot(isRunning = true, canWriteSettings = true),
            RuntimeEvent.ServiceBrightnessWritten(
                writtenPercent = 50f,
                targetSystemValue = 128,
                readBackSystemValue = 128,
                brightnessMode = 0
            )
        )

        assertEquals(50f, snapshot.writtenPercent)
        assertEquals(128, snapshot.lastWriteTargetValue)
        assertEquals(128, snapshot.lastWriteReadBackValue)
        assertEquals(true, snapshot.lastWriteSucceeded)
        assertEquals(null, snapshot.lastError)
    }
}
