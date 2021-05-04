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

package com.devbrackets.android.exomedia.core.audio

import android.net.Uri
import androidx.annotation.IntRange
import com.devbrackets.android.exomedia.core.ListenerMux
import com.devbrackets.android.exomedia.nmp.ExoMediaPlayerImpl
import com.devbrackets.android.exomedia.nmp.manager.window.WindowInfo
import com.devbrackets.android.exomedia.core.listener.MetadataListener
import com.devbrackets.android.exomedia.core.renderer.RendererType
import com.devbrackets.android.exomedia.listener.OnBufferUpdateListener
import com.devbrackets.android.exomedia.nmp.config.PlayerConfig
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.drm.DrmSessionManagerProvider
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray

/**
 * A [AudioPlayerApi] implementation that uses the ExoPlayer
 * as the backing media player.
 */
open class ExoAudioPlayer(protected val config: PlayerConfig) : AudioPlayerApi {
  protected val corePlayer: ExoMediaPlayerImpl = ExoMediaPlayerImpl(config)

  protected var _listenerMux: ListenerMux? = null

  protected var internalListeners = InternalListeners()

  protected var playRequested = false

  override var volume: Float
    get() = corePlayer.volume
    set(value) {
      corePlayer.volume = value
    }

  override val isPlaying: Boolean
    get() = corePlayer.playWhenReady

  override val duration: Long
    get() = if (!_listenerMux!!.isPrepared) {
      0
    } else corePlayer.duration

  override val currentPosition: Long
    get() = if (!_listenerMux!!.isPrepared) {
      0
    } else corePlayer.currentPosition

  override val bufferedPercent: Int
    get() = corePlayer.bufferedPercent

  override val windowInfo: WindowInfo?
    get() = corePlayer.windowInfo

  override val audioSessionId: Int
    get() = corePlayer.audioSessionId

  override val playbackSpeed: Float
    get() = corePlayer.playbackSpeed

  override val availableTracks: Map<RendererType, TrackGroupArray>?
    get() = corePlayer.availableTracks

  override var drmSessionManagerProvider: DrmSessionManagerProvider?
    get() = corePlayer.drmSessionManagerProvider
    set(value) { corePlayer.drmSessionManagerProvider = value }

  init {
    corePlayer.setMetadataListener(internalListeners)
    corePlayer.setBufferUpdateListener(internalListeners)
  }

  override fun setMedia(uri: Uri?, mediaSource: MediaSource?) {
    //Makes sure the listeners get the onPrepared callback
    _listenerMux?.setNotifiedPrepared(false)
    corePlayer.seekTo(0)

    mediaSource?.let {
      corePlayer.setMediaSource(it)
      _listenerMux?.setNotifiedCompleted(false)
      corePlayer.prepare()
      return
    }

    uri?.let {
      corePlayer.setMediaUri(it)
      _listenerMux?.setNotifiedCompleted(false)
      corePlayer.prepare()
      return
    }

    corePlayer.setMediaSource(null)
  }

  override fun reset() {
    //Purposefully left blank
  }

  override fun seekTo(@IntRange(from = 0) milliseconds: Long) {
    corePlayer.seekTo(milliseconds)
  }

  override fun start() {
    corePlayer.playWhenReady = true
    _listenerMux?.setNotifiedCompleted(false)
    playRequested = true
  }

  override fun pause() {
    corePlayer.playWhenReady = false
    playRequested = false
  }

  override fun stop() {
    corePlayer.stop()
    playRequested = false
  }

  /**
   * If the media has completed playback, calling `restart` will seek to the beginning of the media, and play it.
   *
   * @return `true` if the media was successfully restarted, otherwise `false`
   */
  override fun restart(): Boolean {
    if (!corePlayer.restart()) {
      return false
    }

    _listenerMux?.setNotifiedCompleted(false)
    _listenerMux?.setNotifiedPrepared(false)

    return true
  }

  override fun release() {
    corePlayer.release()
  }

  override fun setPlaybackSpeed(speed: Float): Boolean {
    corePlayer.playbackSpeed = speed
    return true
  }

  override fun setAudioStreamType(streamType: Int) {
    corePlayer.setAudioStreamType(streamType)
  }

  override fun setWakeLevel(levelAndFlags: Int) {
    corePlayer.setWakeLevel(levelAndFlags)
  }

  override fun trackSelectionAvailable(): Boolean {
    return true
  }

  override fun setSelectedTrack(type: RendererType, groupIndex: Int, trackIndex: Int) {
    corePlayer.setSelectedTrack(type, groupIndex, trackIndex)
  }

  override fun getSelectedTrackIndex(type: RendererType, groupIndex: Int): Int {
    return corePlayer.getSelectedTrackIndex(type, groupIndex)
  }

  override fun clearSelectedTracks(type: RendererType) {
    corePlayer.clearSelectedTracks(type)
  }

  override fun setRendererEnabled(type: RendererType, enabled: Boolean) {
    corePlayer.setRendererEnabled(type, enabled)
  }

  override fun isRendererEnabled(type: RendererType): Boolean {
    return corePlayer.isRendererEnabled(type)
  }

  override fun setListenerMux(listenerMux: ListenerMux) {
    this._listenerMux?.let { oldListenerMux ->
      corePlayer.removeListener(oldListenerMux)
      corePlayer.removeAnalyticsListener(oldListenerMux)
    }

    this._listenerMux = listenerMux
    corePlayer.addListener(listenerMux)
    corePlayer.addAnalyticsListener(listenerMux)
  }

  override fun onMediaPrepared() {
    //Purposefully left blank
  }

  override fun setRepeatMode(@Player.RepeatMode repeatMode: Int) {
    corePlayer.setRepeatMode(repeatMode)
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