package com.devbrackets.android.exomedia.listener

/**
 * Interface definition for a callback to be invoked when the media
 * source is ready for playback.
 */
fun interface OnPreparedListener {
  /**
   * Called when the media file is ready for playback.
   */
  fun onPrepared()
}