package com.devbrackets.android.exomedia.listener

/**
 * Interface definition for a callback to be invoked when playback of
 * a media source has completed.
 */
interface OnCompletionListener {
  /**
   * Called when the end of a media source is reached during playback.
   */
  fun onCompletion()
}