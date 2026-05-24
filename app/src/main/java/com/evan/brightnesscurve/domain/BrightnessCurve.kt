package com.evan.brightnesscurve.domain

import com.evan.brightnesscurve.brightness.BrightnessMapping
import com.evan.brightnesscurve.data.BrightnessPoint

object BrightnessCurve {
    fun validate(points: List<BrightnessPoint>) = BrightnessMapping.validate(points)

    fun sortedValid(points: List<BrightnessPoint>): List<BrightnessPoint> =
        BrightnessMapping.sortedValid(points)

    fun targetPercent(
        lux: Float,
        points: List<BrightnessPoint>,
        allowOutdoorFull: Boolean
    ): Float {
        val max = if (allowOutdoorFull) 100f else 85f
        return BrightnessMapping.targetPercent(lux, points, minPercent = 1f, maxPercent = max)
    }
}
