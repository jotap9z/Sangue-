package com.sangue.sangue.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Definindo o nosso esquema de cores
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFE21C2C),
    primaryContainer = Color(0xFFFEF2F2),
    onPrimaryContainer = Color(0xFFE21C2C),
    secondary = TextDark,
    background = BackgroundGray,
    surface = White,
    onPrimary = White,
    onBackground = TextDark,
    onSurface = TextDark,
    onSurfaceVariant = TextGray,
    surfaceVariant = BackgroundGray,
    surfaceTint = White,
    outline = Color(0xFFE2E8F0),
    outlineVariant = Color(0xFFF1F5F9),
    error = Color(0xFFB91C1C),
    errorContainer = Color(0xFFFEF2F2),
    onErrorContainer = Color(0xFFB91C1C)
)

@Composable
fun SangueMaisTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme // Forçando o tema claro por enquanto para focar no design
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(20.dp),
            extraLarge = RoundedCornerShape(24.dp)
        ),
        content = content
    )
}