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

import android.graphics.SurfaceTexture
import android.net.Uri
import android.view.*
import androidx.annotation.IntRange
import com.devbrackets.android.exomedia.core.ListenerMux
import com.devbrackets.android.exomedia.core.listener.CaptionListener
import com.devbrackets.android.exomedia.core.listener.MetadataListener
import com.devbrackets.android.exomedia.core.listener.VideoSizeListener
import com.devbrackets.android.exomedia.core.renderer.RendererType
import com.devbrackets.android.exomedia.core.video.surface.VideoSurface
import com.devbrackets.android.exomedia.listener.OnBufferUpdateListener
import com.devbrackets.android.exomedia.nmp.ExoMediaPlayerImpl
import com.devbrackets.android.exomedia.nmp.config.PlayerConfig
import com.devbrackets.android.exomedia.nmp.manager.window.WindowInfo
import com.google.android.exoplayer2.drm.DrmSessionManagerProvider
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import java.lang.IllegalArgumentException

class ExoVideoPlayer(
    private val playerConfig: PlayerConfig,
    private val surface: VideoSurface
): VideoPlayerApi {
  val corePlayer: ExoMediaPlayerImpl by lazy {
    ExoMediaPlayerImpl(playerConfig).apply {
      setMetadataListener(internalListeners)
      setBufferUpdateListener(internalListeners)
      setVideoSizeListener(internalListeners)
    }
  }

  var _listenerMux: ListenerMux? = null

  protected var playRequested = false

  protected var internalListeners = InternalListeners()

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

  override val availableTracks: Map<RendererType, TrackGroupArray>?
    get() = corePlayer.availableTracks

  override val playbackSpeed: Float
    get() = corePlayer.playbackSpeed

  override var drmSessionManagerProvider: DrmSessionManagerProvider?
    get() = corePlayer.drmSessionManagerProvider
    set(value) {
      corePlayer.drmSessionManagerProvider = value
    }

  init {
    when(surface) {
      is SurfaceView -> {
        surface.holder.addCallback(HolderCallback())
      }
      is TextureView -> {
        surface.surfaceTextureListener = ExoMediaVideoSurfaceTextureListener()
      }
      else -> {
        throw IllegalArgumentException("Surface $surface not one of TextureView or SurfaceView")
      }
    }

    surface.updateVideoSize(0, 0)
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
    // Purposefully left blank
  }

  override fun restart(): Boolean {
    if (!corePlayer.restart()) {
      return false
    }

    //Makes sure the listeners get the onPrepared callback
    _listenerMux?.setNotifiedPrepared(false)
    _listenerMux?.setNotifiedCompleted(false)

    return true
  }

  override fun seekTo(@IntRange(from = 0) milliseconds: Long) {
    corePlayer.seekTo(milliseconds)
  }

  override fun setWakeLevel(levelAndFlags: Int) {
    corePlayer.setWakeLevel(levelAndFlags)
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
    stop(false)
  }

  override fun stop(clearSurface: Boolean) {
    corePlayer.stop()
    playRequested = false

    if (clearSurface) {
      _listenerMux?.clearSurfaceWhenReady(surface)
    }
  }

  override fun trackSelectionAvailable(): Boolean {
    return true
  }

  override fun setCaptionListener(listener: CaptionListener?) {
    corePlayer.setCaptionListener(listener)
  }

  override fun setSelectedTrack(type: RendererType, groupIndex: Int, trackIndex: Int) {
    corePlayer.setSelectedTrack(type, groupIndex, trackIndex)
  }

  override fun getSelectedTrackIndex(type: RendererType, groupIndex: Int): Int {
    return corePlayer.getSelectedTrackIndex(type, groupIndex)
  }

  /**
   * Clear all selected tracks for the specified renderer.
   * @param type The renderer type
   */
  override fun clearSelectedTracks(type: RendererType) {
    corePlayer.clearSelectedTracks(type)
  }

  override fun setRendererEnabled(type: RendererType, enabled: Boolean) {
    corePlayer.setRendererEnabled(type, enabled)
  }

  /**
   * Return true if at least one renderer for the given type is enabled
   * @param type The renderer type
   * @return true if at least one renderer for the given type is enabled
   */
  override fun isRendererEnabled(type: RendererType): Boolean {
    return corePlayer.isRendererEnabled(type)
  }

  override fun setPlaybackSpeed(speed: Float): Boolean {
    corePlayer.playbackSpeed = speed
    return true
  }

  override fun setAudioStreamType(streamType: Int) {
    corePlayer.setAudioStreamType(streamType)
  }

  override fun release() {
    corePlayer.release()
    playRequested = false
  }

  override fun setRepeatMode(repeatMode: Int) {
    corePlayer.setRepeatMode(repeatMode)
  }

  fun onSurfaceReady(surface: Surface) {
    corePlayer.surface = surface
    if (playRequested) {
      corePlayer.playWhenReady = true
    }
  }

  fun onSurfaceDestroyed() {
    corePlayer.clearSurface()
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
    // Purposefully left blank
  }

  protected inner class InternalListeners : MetadataListener, OnBufferUpdateListener, VideoSizeListener {
    override fun onMetadata(metadata: Metadata) {
      _listenerMux?.onMetadata(metadata)
    }

    override fun onBufferingUpdate(@IntRange(from = 0, to = 100) percent: Int) {
      _listenerMux?.onBufferingUpdate(percent)
    }

    override fun onVideoSizeChanged(width: Int, height: Int, unAppliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
      surface.onVideoSizeChanged(width, height, pixelWidthHeightRatio)
    }
  }

  protected inner class ExoMediaVideoSurfaceTextureListener : TextureView.SurfaceTextureListener {
    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
      onSurfaceReady(Surface(surfaceTexture))
    }

    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
      onSurfaceDestroyed()
      surfaceTexture.release()

      return true
    }

    override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
      // Purposefully left blank
    }

    override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
      // Purposefully left blank
    }
  }

  protected inner class HolderCallback : SurfaceHolder.Callback {
    override fun surfaceCreated(holder: SurfaceHolder) {
      onSurfaceReady(holder.surface)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
      //Purposefully left blank
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
      onSurfaceDestroyed()
      holder.surface.release()
    }
  }
}