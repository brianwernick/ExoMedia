package com.devbrackets.android.exomedia.listener

/**
 * Interface definition of a callback to be invoked when there
 * has been a change is the size of the video currently being displayed.
 */
fun interface OnVideoSizeChangedListener {
  /**
   * Called when the size of the video currently playing has changed.
   * Normally this will only be called once or twice per video, however
   * when using adaptive streams such as HLS, MPeg Dash, or Smooth Stream
   * this will likely be called fairly often (whenever the stream degrades
   * or  improves).
   *
   * @param intrinsicWidth The intrinsic (unscaled) width of the video currently in playback
   * @param intrinsicHeight The intrinsic (unscaled) height of the video currently in playback
   */
  fun onVideoSizeChanged(intrinsicWidth: Int, intrinsicHeight: Int, pixelWidthHeightRatio: Float)
}
