package com.evan.brightnesscurve.system

import android.content.ContentResolver
import android.content.Context
import android.provider.Settings
import com.evan.brightnesscurve.brightness.BrightnessController
import com.evan.brightnesscurve.brightness.BrightnessPermissionManager

object BrightnessSettings {
    fun canWrite(context: Context): Boolean = BrightnessPermissionManager.canWrite(context)

    fun readMode(resolver: ContentResolver): Int =
        Settings.System.getInt(
            resolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        )

    fun readBrightness(resolver: ContentResolver): Int =
        Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS, 128)

    fun readBrightnessPercent(resolver: ContentResolver): Float =
        readBrightness(resolver).coerceIn(0, 255) / 255f * 100f

    fun writeManualBrightness(resolver: ContentResolver, percent: Float) {
        Settings.System.putInt(
            resolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        )
        Settings.System.putInt(
            resolver,
            Settings.System.SCREEN_BRIGHTNESS,
            percentToSystemValue(percent)
        )
    }

    fun restore(resolver: ContentResolver, mode: Int, brightness: Int) {
        Settings.System.putInt(
            resolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            mode
        )
        Settings.System.putInt(
            resolver,
            Settings.System.SCREEN_BRIGHTNESS,
            brightness.coerceIn(1, 255)
        )
    }

    fun percentToSystemValue(percent: Float): Int =
        BrightnessController.percentToSystemValue(percent)
}
