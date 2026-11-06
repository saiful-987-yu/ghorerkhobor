package com.ghorerkhobor.admin.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = TealPrimary,
    secondary = OrangeAccent,
    background = BgLight
)

private val DarkColors = darkColorScheme(
    primary = OrangeAccent,
    secondary = TealPrimary,
    background = BgDark,
    surface = SurfaceDark
)

@Composable
fun GhorerKhoborAdminTheme(themeMode: String, content: @Composable () -> Unit) {
    val useDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(colorScheme = if (useDark) DarkColors else LightColors, content = content)
}
