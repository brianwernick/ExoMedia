/*
 * Copyright (C) 2016 - 2021 ExoMedia Contributors
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

package com.devbrackets.android.exomedia.core.video

import com.devbrackets.android.exomedia.core.audio.AudioPlayerApi
import com.devbrackets.android.exomedia.core.listener.CaptionListener

/**
 * The basic APIs expected in the backing video view
 * implementations to allow us to create an abstraction
 * between the Native (Android) VideoView and the VideoView
 * using the ExoPlayer.
 */
interface VideoPlayerApi: AudioPlayerApi {
  /**
   * Performs the functionality to stop the video in playback
   *
   * @param clearSurface `true` if the surface should be cleared
   */
  fun stop(clearSurface: Boolean)

  /**
   * Specifies the listener to inform of closed caption events
   *
   * @param listener The listener to inform of closed caption events
   */
  fun setCaptionListener(listener: CaptionListener?)
}