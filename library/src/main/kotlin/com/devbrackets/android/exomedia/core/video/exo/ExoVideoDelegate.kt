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

package com.devbrackets.android.exomedia.core.video.exo

import android.content.Context
import android.net.Uri
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import android.view.Surface

import com.devbrackets.android.exomedia.ExoMedia
import com.devbrackets.android.exomedia.core.ListenerMux
import com.devbrackets.android.exomedia.core.api.VideoViewApi
import com.devbrackets.android.exomedia.core.exoplayer.ExoMediaPlayer
import com.devbrackets.android.exomedia.core.exoplayer.WindowInfo
import com.devbrackets.android.exomedia.core.listener.CaptionListener
import com.devbrackets.android.exomedia.core.listener.MetadataListener
import com.devbrackets.android.exomedia.core.video.ClearableSurface
import com.devbrackets.android.exomedia.core.video.mp.NativeVideoDelegate
import com.devbrackets.android.exomedia.listener.OnBufferUpdateListener
import com.google.android.exoplayer2.drm.DrmSessionManager
import com.google.android.exoplayer2.drm.MediaDrmCallback
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray

class ExoVideoDelegate(
    context: Context,
    var clearableSurface: ClearableSurface
) {
  var exoMediaPlayer: ExoMediaPlayer

  var listenerMux: ListenerMux? = null
    set(value) {
      field?.let {
        exoMediaPlayer.removeListener(it)
        exoMediaPlayer.removeAnalyticsListener(it)
      }

      field = value
      field?.let {
        exoMediaPlayer.addListener(it)
        exoMediaPlayer.addAnalyticsListener(it)
      }
    }

  protected var playRequested = false

  protected var internalListeners = InternalListeners()

  val volume: Float
    @FloatRange(from = 0.0, to = 1.0)
    get() = exoMediaPlayer.volume

  val isPlaying: Boolean
    get() = exoMediaPlayer.playWhenReady

  val duration: Long
    get() = if (!listenerMux!!.isPrepared) {
      0
    } else exoMediaPlayer.duration

  val currentPosition: Long
    get() = if (!listenerMux!!.isPrepared) {
      0
    } else exoMediaPlayer.currentPosition

  val bufferedPercent: Int
    get() = exoMediaPlayer.bufferedPercentage

  val windowInfo: WindowInfo?
    get() = exoMediaPlayer.windowInfo

  val availableTracks: Map<ExoMedia.RendererType, TrackGroupArray>?
    get() = exoMediaPlayer.availableTracks

  val playbackSpeed: Float
    get() = exoMediaPlayer.playbackSpeed

  var drmSessionManager: DrmSessionManager?
    get() = exoMediaPlayer.drmSessionManager
    set(value) {
      exoMediaPlayer.drmSessionManager = value
    }

  init {
    exoMediaPlayer = ExoMediaPlayer(context.applicationContext).apply {
      setMetadataListener(internalListeners)
      setBufferUpdateListener(internalListeners)
    }
  }

  @JvmOverloads
  fun setVideoUri(uri: Uri?, mediaSource: MediaSource? = null) {
    //Makes sure the listeners get the onPrepared callback
    listenerMux?.setNotifiedPrepared(false)
    exoMediaPlayer.seekTo(0)

    mediaSource?.let {
      exoMediaPlayer.setMediaSource(it)
      listenerMux?.setNotifiedCompleted(false)
      return
    }

    uri?.let {
      exoMediaPlayer.setUri(it)
      listenerMux?.setNotifiedCompleted(false)
      return
    }

    exoMediaPlayer.setMediaSource(null)
  }

  fun restart(): Boolean {
    if (!exoMediaPlayer.restart()) {
      return false
    }

    //Makes sure the listeners get the onPrepared callback
    listenerMux?.setNotifiedPrepared(false)
    listenerMux?.setNotifiedCompleted(false)

    return true
  }

  fun setVolume(@FloatRange(from = 0.0, to = 1.0) volume: Float): Boolean {
    exoMediaPlayer.volume = volume
    return true
  }

  fun seekTo(@IntRange(from = 0) milliseconds: Long) {
    exoMediaPlayer.seekTo(milliseconds)
  }

  fun start() {
    exoMediaPlayer.playWhenReady = true
    listenerMux?.setNotifiedCompleted(false)
    playRequested = true
  }

  fun pause() {
    exoMediaPlayer.playWhenReady = false
    playRequested = false
  }

  /**
   * Performs the functionality to stop the video in playback
   *
   * @param clearSurface `true` if the surface should be cleared
   */
  fun stopPlayback(clearSurface: Boolean) {
    exoMediaPlayer.stop()
    playRequested = false

    if (clearSurface) {
      listenerMux?.clearSurfaceWhenReady(clearableSurface)
    }
  }

  fun suspend() {
    exoMediaPlayer.release()
    playRequested = false
  }

  fun trackSelectionAvailable(): Boolean {
    return true
  }

  fun setCaptionListener(listener: CaptionListener?) {
    exoMediaPlayer.setCaptionListener(listener)
  }

  fun setTrack(type: ExoMedia.RendererType, groupIndex: Int, trackIndex: Int) {
    exoMediaPlayer.setSelectedTrack(type, groupIndex, trackIndex)
  }

  fun getSelectedTrackIndex(type: ExoMedia.RendererType, groupIndex: Int): Int {
    return exoMediaPlayer.getSelectedTrackIndex(type, groupIndex)
  }

  /**
   * Clear all selected tracks for the specified renderer.
   * @param type The renderer type
   */
  fun clearSelectedTracks(type: ExoMedia.RendererType) {
    exoMediaPlayer.clearSelectedTracks(type)
  }

  fun setRendererEnabled(type: ExoMedia.RendererType, enabled: Boolean) {
    exoMediaPlayer.setRendererEnabled(type, enabled)
  }

  /**
   * Return true if at least one renderer for the given type is enabled
   * @param type The renderer type
   * @return true if at least one renderer for the given type is enabled
   */
  fun isRendererEnabled(type: ExoMedia.RendererType): Boolean {
    return exoMediaPlayer.isRendererEnabled(type)
  }

  fun setPlaybackSpeed(speed: Float): Boolean {
    return exoMediaPlayer.setPlaybackSpeed(speed)
  }

  fun release() {
    exoMediaPlayer.release()
  }

  fun setRepeatMode(repeatMode: Int) {
    exoMediaPlayer.setRepeatMode(repeatMode)
  }

  fun onSurfaceReady(surface: Surface) {
    exoMediaPlayer.setSurface(surface)
    if (playRequested) {
      exoMediaPlayer.playWhenReady = true
    }
  }

  fun onSurfaceDestroyed() {
    exoMediaPlayer.clearSurface()
  }

  protected inner class InternalListeners : MetadataListener, OnBufferUpdateListener {
    override fun onMetadata(metadata: Metadata) {
      listenerMux?.onMetadata(metadata)
    }

    override fun onBufferingUpdate(@IntRange(from = 0, to = 100) percent: Int) {
      listenerMux?.onBufferingUpdate(percent)
    }
  }
}
