package com.evan.brightnesscurve

import android.app.Application
import androidx.room.Room
import com.evan.brightnesscurve.data.AppPreferencesRepository
import com.evan.brightnesscurve.data.BrightnessDatabase
import com.evan.brightnesscurve.data.BrightnessPresetRepository

class BrightnessCurveApp : Application() {
    val database: BrightnessDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            BrightnessDatabase::class.java,
            "brightness_curve.db"
        ).build()
    }

    val preferencesRepository: AppPreferencesRepository by lazy {
        AppPreferencesRepository(applicationContext)
    }

    val presetRepository: BrightnessPresetRepository by lazy {
        BrightnessPresetRepository(database, preferencesRepository)
    }
}
