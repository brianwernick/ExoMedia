package com.devbrackets.android.exomedia.ui.listener

interface VideoControlsButtonListener {
  /**
   * Occurs when the PlayPause button on the [com.devbrackets.android.exomedia.ui.widget.controls.VideoControls]
   * is clicked
   *
   * @return True if the event has been handled
   */
  fun onPlayPauseClicked(): Boolean

  /**
   * Occurs when the Play button on the [com.devbrackets.android.exomedia.ui.widget.controls.VideoControls]
   * is clicked
   *
   * @return True if the event has been handled
   */
  fun onPlayClicked(): Boolean

  /**
   * Occurs when the Pause button on the [com.devbrackets.android.exomedia.ui.widget.controls.VideoControls]
   * is clicked
   *
   * @return True if the event has been handled
   */
  fun onPauseClicked(): Boolean

  /**
   * Occurs when the Previous button on the [com.devbrackets.android.exomedia.ui.widget.controls.VideoControls]
   * is clicked
   *
   * @return True if the event has been handled
   */
  fun onPreviousClicked(): Boolean

  /**
   * Occurs when the Next button on the [com.devbrackets.android.exomedia.ui.widget.controls.VideoControls]
   * is clicked
   *
   * @return True if the event has been handled
   */
  fun onNextClicked(): Boolean

  /**
   * Occurs when the Rewind button on the [com.devbrackets.android.exomedia.ui.widget.controls.VideoControls]
   * is clicked.
   *
   * @return True if the event has been handled
   */
  fun onRewindClicked(): Boolean

  /**
   * Occurs when the Fast Forward button on the [com.devbrackets.android.exomedia.ui.widget.controls.VideoControls]
   * is clicked.
   *
   * @return True if the event has been handled
   */
  fun onFastForwardClicked(): Boolean
}
