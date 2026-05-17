package com.purawale.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AccentGold,
    secondary = SoftBrown,
    tertiary = Pink80,
    background = DarkBrown,
    surface = SurfaceBrown,
    onPrimary = DarkBrown,
    onSecondary = Sand,
    onTertiary = Sand,
    onBackground = Sand,
    onSurface = Sand,
)

private val LightColorScheme = lightColorScheme(
    primary = DeepBrown,
    secondary = SoftBrown,
    tertiary = AccentGold,
    background = DeepBrown,
    surface = SurfaceBrown,
    onPrimary = Sand,
    onSecondary = Sand,
    onTertiary = DarkBrown,
    onBackground = Sand,
    onSurface = Sand,
)

@Composable
fun CircleBirthdaysTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is disabled for a custom "lively" look
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
