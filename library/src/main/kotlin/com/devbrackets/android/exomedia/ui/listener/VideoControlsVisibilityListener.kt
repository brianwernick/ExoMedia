package com.devbrackets.android.exomedia.ui.listener

/**
 * Interface definition for a callback to be invoked when the visibility
 * of the [com.devbrackets.android.exomedia.ui.widget.DefaultVideoControls] changes
 */
interface VideoControlsVisibilityListener {
  /**
   * Occurs when the DefaultControls becomes visible
   */
  fun onControlsShown()

  /**
   * Occurs when the DefaultControls becomes hidden
   */
  fun onControlsHidden()
}
