package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = VividTeal,
    onPrimary = Color.White,
    primaryContainer = BrandGreenTint,
    onPrimaryContainer = BrandGreenDeep,
    inversePrimary = BrandGreenSoft,

    secondary = AccentAmberDeep,
    onSecondary = Color.White,
    secondaryContainer = AccentAmberTint,
    onSecondaryContainer = Color(0xFF6B4408),

    tertiary = InfoBlue,
    onTertiary = Color.White,
    tertiaryContainer = InfoTint,
    onTertiaryContainer = Color(0xFF1B3F7A),

    background = PaperMist,
    onBackground = Ink,
    surface = CardWhite,
    onSurface = Ink,
    surfaceVariant = PaperDepth,
    onSurfaceVariant = InkMuted,
    surfaceTint = BrandGreen,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFBFBFD),
    surfaceContainer = Color(0xFFF4F4F6),
    surfaceContainerHigh = Color(0xFFEFEFF2),
    surfaceContainerHighest = PaperDepth,
    inverseSurface = NightSurface,
    inverseOnSurface = NightInk,

    outline = LineStrongMist,
    outlineVariant = LineMist,
    error = DangerRed,
    onError = Color.White,
    errorContainer = DangerTint,
    onErrorContainer = Color(0xFF7A231D),
    scrim = Color(0x66101B22)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF5CC0A5),
    onPrimary = Color(0xFF00382C),
    primaryContainer = Color(0xFF0C5344),
    onPrimaryContainer = Color(0xFFB8EADB),
    inversePrimary = BrandGreen,

    secondary = Color(0xFFF0B45F),
    onSecondary = Color(0xFF3D2400),
    secondaryContainer = Color(0xFF5A3A08),
    onSecondaryContainer = Color(0xFFFFDDB3),

    tertiary = Color(0xFF9FC1FF),
    onTertiary = Color(0xFF002E69),
    tertiaryContainer = Color(0xFF1F4679),
    onTertiaryContainer = Color(0xFFD7E3FF),

    background = NightPaper,
    onBackground = NightInk,
    surface = NightSurface,
    onSurface = NightInk,
    surfaceVariant = NightSurfaceAlt,
    onSurfaceVariant = NightInkMuted,
    surfaceTint = Color(0xFF5CC0A5),
    surfaceContainerLowest = Color(0xFF081014),
    surfaceContainerLow = Color(0xFF111A1F),
    surfaceContainer = Color(0xFF162127),
    surfaceContainerHigh = Color(0xFF1F2C33),
    surfaceContainerHighest = Color(0xFF27363E),
    inverseSurface = Paper,
    inverseOnSurface = Ink,

    outline = Color(0xFF3A4B54),
    outlineVariant = NightLine,
    error = Color(0xFFF08A82),
    onError = Color(0xFF56150F),
    errorContainer = Color(0xFF7A231D),
    onErrorContainer = Color(0xFFFFDAD6),
    scrim = Color(0x99000000)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // keep the branded POS identity
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(
            displayLarge = PosType.displayLarge,
            displayMedium = PosType.displayMedium,
            displaySmall = PosType.displaySmall,
            headlineLarge = PosType.headlineLarge,
            headlineMedium = PosType.headlineMedium,
            headlineSmall = PosType.headlineSmall,
            titleLarge = PosType.titleLarge,
            titleMedium = PosType.titleMedium,
            titleSmall = PosType.titleSmall,
            bodyLarge = PosType.bodyLarge,
            bodyMedium = PosType.bodyMedium,
            bodySmall = PosType.bodySmall,
            labelLarge = PosType.labelLarge,
            labelMedium = PosType.labelMedium,
            labelSmall = PosType.labelSmall
        ),
        shapes = PosShapes,
        content = content
    )
}