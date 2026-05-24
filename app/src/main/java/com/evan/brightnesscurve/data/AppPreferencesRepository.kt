package com.evan.brightnesscurve.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.brightnessDataStore by preferencesDataStore("brightness_controller_settings")

data class AppSettings(
    val activePresetId: Long?,
    val serviceEnabled: Boolean,
    val startOnBoot: Boolean,
    val allowOutdoorFull: Boolean,
    val lastComfortPercent: Float,
    val minAllowedPercent: Float,
    val maxAllowedPercent: Float,
    val responseSpeed: ResponseSpeed
)

data class OriginalBrightnessSettings(
    val mode: Int,
    val brightness: Int
)

class AppPreferencesRepository(private val context: Context) {
    private object Keys {
        val ActivePresetId = longPreferencesKey("active_preset_id")
        val ServiceEnabled = booleanPreferencesKey("service_enabled")
        val StartOnBoot = booleanPreferencesKey("start_on_boot")
        val AllowOutdoorFull = booleanPreferencesKey("allow_outdoor_full")
        val LastComfortPercent = floatPreferencesKey("last_comfort_percent")
        val MinAllowedPercent = floatPreferencesKey("min_allowed_percent")
        val MaxAllowedPercent = floatPreferencesKey("max_allowed_percent")
        val ResponseSpeed = stringPreferencesKey("response_speed")
        val OriginalMode = intPreferencesKey("original_mode")
        val OriginalBrightness = intPreferencesKey("original_brightness")
    }

    val settings: Flow<AppSettings> = context.brightnessDataStore.data.map { prefs ->
        AppSettings(
            activePresetId = prefs[Keys.ActivePresetId],
            serviceEnabled = prefs[Keys.ServiceEnabled] ?: false,
            startOnBoot = prefs[Keys.StartOnBoot] ?: false,
            allowOutdoorFull = prefs[Keys.AllowOutdoorFull] ?: true,
            lastComfortPercent = prefs[Keys.LastComfortPercent] ?: 20f,
            minAllowedPercent = prefs[Keys.MinAllowedPercent] ?: 3f,
            maxAllowedPercent = prefs[Keys.MaxAllowedPercent] ?: 100f,
            responseSpeed = ResponseSpeed.fromStored(prefs[Keys.ResponseSpeed])
        )
    }

    suspend fun currentSettings(): AppSettings = settings.first()

    suspend fun setActivePresetId(id: Long) {
        context.brightnessDataStore.edit { it[Keys.ActivePresetId] = id }
    }

    suspend fun setServiceEnabled(enabled: Boolean) {
        context.brightnessDataStore.edit { it[Keys.ServiceEnabled] = enabled }
    }

    suspend fun setStartOnBoot(enabled: Boolean) {
        context.brightnessDataStore.edit { it[Keys.StartOnBoot] = enabled }
    }

    suspend fun setAllowOutdoorFull(enabled: Boolean) {
        context.brightnessDataStore.edit { it[Keys.AllowOutdoorFull] = enabled }
    }

    suspend fun setLastComfortPercent(percent: Float) {
        context.brightnessDataStore.edit {
            it[Keys.LastComfortPercent] = percent.coerceIn(1f, 100f)
        }
    }

    suspend fun setMinAllowedPercent(percent: Float) {
        context.brightnessDataStore.edit { prefs ->
            val max = prefs[Keys.MaxAllowedPercent] ?: 100f
            prefs[Keys.MinAllowedPercent] = percent.coerceIn(1f, max.coerceAtLeast(1f))
        }
    }

    suspend fun setMaxAllowedPercent(percent: Float) {
        context.brightnessDataStore.edit { prefs ->
            val min = prefs[Keys.MinAllowedPercent] ?: 3f
            prefs[Keys.MaxAllowedPercent] = percent.coerceIn(min.coerceAtMost(100f), 100f)
        }
    }

    suspend fun setResponseSpeed(speed: ResponseSpeed) {
        context.brightnessDataStore.edit { it[Keys.ResponseSpeed] = speed.name }
    }

    suspend fun saveOriginalSettings(mode: Int, brightness: Int) {
        context.brightnessDataStore.edit {
            it[Keys.OriginalMode] = mode
            it[Keys.OriginalBrightness] = brightness
        }
    }

    suspend fun originalSettings(): OriginalBrightnessSettings? {
        val prefs = context.brightnessDataStore.data.first()
        val mode = prefs[Keys.OriginalMode] ?: return null
        val brightness = prefs[Keys.OriginalBrightness] ?: return null
        return OriginalBrightnessSettings(mode, brightness)
    }

    suspend fun clearOriginalSettings() {
        context.brightnessDataStore.edit {
            it.remove(Keys.OriginalMode)
            it.remove(Keys.OriginalBrightness)
        }
    }
}
