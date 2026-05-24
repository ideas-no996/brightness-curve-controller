package com.evan.brightnesscurve.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.evan.brightnesscurve.BrightnessCurveApp
import com.evan.brightnesscurve.system.BrightnessSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        val app = context.applicationContext as BrightnessCurveApp
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val settings = app.preferencesRepository.currentSettings()
            if (settings.startOnBoot && settings.serviceEnabled && BrightnessSettings.canWrite(context)) {
                ServiceController.start(context)
            }
            pendingResult.finish()
        }
    }
}
