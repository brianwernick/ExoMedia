package com.devbrackets.android.exomediademo.ui.support

import android.view.View
import android.view.Window
import android.view.WindowInsets
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class FullscreenManager(
  private val window: Window,
  private val onUiVisible: () -> Unit
) : View.OnApplyWindowInsetsListener {
  private val navBarType = WindowInsetsCompat.Type.navigationBars()
  private val systemBarType = WindowInsetsCompat.Type.systemBars()

  private val insetsController by lazy {
    WindowInsetsControllerCompat(window, window.decorView).apply {
      systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_TOUCH
    }
  }

  init {
    window.decorView.setOnApplyWindowInsetsListener(this)
  }

  fun enterFullscreen() {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    insetsController.hide(systemBarType)

  }

  fun exitFullscreen() {
    WindowCompat.setDecorFitsSystemWindows(window, true)
    insetsController.show(systemBarType)
  }

  override fun onApplyWindowInsets(v: View, insets: WindowInsets): WindowInsets {
    val insetsCompat = WindowInsetsCompat.toWindowInsetsCompat(insets, v)
    if (insetsCompat.isVisible(navBarType)) {
      onUiVisible()
    }

    return insets
  }
}