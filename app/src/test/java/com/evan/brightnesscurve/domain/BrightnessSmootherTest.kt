package com.evan.brightnesscurve.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrightnessSmootherTest {
    @Test
    fun firstSampleAlwaysWrites() {
        val smoother = BrightnessSmoother(
            smoothingLevel = 0.35f,
            maxChangePerUpdate = 8f,
            minUpdateDelta = 3f
        )

        val decision = smoother.onSample(rawLux = 100f) { 20f }

        assertTrue(decision.shouldWrite)
        assertEquals(20f, decision.targetPercent, 0.1f)
    }

    @Test
    fun smallTargetChangeIsDebounced() {
        val smoother = BrightnessSmoother(
            smoothingLevel = 0.35f,
            maxChangePerUpdate = 8f,
            minUpdateDelta = 3f
        )

        smoother.onSample(rawLux = 100f) { 20f }
        val decision = smoother.onSample(rawLux = 120f) { 21.5f }

        assertFalse(decision.shouldWrite)
    }

    @Test
    fun largeJumpIsRateLimited() {
        val smoother = BrightnessSmoother(
            smoothingLevel = 0.35f,
            maxChangePerUpdate = 8f,
            minUpdateDelta = 3f
        )

        smoother.onSample(rawLux = 100f) { 20f }
        val decision = smoother.onSample(rawLux = 10000f) { 90f }

        assertTrue(decision.shouldWrite)
        assertEquals(28f, decision.targetPercent, 0.1f)
    }
}
