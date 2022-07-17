package com.devbrackets.android.exomedia.core.state

/**
 * Interface definition for a callback to be invoked when the playback
 * state of the [com.devbrackets.android.exomedia.ui.widget.VideoView] or
 * [com.devbrackets.android.exomedia.AudioPlayer] changes
 */
fun interface PlaybackStateListener {
  /**
   * Called when the playback state has changed with one of the value
   * from [PlaybackState]
   *
   * @param state The value representing the new playback state
   */
  fun onPlaybackStateChange(state: PlaybackState)
}