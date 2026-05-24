package com.evan.brightnesscurve.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object PresetCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    fun encodePoints(points: List<BrightnessPoint>): String = json.encodeToString(points)

    fun decodePoints(value: String): List<BrightnessPoint> = json.decodeFromString(value)

    fun encodeSnapshot(snapshot: BrightnessPresetSnapshot): String = json.encodeToString(snapshot)

    fun decodeSnapshot(value: String): BrightnessPresetSnapshot = json.decodeFromString(value)
}
