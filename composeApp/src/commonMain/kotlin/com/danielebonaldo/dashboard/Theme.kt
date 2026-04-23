package com.danielebonaldo.dashboard

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFBCC5FF),
    secondary = Color(0xFFC2C8DC),
    tertiary = Color(0xFFEFB8C8),
    background = Color(0xFF000000),
    /* Other default colors to override
  background = Color(0xFFFFFBFE),
  surface = Color(0xFFFFFBFE),
  onPrimary = Color.White,
  onSecondary = Color.White,
  onTertiary = Color.White,
  onBackground = Color(0xFF1C1B1F),
  onSurface = Color(0xFF1C1B1F),
  */
)

@Composable
fun AutomotiveDashboardTheme(
    content: @Composable () -> Unit
) {

    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
