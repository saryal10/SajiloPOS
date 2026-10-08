package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PosTealLight,
    onPrimary = Color.Black,
    primaryContainer = PosTealDark,
    onPrimaryContainer = Color.White,
    secondary = PosAmberAccent,
    onSecondary = Color.Black,
    secondaryContainer = PosAmberDark,
    onSecondaryContainer = Color.White,
    background = PosSlate900,
    surface = PosSlate800,
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = PosSlate700,
    onSurfaceVariant = Color(0xFFCBD5E1),
    error = Color(0xFFEF4444),
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = PosTealPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2F1),
    onPrimaryContainer = PosTealDark,
    secondary = PosAmberDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFF8E1),
    onSecondaryContainer = PosSlate900,
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    onBackground = PosSlate900,
    onSurface = PosSlate900,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = PosSlate700,
    error = Color(0xFFDC2626),
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep high-contrast branded POS palette
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
        typography = Typography,
        content = content
    )
}
