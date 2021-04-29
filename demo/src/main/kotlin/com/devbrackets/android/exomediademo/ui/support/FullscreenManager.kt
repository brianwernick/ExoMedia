package com.devbrackets.android.exomediademo.ui.support

import android.view.View
import android.view.Window

class FullscreenManager(
    private val window: Window,
    private val onUiVisible: () -> Unit
): View.OnSystemUiVisibilityChangeListener {
  /**
   * Determines the appropriate fullscreen flags based on the
   * systems API version.
   *
   * @return The appropriate decor view flags to enter fullscreen mode when supported
   */
  private val fullscreenUiFlags: Int
    get() = (View.SYSTEM_UI_FLAG_LOW_PROFILE
        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        or View.SYSTEM_UI_FLAG_FULLSCREEN
        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)

  private val stableUiFlags: Int
    get() = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)

  private var lastVisibility = 0
  private var initialUiFlags = 0

  override fun onSystemUiVisibilityChange(visibility: Int) {
    // NOTE: if the screen is double tapped in just the right way (or wrong way)
    // the SYSTEM_UI_FLAG_HIDE_NAVIGATION flag is dropped. Because of this we
    // no longer get notified of the temporary change when the screen is tapped
    // (i.e. the VideoControls get the touch event instead of the OS). So we store
    // the visibility off for use in the ControlsVisibilityListener for verification
    lastVisibility = visibility
    if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
      onUiVisible()
    }
  }

  /**
   * Correctly sets up the fullscreen flags to avoid popping when we switch
   * between fullscreen and not
   */
  fun initUiFlags() {
    // Stores the current flags for later use
    initialUiFlags = window.decorView.systemUiVisibility

    window.decorView.systemUiVisibility = stableUiFlags
    window.decorView.setOnSystemUiVisibilityChangeListener(this)
  }

  /**
   * Reset the flags to what it was before we set initialized them in [initUiFlags]
   */
  fun resetUiFlags() {
    window.decorView.systemUiVisibility = initialUiFlags
  }

  /**
   * Applies the correct flags to the windows decor view to enter
   * or exit fullscreen mode
   *
   * @param fullscreen True if entering fullscreen mode
   */
  fun setUiFlags(fullscreen: Boolean) {
    window.decorView.systemUiVisibility = if (fullscreen) fullscreenUiFlags else stableUiFlags
  }

  fun enterFullscreen() {
    setUiFlags(true)
  }

  fun exitFullscreen() {
    if (lastVisibility != View.SYSTEM_UI_FLAG_VISIBLE) {
      setUiFlags(false)
    }
  }
}