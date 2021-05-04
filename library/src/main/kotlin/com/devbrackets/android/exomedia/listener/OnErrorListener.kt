/*
 * Copyright (C) 2016-2019 ExoMedia Contributors
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
 * has been an error during an asynchronous operation.
 */
interface OnErrorListener {
  /**
   * Called to indicate an error has occurred. In the case that the error was
   * caused by the Native [android.media.MediaPlayer] `e` should
   * be of the type [com.devbrackets.android.exomedia.core.exception.NativeMediaPlaybackException]
   * while all other exceptions will be provided by the [com.google.android.exoplayer2.ExoPlayer]
   * implementation directly.
   *
   * @param e The [Exception] that caused or describes the error
   * @return True if the method handled the error, false if it didn't.
   */
  fun onError(e: Exception?): Boolean
}