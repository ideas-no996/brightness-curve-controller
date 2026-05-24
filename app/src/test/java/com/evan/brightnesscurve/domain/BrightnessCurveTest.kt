package com.evan.brightnesscurve.domain

import com.evan.brightnesscurve.data.BrightnessPoint
import com.evan.brightnesscurve.data.DefaultPresets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BrightnessCurveTest {
    @Test
    fun defaultComfortCurveKeepsIndoorLightNearTwentyPercent() {
        val points = DefaultPresets.comfort20Points

        assertEquals(20f, BrightnessCurve.targetPercent(100f, points, true), 0.1f)
        assertEquals(20f, BrightnessCurve.targetPercent(500f, points, true), 0.1f)
    }

    @Test
    fun defaultComfortCurveAllowsOutdoorFullBrightness() {
        val points = DefaultPresets.comfort20Points

        assertEquals(90f, BrightnessCurve.targetPercent(10000f, points, true), 0.1f)
        assertEquals(100f, BrightnessCurve.targetPercent(25000f, points, true), 0.1f)
    }

    @Test
    fun outdoorCapLimitsBrightnessWhenDisabled() {
        val points = DefaultPresets.comfort20Points

        assertEquals(85f, BrightnessCurve.targetPercent(25000f, points, false), 0.1f)
    }

    @Test
    fun validationRejectsNonIncreasingLux() {
        val points = listOf(
            BrightnessPoint(0f, 10f),
            BrightnessPoint(10f, 20f),
            BrightnessPoint(10f, 30f)
        )

        assertThrows(IllegalArgumentException::class.java) {
            BrightnessCurve.validate(points)
        }
    }

    @Test
    fun validationRejectsOutOfRangeBrightness() {
        val points = listOf(
            BrightnessPoint(0f, 10f),
            BrightnessPoint(10f, 0f),
            BrightnessPoint(20f, 30f)
        )

        assertThrows(IllegalArgumentException::class.java) {
            BrightnessCurve.validate(points)
        }
    }
}
