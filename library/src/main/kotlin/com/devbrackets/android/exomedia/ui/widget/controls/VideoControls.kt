/*
 * Copyright (C) 2015 - 2018 ExoMedia Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.devbrackets.android.exomedia.ui.widget.controls

import androidx.annotation.IntRange
import com.devbrackets.android.exomedia.ui.widget.VideoView

/**
 * An Interface that represents the core VideoControl functionality that
 * the [VideoView] uses to inform the controls of updated states, etc.
 */
interface VideoControls {

  /**
   * `true` if the [VideoControls] are visible
   */
  val isVisible: Boolean

  /**
   * Called when the controls have been registered by the
   * [VideoView].
   *
   * @param videoView The [VideoView] that the controls are attached to
   */
  fun onAttachedToView(videoView: VideoView)

  /**
   * Called when the controls have been cleaned up on the [VideoView]
   * side in preparation for detachment.
   *
   * @param videoView The [VideoView] that the controls are detaching from
   */
  fun onDetachedFromView(videoView: VideoView)

  /**
   * Shows the controls immediately
   */
  fun show()

  /**
   * Hides the controls immediately if `delayed` is false
   * otherwise a delay determined by the implementation will be used
   * before the controls are hidden.  If the user is interacting with
   * the controls then we wait until after they are done to start the
   * delay.
   */
  fun hide(delayed: Boolean)

  /**
   * Update the controls to indicate that the video
   * is loading.
   *
   * @param initialLoad `true` if the loading is the initial state, not for seeking or buffering
   */
  fun showLoading(initialLoad: Boolean)

  /**
   * Update the controls to indicate that the video is no longer loading
   * which will re-display the play/pause, progress, etc. controls
   */
  fun finishLoading()

  /**
   * Informs the controls that the playback state has changed.  This will
   * update to display the correct views, and manage progress polling.
   *
   * @param isPlaying True if the media is currently playing
   */
  fun updatePlaybackState(isPlaying: Boolean)

  /**
   * Sets the video duration in Milliseconds to display
   * at the end of the progress bar
   *
   * @param duration The duration of the video in milliseconds
   */
  fun setDuration(@IntRange(from = 0) duration: Long)
}