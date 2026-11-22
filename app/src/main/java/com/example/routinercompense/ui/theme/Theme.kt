package com.example.routinercompense.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppThemePalette(val displayName: String, val primaryColor: Color) {
    OCEAN("Bleu Océan 🌊", Color(0xFF1976D2)),
    NATURE("Vert Nature 🌿", Color(0xFF2E7D32)),
    MAGIC("Violet Magique 🔮", Color(0xFF7B1FA2)),
    SUNSET("Orange Soleil ☀️", Color(0xFFE65100)),
    PINK("Rose Bonbon 💖", Color(0xFFD81B60))
}

enum class DarkThemeOption(val displayName: String) {
    LIGHT("Clair ☀️"),
    DARK("Sombre 🌙"),
    SYSTEM("Automatique 📱")
}

val StarGold = Color(0xFFFFB300)

fun getLightColorScheme(palette: AppThemePalette): ColorScheme {
    return when (palette) {
        AppThemePalette.OCEAN -> lightColorScheme(
            primary = Color(0xFF1976D2),
            primaryContainer = Color(0xFFBBDEFB),
            onPrimaryContainer = Color(0xFF0D47A1),
            secondary = StarGold,
            background = Color(0xFFF7F9FC),
            surface = Color.White
        )
        AppThemePalette.NATURE -> lightColorScheme(
            primary = Color(0xFF2E7D32),
            primaryContainer = Color(0xFFC8E6C9),
            onPrimaryContainer = Color(0xFF1B5E20),
            secondary = Color(0xFFFFA000),
            background = Color(0xFFF1F8E9),
            surface = Color.White
        )
        AppThemePalette.MAGIC -> lightColorScheme(
            primary = Color(0xFF7B1FA2),
            primaryContainer = Color(0xFFE1BEE7),
            onPrimaryContainer = Color(0xFF4A148C),
            secondary = Color(0xFFFF4081),
            background = Color(0xFFF3E5F5),
            surface = Color.White
        )
        AppThemePalette.SUNSET -> lightColorScheme(
            primary = Color(0xFFE65100),
            primaryContainer = Color(0xFFFFE0B2),
            onPrimaryContainer = Color(0xFFBF360C),
            secondary = Color(0xFFFFC107),
            background = Color(0xFFFFF3E0),
            surface = Color.White
        )
        AppThemePalette.PINK -> lightColorScheme(
            primary = Color(0xFFD81B60),
            primaryContainer = Color(0xFFF8BBD0),
            onPrimaryContainer = Color(0xFF880E4F),
            secondary = Color(0xFFAB47BC),
            background = Color(0xFFFCE4EC),
            surface = Color.White
        )
    }
}

fun getDarkColorScheme(palette: AppThemePalette): ColorScheme {
    val primaryLight = palette.primaryColor
    return darkColorScheme(
        primary = primaryLight,
        primaryContainer = Color(0xFF2C2C2C),
        secondary = StarGold,
        background = Color(0xFF121212),
        surface = Color(0xFF1E1E1E),
        onSurface = Color.White
    )
}

@Composable
fun RoutineTheme(
    palette: AppThemePalette = AppThemePalette.OCEAN,
    themeOption: DarkThemeOption = DarkThemeOption.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (themeOption) {
        DarkThemeOption.LIGHT -> false
        DarkThemeOption.DARK -> true
        DarkThemeOption.SYSTEM -> isSystemInDarkTheme()
    }

    val colors = if (isDark) getDarkColorScheme(palette) else getLightColorScheme(palette)

    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
