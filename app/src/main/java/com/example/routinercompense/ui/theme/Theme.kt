package com.example.routinercompense.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val SoftYellow = Color(0xFFFFE082)
val StarGold = Color(0xFFFFB300)
val PastelBlue = Color(0xFFE3F2FD)
val OceanBlue = Color(0xFF1976D2)
val MintGreen = Color(0xFFE8F5E9)
val DarkGreen = Color(0xFF2E7D32)
val WarmCoral = Color(0xFFFFE0B2)
val DarkOrange = Color(0xFFE65100)
val CardBackground = Color(0xFFFFFFFF)

private val LightColorScheme = lightColorScheme(
    primary = OceanBlue,
    onPrimary = Color.White,
    secondary = StarGold,
    onSecondary = Color.Black,
    tertiary = DarkOrange,
    background = Color(0xFFF7F9FC),
    surface = CardBackground,
    onSurface = Color(0xFF212121)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color.Black,
    secondary = StarGold,
    onSecondary = Color.Black,
    tertiary = Color(0xFFFFB74D),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onSurface = Color.White
)

@Composable
fun RoutineTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
