package com.devbrackets.android.exomedia.listener

import androidx.media3.common.Timeline

/**
 * Interface definition of a callback to be invoked indicating
 * the media timeline has changed.
 */
interface OnTimelineChangedListener {
  /**
   * Called when the [Timeline] for the media playback has changed. This
   * can occur when the media is first loaded and during playback if the
   * media is a windowed stream or a live event.
   *
   * A *windowed stream* represents media where the start and end is constantly
   * changing such as with a security camera.
   *
   * A *live event* represents media that has a static start-time/file and a
   * changing end-time while the event is ongoing.
   */
  fun onTimelineChanged(timeline: Timeline)
}