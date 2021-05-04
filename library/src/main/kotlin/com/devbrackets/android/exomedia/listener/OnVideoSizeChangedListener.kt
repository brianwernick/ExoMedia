/*
 * Copyright (C) 2017 - 2018 ExoMedia Contributors
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

package com.devbrackets.android.exomedia.listener

/**
 * Interface definition of a callback to be invoked when there
 * has been a change is the size of the video currently being displayed.
 */
interface OnVideoSizeChangedListener {
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
