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

package com.devbrackets.android.exomedia.core.audio

import android.content.Context
import android.net.Uri
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import com.devbrackets.android.exomedia.ExoMedia
import com.devbrackets.android.exomedia.core.ListenerMux
import com.devbrackets.android.exomedia.core.api.AudioPlayerApi
import com.devbrackets.android.exomedia.core.exoplayer.ExoMediaPlayer
import com.devbrackets.android.exomedia.core.exoplayer.WindowInfo
import com.devbrackets.android.exomedia.core.listener.MetadataListener
import com.devbrackets.android.exomedia.core.video.mp.NativeVideoDelegate
import com.devbrackets.android.exomedia.listener.OnBufferUpdateListener
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.drm.DrmSessionManager
import com.google.android.exoplayer2.drm.MediaDrmCallback
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray

/**
 * A [AudioPlayerApi] implementation that uses the ExoPlayer
 * as the backing media player.
 */
open class ExoAudioPlayer(protected val context: Context) : AudioPlayerApi {
  protected val exoMediaPlayer: ExoMediaPlayer = ExoMediaPlayer(context)

  protected var _listenerMux: ListenerMux? = null

  protected var internalListeners = InternalListeners()

  protected var playRequested = false

  override val volumeLeft: Float
    get() = exoMediaPlayer.volume

  override val volumeRight: Float
    get() = exoMediaPlayer.volume

  override val isPlaying: Boolean
    get() = exoMediaPlayer.playWhenReady

  override val duration: Long
    get() = if (!_listenerMux!!.isPrepared) {
      0
    } else exoMediaPlayer.duration

  override val currentPosition: Long
    get() = if (!_listenerMux!!.isPrepared) {
      0
    } else exoMediaPlayer.currentPosition

  override val bufferedPercent: Int
    get() = exoMediaPlayer.bufferedPercentage

  override val windowInfo: WindowInfo?
    get() = exoMediaPlayer.windowInfo

  override val audioSessionId: Int
    get() = exoMediaPlayer.audioSessionId

  override val playbackSpeed: Float
    get() = exoMediaPlayer.playbackSpeed

  override val availableTracks: Map<ExoMedia.RendererType, TrackGroupArray>?
    get() = exoMediaPlayer.availableTracks

  override var drmSessionManager: DrmSessionManager?
    get() = exoMediaPlayer.drmSessionManager
    set(value) { exoMediaPlayer.drmSessionManager = value }

  init {
    exoMediaPlayer.setMetadataListener(internalListeners)
    exoMediaPlayer.setBufferUpdateListener(internalListeners)
  }

  override fun setDataSource(uri: Uri?, mediaSource: MediaSource?) {
    //Makes sure the listeners get the onPrepared callback
    _listenerMux?.setNotifiedPrepared(false)
    exoMediaPlayer.seekTo(0)

    mediaSource?.let {
      exoMediaPlayer.setMediaSource(it)
      _listenerMux?.setNotifiedCompleted(false)
      return
    }

    uri?.let {
      exoMediaPlayer.setUri(it)
      _listenerMux?.setNotifiedCompleted(false)
      return
    }

    exoMediaPlayer.setMediaSource(null)
  }

  override fun prepareAsync() {
    exoMediaPlayer.prepare()
  }

  override fun reset() {
    //Purposefully left blank
  }

  override fun setVolume(@FloatRange(from = 0.0, to = 1.0) left: Float, @FloatRange(from = 0.0, to = 1.0) right: Float) {
    //Averages the volume since the ExoPlayer only takes a single channel
    exoMediaPlayer.volume = (left + right) / 2
  }

  override fun seekTo(@IntRange(from = 0) milliseconds: Long) {
    exoMediaPlayer.seekTo(milliseconds)
  }

  override fun start() {
    exoMediaPlayer.playWhenReady = true
    _listenerMux?.setNotifiedCompleted(false)
    playRequested = true
  }

  override fun pause() {
    exoMediaPlayer.playWhenReady = false
    playRequested = false
  }

  override fun stopPlayback() {
    exoMediaPlayer.stop()
    playRequested = false
  }

  /**
   * If the media has completed playback, calling `restart` will seek to the beginning of the media, and play it.
   *
   * @return `true` if the media was successfully restarted, otherwise `false`
   */
  override fun restart(): Boolean {
    if (!exoMediaPlayer.restart()) {
      return false
    }

    _listenerMux?.setNotifiedCompleted(false)
    _listenerMux?.setNotifiedPrepared(false)

    return true
  }

  override fun release() {
    exoMediaPlayer.release()
  }

  override fun setPlaybackSpeed(speed: Float): Boolean {
    return exoMediaPlayer.setPlaybackSpeed(speed)
  }

  override fun setAudioStreamType(streamType: Int) {
    exoMediaPlayer.setAudioStreamType(streamType)
  }

  override fun setWakeMode(context: Context, mode: Int) {
    exoMediaPlayer.setWakeMode(context, mode)
  }

  override fun trackSelectionAvailable(): Boolean {
    return true
  }

  override fun setTrack(type: ExoMedia.RendererType, groupIndex: Int, trackIndex: Int) {
    exoMediaPlayer.setSelectedTrack(type, groupIndex, trackIndex)
  }

  override fun getSelectedTrackIndex(type: ExoMedia.RendererType, groupIndex: Int): Int {
    return exoMediaPlayer.getSelectedTrackIndex(type, groupIndex)
  }

  override fun setListenerMux(listenerMux: ListenerMux) {
    this._listenerMux?.let { oldListenerMux ->
      exoMediaPlayer.removeListener(oldListenerMux)
      exoMediaPlayer.removeAnalyticsListener(oldListenerMux)
    }

    this._listenerMux = listenerMux
    exoMediaPlayer.addListener(listenerMux)
    exoMediaPlayer.addAnalyticsListener(listenerMux)
  }

  override fun onMediaPrepared() {
    //Purposefully left blank
  }

  override fun setRepeatMode(@Player.RepeatMode repeatMode: Int) {
    exoMediaPlayer.setRepeatMode(repeatMode)
  }

  protected inner class InternalListeners : MetadataListener, OnBufferUpdateListener {
    override fun onMetadata(metadata: Metadata) {
      _listenerMux?.onMetadata(metadata)
    }

    override fun onBufferingUpdate(@IntRange(from = 0, to = 100) percent: Int) {
      _listenerMux?.onBufferingUpdate(percent)
    }
  }
}