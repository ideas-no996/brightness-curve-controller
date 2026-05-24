package com.evan.brightnesscurve.domain

data class BrightnessDecision(
    val rawLux: Float,
    val smoothedLux: Float,
    val targetPercent: Float,
    val shouldWrite: Boolean
)

class BrightnessSmoother(
    private val smoothingLevel: Float,
    private val maxChangePerUpdate: Float,
    private val minUpdateDelta: Float
) {
    private var smoothedLux: Float? = null
    private var lastWrittenPercent: Float? = null

    fun onSample(rawLux: Float, targetForLux: (Float) -> Float): BrightnessDecision {
        val alpha = smoothingLevel.coerceIn(0.05f, 0.95f)
        val nextSmoothedLux = smoothedLux?.let { previous ->
            previous + (rawLux - previous) * alpha
        } ?: rawLux

        smoothedLux = nextSmoothedLux
        val rawTargetPercent = targetForLux(nextSmoothedLux)

        val previousPercent = lastWrittenPercent
        val limitedTarget = if (previousPercent == null) {
            rawTargetPercent
        } else {
            val delta = rawTargetPercent - previousPercent
            previousPercent + delta.coerceIn(-maxChangePerUpdate, maxChangePerUpdate)
        }.coerceIn(1f, 100f)

        val shouldWrite = previousPercent == null ||
            kotlin.math.abs(limitedTarget - previousPercent) >= minUpdateDelta

        if (shouldWrite) {
            lastWrittenPercent = limitedTarget
        }

        return BrightnessDecision(
            rawLux = rawLux,
            smoothedLux = nextSmoothedLux,
            targetPercent = limitedTarget,
            shouldWrite = shouldWrite
        )
    }

    fun reset(lastPercent: Float? = null) {
        smoothedLux = null
        lastWrittenPercent = lastPercent
    }
}
