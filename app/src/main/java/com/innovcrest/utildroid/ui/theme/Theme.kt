package com.innovcrest.utildroid.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

/** The two in-between weights the design leans on (Roboto Flex 650 / 750). */
object Type {
    /** Row titles, buttons, section headers. */
    val Emphasis = FontWeight(650)
    /** Screen titles and big numbers. */
    val Display = FontWeight(750)
}

// Roles as the screens use them:
//   background             page ground
//   surface                cards and list rows
//   surfaceContainerHigh   nav bar, neutral buttons, info chips
//   surfaceContainerHighest screenshot placeholders
//   primaryContainer       nav indicator, updates summary
//   secondaryContainer     tonal buttons, selected chip, progress track
//   tertiaryContainer      store banners
private val LightColors = lightColorScheme(
    primary = Color(0xFF146C2E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7F2C3),
    onPrimaryContainer = Color(0xFF002110),
    secondary = Color(0xFF506352),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD2E8D3),
    onSecondaryContainer = Color(0xFF0D1F12),
    tertiary = Color(0xFF00696B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFBCEBEE),
    onTertiaryContainer = Color(0xFF002022),
    background = Color(0xFFF0F5ED),
    onBackground = Color(0xFF171D18),
    surface = Color.White,
    onSurface = Color(0xFF171D18),
    surfaceVariant = Color(0xFFDFE4DC),
    onSurfaceVariant = Color(0xFF414941),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF0F5ED),
    surfaceContainer = Color(0xFFE4EAE2),
    surfaceContainerHigh = Color(0xFFE4EAE2),
    surfaceContainerHighest = Color(0xFFDCE7DB),
    outline = Color(0xFF717970),
    outlineVariant = Color(0xFFC1C9BF),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF88D996),
    onPrimary = Color(0xFF00390F),
    primaryContainer = Color(0xFF005321),
    onPrimaryContainer = Color(0xFFB7F2C3),
    secondary = Color(0xFFB7CCB8),
    onSecondary = Color(0xFF233426),
    secondaryContainer = Color(0xFF38493A),
    onSecondaryContainer = Color(0xFFD2E8D3),
    tertiary = Color(0xFF80D4D6),
    onTertiary = Color(0xFF003738),
    tertiaryContainer = Color(0xFF004F51),
    onTertiaryContainer = Color(0xFFBCEBEE),
    background = Color(0xFF101510),
    onBackground = Color(0xFFDFE4DC),
    surface = Color(0xFF1C211C),
    onSurface = Color(0xFFDFE4DC),
    surfaceVariant = Color(0xFF313631),
    onSurfaceVariant = Color(0xFFC1C9BF),
    surfaceContainerLowest = Color(0xFF0B0F0B),
    surfaceContainerLow = Color(0xFF101510),
    surfaceContainer = Color(0xFF262B26),
    surfaceContainerHigh = Color(0xFF262B26),
    surfaceContainerHighest = Color(0xFF313631),
    outline = Color(0xFF8B938A),
    outlineVariant = Color(0xFF414941),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

@Composable
fun UtilDroidTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
