package com.devbrackets.android.exomediademo.ui.support.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember

object DemoTheme {
  val colors: ColorScheme
    @Composable
    @ReadOnlyComposable
    get() = LocalColorScheme.current

  val typography: Typography
    @Composable
    @ReadOnlyComposable
    get() = LocalTypography.current

  val shapes: Shapes
    @Composable
    @ReadOnlyComposable
    get() = LocalShapes.current
}

@Composable
fun DemoTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit
) {
  val colorScheme = getColorScheme(darkTheme = darkTheme)
  val typography = Typography
  val shapes = remember { Shapes() }

  CompositionLocalProvider(
    LocalColorScheme provides colorScheme,
    LocalTypography provides typography,
    LocalShapes provides shapes
  ) {
    MaterialTheme(
      colorScheme = LocalColorScheme.current,
      typography = LocalTypography.current,
      shapes = LocalShapes.current,
      content = content
    )
  }
}