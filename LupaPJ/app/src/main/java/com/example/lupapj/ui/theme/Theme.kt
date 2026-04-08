package com.example.lupapj.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = MintGlow,
    secondary = Honey,
    tertiary = Peach,
    background = NightMoss,
    surface = NightPaper,
    surfaceVariant = Walnut,
    onPrimary = NightMoss,
    onSecondary = NightMoss,
    onBackground = Cream,
    onSurface = Cream,
    onSurfaceVariant = Cream
)

private val LightColorScheme = lightColorScheme(
    primary = Moss,
    secondary = Honey,
    tertiary = Peach,
    background = Cream,
    surface = Color.White,
    surfaceVariant = WarmPaper,
    primaryContainer = SkyTint,
    secondaryContainer = Color(0xFFFFEAB0),
    tertiaryContainer = Color(0xFFFFD7BE),
    onPrimary = Color.White,
    onSecondary = Walnut,
    onTertiary = Walnut,
    onBackground = Walnut,
    onSurface = Walnut,
    onSurfaceVariant = WalnutSoft
)

@Composable
fun LupaPJTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
