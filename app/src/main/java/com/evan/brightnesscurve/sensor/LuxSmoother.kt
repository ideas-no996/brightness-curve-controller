package com.evan.brightnesscurve.sensor

import kotlin.math.abs

class LuxSmoother(
    private val baseAlpha: Float,
    private val fastAlpha: Float = 0.65f,
    private val sharpChangeRatio: Float = 0.6f,
    private val sharpChangeLux: Float = 80f
) {
    private var smoothedLux: Float? = null

    fun onSample(rawLux: Float): Float {
        val previous = smoothedLux
        val next = if (previous == null) {
            rawLux
        } else {
            val alpha = if (isSharpChange(previous, rawLux)) fastAlpha else baseAlpha
            previous + (rawLux - previous) * alpha.coerceIn(0.05f, 0.95f)
        }
        smoothedLux = next
        return next
    }

    fun reset() {
        smoothedLux = null
    }

    private fun isSharpChange(previous: Float, rawLux: Float): Boolean {
        val delta = abs(rawLux - previous)
        val ratio = delta / previous.coerceAtLeast(1f)
        return delta >= sharpChangeLux || ratio >= sharpChangeRatio
    }
}
