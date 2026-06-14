package com.edgequest.hero.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val EdgeQuestDarkColorScheme = darkColorScheme(
    primary = Emerald,
    secondary = Gold,
    tertiary = Coral,
    background = DarkNavy,
    surface = DarkNavyCard,
    onPrimary = DarkNavy,
    onSecondary = DarkNavy,
    onTertiary = OffWhite,
    onBackground = OffWhite,
    onSurface = OffWhite,
    surfaceVariant = BorderLine,
    onSurfaceVariant = SubText,
    outline = BorderLine
)

@Composable
fun EdgeQuestTheme(content: @Composable () -> Unit) {
    val colorScheme = EdgeQuestDarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DarkNavy.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}