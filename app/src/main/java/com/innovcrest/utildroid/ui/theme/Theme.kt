package com.innovcrest.utildroid.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object Brand {
    /** Fill colour for primary buttons and progress. */
    val Accent = Color(0xFF3DDC84)
    /** Text and icons placed on [Accent]. */
    val OnAccent = Color(0xFF08130D)
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF12804A),            // green text and icons on white
    onPrimary = Color.White,
    primaryContainer = Brand.Accent,
    onPrimaryContainer = Brand.OnAccent,
    secondaryContainer = Color(0xFFE6F7EC), // soft green banners and progress track
    onSecondaryContainer = Color(0xFF14361F),
    background = Color(0xFFF6F8F7),
    onBackground = Color(0xFF111814),
    surface = Color.White,
    onSurface = Color(0xFF111814),
    surfaceVariant = Color(0xFFEEF2F0),
    onSurfaceVariant = Color(0xFF5B6862),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF1F4F2),
    outline = Color(0xFFC3CDC8),
    outlineVariant = Color(0xFFE1E7E4),
    error = Color(0xFFC62828),
)

private val DarkColors = darkColorScheme(
    primary = Brand.Accent,
    onPrimary = Brand.OnAccent,
    primaryContainer = Brand.Accent,
    onPrimaryContainer = Brand.OnAccent,
    secondaryContainer = Color(0xFF14261C),
    onSecondaryContainer = Color(0xFFA8C9B5),
    background = Color(0xFF0B0F0D),
    onBackground = Color(0xFFE8EEEA),
    surface = Color(0xFF141A17),
    onSurface = Color(0xFFE8EEEA),
    surfaceVariant = Color(0xFF1A2420),
    onSurfaceVariant = Color(0xFF9AA8A0),
    surfaceContainer = Color(0xFF0F1412),
    surfaceContainerHigh = Color(0xFF1A2420),
    outline = Color(0xFF3A4A41),
    outlineVariant = Color(0xFF222B26),
    error = Color(0xFFFF9592),
)

@Composable
fun UtilDroidTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
