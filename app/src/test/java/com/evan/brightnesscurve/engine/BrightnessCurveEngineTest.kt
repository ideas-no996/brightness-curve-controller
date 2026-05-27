package com.evan.brightnesscurve.engine

import com.evan.brightnesscurve.data.BrightnessPoint
import com.evan.brightnesscurve.data.DefaultPresets
import com.evan.brightnesscurve.data.ResponseSpeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BrightnessCurveEngineTest {
    @Test
    fun `first valid sample writes mapped brightness`() {
        val engine = BrightnessCurveEngine()

        val decision = engine.decide(
            rawLux = 100f,
            points = DefaultPresets.comfort20Points,
            minPercent = 3f,
            maxPercent = 100f,
            responseSpeed = ResponseSpeed.Standard,
            lastWrittenPercent = null,
            nowElapsedMillis = 1_000L,
            lastWriteElapsedMillis = 0L
        )

        assertTrue(decision.shouldWrite)
        assertEquals(20f, decision.targetPercent, 0.1f)
    }

    @Test
    fun `write is throttled inside minimum interval`() {
        val engine = BrightnessCurveEngine()

        val decision = engine.decide(
            rawLux = 25_000f,
            points = DefaultPresets.comfort20Points,
            minPercent = 3f,
            maxPercent = 100f,
            responseSpeed = ResponseSpeed.Standard,
            lastWrittenPercent = 20f,
            nowElapsedMillis = 100L,
            lastWriteElapsedMillis = 0L
        )

        assertFalse(decision.shouldWrite)
        assertEquals(NoWriteReason.Throttled, decision.noWriteReason)
    }

    @Test
    fun `small brightness change is skipped after throttle window`() {
        val engine = BrightnessCurveEngine()

        val decision = engine.decide(
            rawLux = 100f,
            points = DefaultPresets.comfort20Points,
            minPercent = 3f,
            maxPercent = 100f,
            responseSpeed = ResponseSpeed.Standard,
            lastWrittenPercent = 20f,
            nowElapsedMillis = 1_000L,
            lastWriteElapsedMillis = 0L
        )

        assertFalse(decision.shouldWrite)
        assertEquals(NoWriteReason.ChangeTooSmall, decision.noWriteReason)
    }

    @Test
    fun `invalid curve points are rejected`() {
        val engine = BrightnessCurveEngine()
        val points = listOf(
            BrightnessPoint(0f, 10f),
            BrightnessPoint(10f, 20f),
            BrightnessPoint(10f, 30f)
        )

        assertThrows(IllegalArgumentException::class.java) {
            engine.decide(
                rawLux = 5f,
                points = points,
                minPercent = 3f,
                maxPercent = 100f,
                responseSpeed = ResponseSpeed.Standard,
                lastWrittenPercent = null,
                nowElapsedMillis = 1_000L,
                lastWriteElapsedMillis = 0L
            )
        }
    }
}
