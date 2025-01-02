package com.devbrackets.android.exomediademo.ui.support.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
  primary = Color(0xFF42A5F5),
  secondary = Color(0xFF607D8B),
  background = Color(0xFFEBEBF5),
  surface = Color(0xFFEBEBF5)
)

private val DarkColorScheme = darkColorScheme(
  primary = Color(0xFF42A5F5),
  secondary = Color(0xFF607D8B),
  background = Color(0xFF2A2A2B),
  surface = Color(0xFF2A2A2B)
)

internal val LocalColorScheme = staticCompositionLocalOf { DarkColorScheme }

@Composable
@ReadOnlyComposable
internal fun getColorScheme(
  darkTheme: Boolean = isSystemInDarkTheme()
): ColorScheme {
  return when (darkTheme) {
    true -> DarkColorScheme
    false -> LightColorScheme
  }
}
