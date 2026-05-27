package com.evan.brightnesscurve.ui

import com.evan.brightnesscurve.service.RuntimeSnapshot
import com.evan.brightnesscurve.service.RuntimeStatus
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticReportTest {
    @Test
    fun `diagnostic report includes core brightness loop evidence`() {
        val state = MainUiState(
            settingsLoaded = true,
            canWriteSettings = true,
            runtime = RuntimeSnapshot(
                status = RuntimeStatus.AutoRunning,
                isRunning = true,
                rawLux = 120f,
                smoothedLux = 100f,
                targetPercent = 40f,
                targetSystemValue = 102,
                currentBrightnessValue = 101,
                lastWriteTargetValue = 102,
                lastWriteReadBackValue = 102,
                lastWriteSucceeded = true,
                lastNoWriteReason = "Throttled",
                brightnessMode = 0,
                lightSensorName = "Test ALS",
                lightSensorRegistered = true
            )
        )

        val report = buildDiagnosticReport(
            state = state,
            deviceInfo = DiagnosticDeviceInfo(
                manufacturer = "TestMaker",
                model = "TestModel",
                androidRelease = "14",
                sdkInt = 35,
                fingerprint = "test/fingerprint"
            ),
            generatedAtMillis = 123L
        )

        assertTrue(report.contains("generatedAtMillis=123"))
        assertTrue(report.contains("manufacturer=TestMaker"))
        assertTrue(report.contains("runtimeStatus=AutoRunning"))
        assertTrue(report.contains("canWriteSettings=true"))
        assertTrue(report.contains("rawLux=120.0"))
        assertTrue(report.contains("smoothedLux=100.0"))
        assertTrue(report.contains("targetPercent=40.0"))
        assertTrue(report.contains("targetSystemValue=102"))
        assertTrue(report.contains("currentSystemBrightness=101"))
        assertTrue(report.contains("lastWriteTargetValue=102"))
        assertTrue(report.contains("lastWriteReadBackValue=102"))
        assertTrue(report.contains("lastWriteSucceeded=true"))
        assertTrue(report.contains("lastNoWriteReason=Throttled"))
        assertTrue(report.contains("lightSensorName=Test ALS"))
    }
}
