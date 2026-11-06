package com.ghorerkhobor.user.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val Colors = lightColorScheme(
    primary = TealPrimary,
    secondary = OrangeAccent,
    background = BgLight
)

@Composable
fun GhorerKhoborTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}
