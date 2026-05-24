package com.evan.brightnesscurve.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

object ServiceController {
    fun start(context: Context) {
        val intent = Intent(context, BrightnessControlService::class.java)
            .setAction(ServiceActions.ACTION_START)
        ContextCompat.startForegroundService(context, intent)
    }

    fun stop(context: Context) {
        val intent = Intent(context, BrightnessControlService::class.java)
            .setAction(ServiceActions.ACTION_STOP)
        context.startService(intent)
    }
}
