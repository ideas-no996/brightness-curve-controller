package com.evan.brightnesscurve.brightness

import android.content.Context
import android.provider.Settings

object BrightnessPermissionManager {
    fun canWrite(context: Context): Boolean = Settings.System.canWrite(context)
}
