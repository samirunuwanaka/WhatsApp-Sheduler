package com.statusflow.scheduler.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val ForestNight = Color(0xFF0B1F1A)
val DeepCanopy = Color(0xFF14352C)
val Leaf = Color(0xFF1FA97A)
val SoftMoss = Color(0xFF7BC9A6)
val Mist = Color(0xFFE8F5EF)
val WarmSand = Color(0xFFF3E6D0)
val Ember = Color(0xFFE07A5F)
val Fog = Color(0xFF9BB5AB)

private val DarkColors = darkColorScheme(
    primary = Leaf,
    onPrimary = ForestNight,
    secondary = SoftMoss,
    onSecondary = ForestNight,
    tertiary = WarmSand,
    background = ForestNight,
    onBackground = Mist,
    surface = DeepCanopy,
    onSurface = Mist,
    surfaceVariant = Color(0xFF1B4338),
    onSurfaceVariant = Fog,
    error = Ember,
    onError = Mist
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0F6E52),
    onPrimary = Color.White,
    secondary = Color(0xFF3D8B6E),
    onSecondary = Color.White,
    tertiary = Ember,
    background = Mist,
    onBackground = ForestNight,
    surface = Color.White,
    onSurface = ForestNight,
    surfaceVariant = Color(0xFFD7EBE2),
    onSurfaceVariant = Color(0xFF3A5A4E),
    error = Ember,
    onError = Color.White
)

private val DisplayFont = FontFamily.Serif
private val BodyFont = FontFamily.SansSerif

@Composable
fun StatusFlowTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme || isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = androidx.compose.material3.Typography(
            displayLarge = TextStyle(
                fontFamily = DisplayFont,
                fontWeight = FontWeight.Bold,
                fontSize = 42.sp,
                lineHeight = 46.sp
            ),
            headlineMedium = TextStyle(
                fontFamily = DisplayFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 28.sp
            ),
            titleLarge = TextStyle(
                fontFamily = DisplayFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp
            ),
            titleMedium = TextStyle(
                fontFamily = BodyFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            ),
            bodyLarge = TextStyle(
                fontFamily = BodyFont,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp
            ),
            bodyMedium = TextStyle(
                fontFamily = BodyFont,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp
            ),
            labelLarge = TextStyle(
                fontFamily = BodyFont,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
        ),
        content = content
    )
}
