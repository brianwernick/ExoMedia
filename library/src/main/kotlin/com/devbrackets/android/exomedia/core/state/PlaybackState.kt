package com.devbrackets.android.exomedia.core.state


/**
 * States associated with the current media playback for the
 * [com.devbrackets.android.exomedia.ui.widget.VideoView]
 * and [com.devbrackets.android.exomedia.AudioPlayer]
 */
enum class PlaybackState {
  /**
   * The player has not been initialized with a media uri or previously prepared
   */
  IDLE,

  /**
   * The media uri specified is currently being prepared for playback
   */
  PREPARING,

  /**
   * The media needs to buffer before playback can be started or resume
   */
  BUFFERING,

  /**
   * The media is currently seeking to the requested position
   */
  SEEKING,

  /**
   * The media is ready for playback. This can occur after [.BUFFERING]
   * if playback wasn't requested
   */
  READY,

  /**
   * The media is currently in playing
   */
  PLAYING,

  /**
   * The media playback was paused
   */
  PAUSED,

  /**
   * The playback completed normally
   */
  COMPLETED,

  /**
   * The playback was stopped
   */
  STOPPED,

  /**
   * The player has been released and can no longer be reused
   */
  RELEASED,

  /**
   * There was an error during playback
   */
  ERROR
}