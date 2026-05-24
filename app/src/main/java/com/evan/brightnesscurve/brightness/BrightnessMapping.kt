package com.evan.brightnesscurve.brightness

import com.evan.brightnesscurve.data.BrightnessPoint
import kotlin.math.log10

object BrightnessMapping {
    fun validate(points: List<BrightnessPoint>) {
        require(points.size >= 3) { "至少需要 3 个控制点" }
        points.forEach { point ->
            require(point.lux >= 0f) { "lux 不能小于 0" }
            require(point.brightnessPercent in 1f..100f) { "亮度必须在 1% 到 100% 之间" }
        }
        points.zipWithNext().forEach { (left, right) ->
            require(right.lux > left.lux) { "lux 控制点必须严格递增" }
        }
    }

    fun sortedValid(points: List<BrightnessPoint>): List<BrightnessPoint> =
        points.sortedBy { it.lux }.also(::validate)

    fun targetPercent(
        lux: Float,
        points: List<BrightnessPoint>,
        minPercent: Float,
        maxPercent: Float
    ): Float {
        val sorted = sortedValid(points)
        val safeLux = lux.coerceAtLeast(0f)
        val raw = when {
            safeLux <= sorted.first().lux -> sorted.first().brightnessPercent
            safeLux >= sorted.last().lux -> sorted.last().brightnessPercent
            else -> interpolate(safeLux, sorted)
        }
        val min = minPercent.coerceIn(1f, 100f)
        val max = maxPercent.coerceIn(min, 100f)
        return raw.coerceIn(min, max)
    }

    private fun interpolate(lux: Float, points: List<BrightnessPoint>): Float {
        val index = points.indexOfFirst { it.lux >= lux }
        val right = points[index]
        val left = points[index - 1]
        val leftLux = logLux(left.lux)
        val rightLux = logLux(right.lux)
        val currentLux = logLux(lux)
        val span = (rightLux - leftLux).takeIf { it > 0f } ?: return right.brightnessPercent
        val ratio = ((currentLux - leftLux) / span).coerceIn(0f, 1f)
        return left.brightnessPercent +
            (right.brightnessPercent - left.brightnessPercent) * ratio
    }

    fun logLux(lux: Float): Float = log10(lux.coerceAtLeast(0f) + 1f)
}
