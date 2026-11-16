package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = Emerald900,
    onPrimary = Color.White,
    primaryContainer = Emerald100,
    onPrimaryContainer = Emerald950,
    secondary = Amber600,
    onSecondary = Color.White,
    secondaryContainer = Amber100,
    onSecondaryContainer = Amber700,
    tertiary = Teal700,
    onTertiary = Color.White,
    tertiaryContainer = Teal100,
    onTertiaryContainer = Emerald950,
    background = CanvasLight,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,
    outline = Slate200,
    outlineVariant = Slate200,
    error = StatusDanger,
    onError = Color.White,
    errorContainer = StatusDangerBg,
    onErrorContainer = StatusDanger
)

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldMint,
    onPrimary = Emerald950,
    primaryContainer = Emerald800,
    onPrimaryContainer = Emerald50,
    secondary = AmberBright,
    onSecondary = Slate900,
    secondaryContainer = Amber700,
    onSecondaryContainer = Amber50,
    tertiary = Teal100,
    onTertiary = Emerald950,
    background = DarkCanvas,
    onBackground = Color(0xFFE6F2ED),
    surface = DarkSurface,
    onSurface = Color(0xFFE6F2ED),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFF9EB5AD),
    outline = DarkBorder,
    outlineVariant = DarkBorder,
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF450A0A)
)

val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
