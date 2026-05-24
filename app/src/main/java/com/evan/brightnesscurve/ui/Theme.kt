package com.evan.brightnesscurve.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ColorScheme = lightColorScheme(
    primary = Color(0xFF4E7F7A),
    secondary = Color(0xFF7B8A72),
    tertiary = Color(0xFF8A7A58),
    background = Color(0xFFF7F7F2),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onBackground = Color(0xFF253233),
    onSurface = Color(0xFF253233)
)

@Composable
fun BrightnessAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
