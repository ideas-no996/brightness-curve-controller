package com.evan.brightnesscurve.brightness

import org.junit.Assert.assertEquals
import org.junit.Test

class BrightnessControllerTest {
    @Test
    fun `percent conversion uses Android system brightness range`() {
        assertEquals(0, BrightnessController.percentToSystemValue(0f))
        assertEquals(128, BrightnessController.percentToSystemValue(50f))
        assertEquals(255, BrightnessController.percentToSystemValue(100f))
    }
}
