package com.devbrackets.android.exomedia.ui.listener

/**
 * Interface definition for a callback to be invoked when the media
 * has external seek requests
 */
interface VideoControlsSeekListener {

  /**
   * Occurs when the media has entered a state of waiting until the
   * [.onSeekEnded] is called.
   *
   * @return `true` if the state has been handled
   */
  fun onSeekStarted(): Boolean

  /**
   * Occurs when the media has finalized the request to seek.  This
   * may be called after [.onSeekStarted] or on its own.
   *
   * @param seekTime The time in milliseconds to seek to
   * @return `true` if the seek has been handled
   */
  fun onSeekEnded(seekTime: Long): Boolean
}
