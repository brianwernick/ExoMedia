package com.devbrackets.android.exomedia.listener

import androidx.annotation.IntRange

/**
 * Interface definition of a callback to be invoked indicating buffering
 * status of a media resource being streamed.
 */
interface OnBufferUpdateListener {
  /**
   * Called to update status in buffering a media stream.
   * The received buffering percentage
   * indicates how much of the content has been buffered or played.
   * For example a buffering update of 80 percent when half the content
   * has already been played indicates that the next 30 percent of the
   * content to play has been buffered.
   *
   * @param percent The integer percent that is buffered [0, 100] inclusive
   */
  fun onBufferingUpdate(@IntRange(from = 0, to = 100) percent: Int)
}