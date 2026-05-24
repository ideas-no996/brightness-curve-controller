package com.evan.brightnesscurve.brightness

import kotlin.math.abs

data class BrightnessDecision(
    val rawLux: Float,
    val smoothedLux: Float,
    val mappedPercent: Float,
    val targetPercent: Float,
    val shouldWrite: Boolean
)

class BrightnessRamp(
    private val brightenStepPercent: Float,
    private val darkenStepPercent: Float,
    private val minPercentDelta: Float = 1f,
    private val minSystemDelta: Int = 2
) {
    fun next(currentPercent: Float?, mappedPercent: Float): Pair<Float, Boolean> {
        val previous = currentPercent ?: return mappedPercent to true
        val delta = mappedPercent - previous
        val step = if (delta > 0f) brightenStepPercent else darkenStepPercent
        val next = previous + delta.coerceIn(-step, step)
        val systemDelta = abs(
            BrightnessController.percentToSystemValue(next) -
                BrightnessController.percentToSystemValue(previous)
        )
        val shouldWrite = abs(next - previous) >= minPercentDelta && systemDelta >= minSystemDelta
        return next to shouldWrite
    }
}
