/*
 * Copyright (C) 2016 - 2019 ExoMedia Contributors
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

package com.devbrackets.android.exomedia

import android.content.Context
import android.net.Uri
import com.devbrackets.android.exomedia.core.ListenerMux
import com.devbrackets.android.exomedia.core.api.AudioPlayerApi
import com.devbrackets.android.exomedia.core.audio.ExoAudioPlayer
import com.devbrackets.android.exomedia.core.audio.NativeAudioPlayer
import com.devbrackets.android.exomedia.core.exoplayer.ExoMediaPlayer
import com.devbrackets.android.exomedia.core.listener.MetadataListener
import com.devbrackets.android.exomedia.listener.*
import com.devbrackets.android.exomedia.util.DeviceUtil
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.source.MediaSource

/**
 * An AudioPlayer that uses the ExoPlayer as the backing architecture.  If the current device
 * does *NOT* pass the Android Compatibility Test Suite (CTS) then the backing architecture
 * will fall back to using the default Android MediaPlayer.
 *
 *
 * To help with quick conversions from the Android MediaPlayer this class follows the APIs
 * the Android MediaPlayer provides.
 */
open class AudioPlayer(protected val audioPlayerImpl: AudioPlayerApi) : AudioPlayerApi by audioPlayerImpl {
  protected val listenerMux = ListenerMux(MuxNotifier())

  protected var overriddenDuration: Long = -1

  @JvmOverloads
  constructor(context: Context, deviceUtil: DeviceUtil = DeviceUtil()) : this(if (deviceUtil.supportsExoPlayer(context)) ExoAudioPlayer(context) else NativeAudioPlayer(context))

  init {
    audioPlayerImpl.setListenerMux(listenerMux)
  }

  /**
   * Retrieves the duration of the current audio item.  This should only be called after
   * the item is prepared (see [.setOnPreparedListener]).
   * If [.overrideDuration] is set then that value will be returned.
   *
   * @return The millisecond duration of the video
   */
  override val duration: Long
    get() = if (overriddenDuration >= 0) {
      overriddenDuration
    } else audioPlayerImpl.duration

  /**
   * Retrieves the current buffer percent of the audio item.  If an audio item is not currently
   * prepared or buffering the value will be 0.  This should only be called after the audio item is
   * prepared (see [.setOnPreparedListener])
   *
   * @return The integer percent that is buffered [0, 100] inclusive
   */
  val bufferPercentage: Int
    get() = audioPlayerImpl.bufferedPercent

  /**
   * Sets the source path for the audio item.  This path can be a web address (e.g. http://) or
   * an absolute local path (e.g. file://)
   *
   * @param uri The Uri representing the path to the audio item
   */
  fun setDataSource(uri: Uri?) {
    // NOTE: this method is needed to simplify Java integration, @JvmOverloads can't be
    // used on an Interface nor can it be used below due to the default values needing to
    // be specified in the interface and not at the call site
    setDataSource(uri, null)
  }

  /**
   * Sets the source path for the audio item.  This path can be a web address (e.g. http://) or
   * an absolute local path (e.g. file://)
   *
   * @param uri The Uri representing the path to the audio item
   * @param mediaSource The MediaSource to use for audio playback
   */
  override fun setDataSource(uri: Uri?, mediaSource: MediaSource?) {
    audioPlayerImpl.setDataSource(uri, mediaSource)
    overrideDuration(-1)
  }

  /**
   * Stops the current audio playback and resets the repeatListener states
   * so that we receive the callbacks for events like onPrepared
   */
  override fun reset() {
    stopPlayback()
    setDataSource(null, null)

    audioPlayerImpl.reset()
  }

  /**
   * Setting this will override the duration that the item may actually be.  This method should
   * only be used when the item doesn't return the correct duration such as with audio streams.
   * This only overrides the current audio item.
   *
   * @param duration The duration for the current media item or &lt; 0 to disable
   */
  fun overrideDuration(duration: Long) {
    overriddenDuration = duration
  }

  /**
   * Sets the repeatListener to inform of VideoPlayer prepared events
   *
   * @param listener The repeatListener
   */
  fun setOnPreparedListener(listener: OnPreparedListener?) {
    listenerMux.setOnPreparedListener(listener)
  }

  /**
   * Sets the repeatListener to inform of VideoPlayer completion events
   *
   * @param listener The repeatListener
   */
  fun setOnCompletionListener(listener: OnCompletionListener?) {
    listenerMux.setOnCompletionListener(listener)
  }

  /**
   * Sets the repeatListener to inform of VideoPlayer buffer update events
   *
   * @param listener The repeatListener
   */
  fun setOnBufferUpdateListener(listener: OnBufferUpdateListener?) {
    listenerMux.setOnBufferUpdateListener(listener)
  }

  /**
   * Sets the repeatListener to inform of VideoPlayer seek completion events
   *
   * @param listener The repeatListener
   */
  fun setOnSeekCompletionListener(listener: OnSeekCompletionListener?) {
    listenerMux.setOnSeekCompletionListener(listener)
  }

  /**
   * Sets the repeatListener to inform of playback errors
   *
   * @param listener The repeatListener
   */
  fun setOnErrorListener(listener: OnErrorListener?) {
    listenerMux.setOnErrorListener(listener)
  }

  /**
   * Sets the repeatListener to inform of ID3 metadata updates
   *
   * @param listener The repeatListener to inform
   */
  fun setMetadataListener(listener: MetadataListener?) {
    listenerMux.setMetadataListener(listener)
  }

  /**
   * Sets the repeatListener to inform of Analytics updates
   *
   * @param listener The repeatListener to inform
   */
  fun setAnalyticsListener(listener: AnalyticsListener?) {
    listenerMux.setAnalyticsListener(listener)
  }

  /**
   * Performs the functionality to stop the progress polling, and stop any other
   * procedures from running that we no longer need.
   */
  private fun onPlaybackEnded() {
    pause()
  }

  private inner class MuxNotifier : ListenerMux.Notifier() {
    override fun shouldNotifyCompletion(endLeeway: Long): Boolean {
      return currentPosition > 0 && duration > 0 && currentPosition + endLeeway >= duration
    }

    override fun onExoPlayerError(exoMediaPlayer: ExoMediaPlayer, e: Exception?) {
      stopPlayback()

      exoMediaPlayer.forcePrepare()
    }

    override fun onMediaPlaybackEnded() {
      onPlaybackEnded()
    }

    override fun onPrepared() {
      audioPlayerImpl.onMediaPrepared()
    }
  }
}
