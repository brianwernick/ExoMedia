package com.devbrackets.android.exomediademo.ui.support

import android.view.View
import android.view.Window
import androidx.core.view.*

class FullscreenManager(
    private val window: Window,
    private val onUiVisible: () -> Unit
) {
  private val insetType = WindowInsetsCompat.Type.systemBars()
  private val insetsController by lazy {
    WindowInsetsControllerCompat(window, window.decorView).apply {
      systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_TOUCH
    }
  }

  init {
    window.decorView.setOnSystemUiVisibilityChangeListener {
      if (it and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
        onUiVisible()
      }
    }
  }

  fun enterFullscreen() {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    insetsController.hide(insetType)

  }

  fun exitFullscreen() {
    WindowCompat.setDecorFitsSystemWindows(window, true)
    insetsController.show(insetType)
  }
}