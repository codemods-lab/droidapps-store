package com.innovcrest.utildroid.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class CategoryStyle(val container: Color, val content: Color, val icon: ImageVector)

/** "Coming soon" purple, shared by pills and the Games tile. */
internal val SoonContainer = Color(0xFFEADDFF)
internal val SoonContent = Color(0xFF21005D)

/** Fixed pastel + icon per category; the pastels are used in both light and dark themes. */
fun categoryStyle(category: String): CategoryStyle = when (category.lowercase()) {
    "tools" -> CategoryStyle(Color(0xFFD2E8D3), Color(0xFF0D1F12), Icons.Rounded.Build)
    "media" -> CategoryStyle(Color(0xFFFFDAD5), Color(0xFF410002), Icons.Rounded.PlayArrow)
    "finance" -> CategoryStyle(Color(0xFFD6E3FF), Color(0xFF001B3E), Icons.AutoMirrored.Rounded.ShowChart)
    "personalisation", "personalization", "personalise" ->
        CategoryStyle(Color(0xFFFFDF8E), Color(0xFF261A00), Icons.Rounded.Widgets)
    "games" -> CategoryStyle(SoonContainer, SoonContent, Icons.Rounded.Casino)
    else -> CategoryStyle(Color(0xFFBCEBEE), Color(0xFF002022), Icons.Rounded.Category)
}
