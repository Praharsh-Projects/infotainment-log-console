package com.praharsh.infotainmentlogconsole.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NordicScheme = darkColorScheme(
    primary = Color(0xFF3ED39F),
    secondary = Color(0xFF7DD7BA),
    tertiary = Color(0xFF8AE6C7),
    background = Color(0xFF08111F),
    surface = Color(0xFF101B2F),
    surfaceVariant = Color(0xFF162338),
    onPrimary = Color(0xFF08111F)
)

private val UrbanScheme = darkColorScheme(
    primary = Color(0xFF7AA5FF),
    secondary = Color(0xFFA9C0FF),
    tertiary = Color(0xFFC0D0FF),
    background = Color(0xFF08111F),
    surface = Color(0xFF101B2F),
    surfaceVariant = Color(0xFF162338),
    onPrimary = Color(0xFF08111F)
)

@Composable
fun LogConsoleTheme(activeBrandKey: String, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (activeBrandKey == "urban") UrbanScheme else NordicScheme,
        content = content
    )
}
