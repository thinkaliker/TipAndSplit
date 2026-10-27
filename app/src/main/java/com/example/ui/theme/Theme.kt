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

private val DarkColorScheme =
    darkColorScheme(
        primary = DarkPrimary,
        onPrimary = Color(0xFF003822),
        primaryContainer = ForestSecondary,
        onPrimaryContainer = Color(0xFFB7F397),
        secondary = DarkAccent,
        onSecondary = Color(0xFF4C1F00),
        tertiary = AmberHighlight,
        onTertiary = Color(0xFF422C00),
        background = DarkBackground,
        onBackground = DarkText,
        surface = DarkSurface,
        onSurface = DarkText,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = DarkMutedText,
        outline = Color(0xFF3B4A40),
        outlineVariant = Color(0xFF2B3630),
    )

private val LightColorScheme =
    lightColorScheme(
        primary = ForestPrimary,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD8F3DC),
        onPrimaryContainer = ForestPrimary,
        secondary = TerracottaAccent,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFFFDBC8),
        onSecondaryContainer = Color(0xFF381500),
        tertiary = ForestTertiary,
        onTertiary = Color.White,
        background = WarmPaperBackground,
        onBackground = DarkInk,
        surface = PureCardWhite,
        onSurface = DarkInk,
        surfaceVariant = Color(0xFFF0EFEB),
        onSurfaceVariant = MutedInk,
        outline = SubtleBorder,
        outlineVariant = Color(0xFFEBE8E1),
    )

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our handcrafted rich bistro theme by default
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
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
