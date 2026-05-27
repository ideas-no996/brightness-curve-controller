package com.evan.brightnesscurve.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TutorialVisibilityTest {
    @Test
    fun `does not auto show before persisted settings are loaded`() {
        assertFalse(
            shouldAutoShowTutorial(
                settingsLoaded = false,
                hasSeenTutorial = false,
                showTutorialOnStartup = true
            )
        )
    }

    @Test
    fun `auto shows for first run after settings load`() {
        assertTrue(
            shouldAutoShowTutorial(
                settingsLoaded = true,
                hasSeenTutorial = false,
                showTutorialOnStartup = false
            )
        )
    }

    @Test
    fun `does not auto show after user opted out`() {
        assertFalse(
            shouldAutoShowTutorial(
                settingsLoaded = true,
                hasSeenTutorial = true,
                showTutorialOnStartup = false
            )
        )
    }

    @Test
    fun `auto shows after user explicitly enables startup tutorial`() {
        assertTrue(
            shouldAutoShowTutorial(
                settingsLoaded = true,
                hasSeenTutorial = true,
                showTutorialOnStartup = true
            )
        )
    }
}
