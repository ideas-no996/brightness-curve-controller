package com.evan.brightnesscurve.brightness

import android.content.ContentResolver
import android.content.Context
import android.provider.Settings
import kotlin.math.roundToInt

class BrightnessController(private val context: Context) {
    private val resolver: ContentResolver = context.contentResolver

    fun canWrite(): Boolean = BrightnessPermissionManager.canWrite(context)

    fun readMode(): Int =
        Settings.System.getInt(
            resolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        )

    fun isAutoMode(): Boolean = readMode() == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC

    fun readBrightness(): Int =
        Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS, 128)

    fun readBrightnessPercent(): Float =
        readBrightness().coerceIn(MIN_SYSTEM_BRIGHTNESS, MAX_SYSTEM_BRIGHTNESS) /
            MAX_SYSTEM_BRIGHTNESS.toFloat() * 100f

    fun writeManualBrightness(percent: Float): Int {
        val systemValue = percentToSystemValue(percent)
        val modeWritten = Settings.System.putInt(
            resolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        )
        val brightnessWritten = Settings.System.putInt(
            resolver,
            Settings.System.SCREEN_BRIGHTNESS,
            systemValue
        )
        check(modeWritten && brightnessWritten) { "系统拒绝写入亮度设置" }
        return systemValue
    }

    fun restore(mode: Int, brightness: Int) {
        Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, mode)
        Settings.System.putInt(
            resolver,
            Settings.System.SCREEN_BRIGHTNESS,
            brightness.coerceIn(MIN_SYSTEM_BRIGHTNESS, MAX_SYSTEM_BRIGHTNESS)
        )
    }

    companion object {
        const val MIN_SYSTEM_BRIGHTNESS = 1
        const val MAX_SYSTEM_BRIGHTNESS = 255

        fun percentToSystemValue(percent: Float): Int =
            (percent.coerceIn(1f, 100f) / 100f * MAX_SYSTEM_BRIGHTNESS)
                .roundToInt()
                .coerceIn(MIN_SYSTEM_BRIGHTNESS, MAX_SYSTEM_BRIGHTNESS)
    }
}
