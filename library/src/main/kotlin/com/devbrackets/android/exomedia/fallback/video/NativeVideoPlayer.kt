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

package com.devbrackets.android.exomedia.fallback.video

import android.content.ContentValues
import android.content.Context
import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.*
import androidx.annotation.FloatRange
import com.devbrackets.android.exomedia.core.ListenerMux
import com.devbrackets.android.exomedia.core.listener.CaptionListener
import com.devbrackets.android.exomedia.core.renderer.RendererType
import com.devbrackets.android.exomedia.core.video.VideoPlayerApi
import com.devbrackets.android.exomedia.core.video.surface.VideoSurface
import com.devbrackets.android.exomedia.nmp.manager.window.WindowInfo
import com.google.android.exoplayer2.drm.DrmSessionManagerProvider
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import java.io.IOException

class NativeVideoPlayer(
    protected var context: Context,
    private val surface: VideoSurface
) : VideoPlayerApi {

  protected var headers: Map<String, String>? = null

  protected var currentState = State.IDLE

  // TODO: this works differently from the NativeAudioPlayer, why?
  protected val mediaPlayer: MediaPlayer by lazy {
    MediaPlayer().apply {
      setOnInfoListener(internalListeners)
      setOnErrorListener(internalListeners)
      setOnPreparedListener(internalListeners)
      setOnCompletionListener(internalListeners)
      setOnSeekCompleteListener(internalListeners)
      setOnBufferingUpdateListener(internalListeners)
      setOnVideoSizeChangedListener(internalListeners)

      setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
      setScreenOnWhilePlaying(true)
    }
  }

  protected var playRequested = false
  protected var requestedSeek: Long = 0
  protected var currentBufferPercent: Int = 0

  @FloatRange(from = 0.0, to = 1.0)
  protected var requestedVolume = 1.0f

  protected var _listenerMux: ListenerMux? = null

  var internalListeners = InternalListeners()

  /**
   * Register a callback to be invoked when the end of a media file
   * has been reached during playback.
   */
  var onCompletionListener: MediaPlayer.OnCompletionListener? = null

  /**
   * Register a callback to be invoked when the media file
   * is loaded and ready to go.
   */
  var onPreparedListener: MediaPlayer.OnPreparedListener? = null

  /**
   * Register a callback to be invoked when the status of a network
   * stream's buffer has changed.
   */
  var onBufferingUpdateListener: MediaPlayer.OnBufferingUpdateListener? = null

  /**
   * Register a callback to be invoked when a seek operation has been
   * completed.
   */
  var onSeekCompleteListener: MediaPlayer.OnSeekCompleteListener? = null

  /**
   * Register a callback to be invoked when an error occurs
   * during playback or setup.  If no repeatListener is specified,
   * or if the repeatListener returned false, TextureVideoView will inform
   * the user of any errors.
   */
  var onErrorListener: MediaPlayer.OnErrorListener? = null

  /**
   * Register a callback to be invoked when an informational event
   * occurs during playback or setup.
   */
  var onInfoListener: MediaPlayer.OnInfoListener? = null

  override var volume: Float
    get() = requestedVolume
    set(value) {
      requestedVolume = value
      mediaPlayer.setVolume(value, value)
    }

  override val duration: Long
    get() = if (_listenerMux?.isPrepared != true || !isReady) {
      0
    } else mediaPlayer.duration.toLong()

  override val currentPosition: Long
    get() = if (_listenerMux?.isPrepared != true || !isReady) {
      0
    } else mediaPlayer.currentPosition.toLong()

  override val isPlaying: Boolean
    get() = isReady && mediaPlayer.isPlaying

  override val bufferedPercent: Int
    get() = currentBufferPercent

  override var drmSessionManagerProvider: DrmSessionManagerProvider?
    get() = null
    set(_) {}

  override val audioSessionId: Int
    get() = mediaPlayer.audioSessionId

  override val availableTracks: Map<RendererType, TrackGroupArray>?
    get() = null

  override val windowInfo: WindowInfo?
    get() = null

  // Marshmallow+ support setting the playback speed natively
  override val playbackSpeed: Float
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      mediaPlayer.playbackParams.speed
    } else 1f

  protected val isReady: Boolean
    get() = currentState != State.ERROR && currentState != State.IDLE && currentState != State.PREPARING

  enum class State {
    ERROR,
    IDLE,
    PREPARING,
    PREPARED,
    PLAYING,
    PAUSED,
    COMPLETED
  }

  init {
    currentState = State.IDLE

    when (surface) {
      is SurfaceView -> {
        surface.holder.addCallback(HolderCallback())
      }
      is TextureView -> {
        surface.surfaceTextureListener = TextureVideoViewSurfaceListener()
      }
      else -> {
        throw IllegalArgumentException("Surface $surface not one of TextureView or SurfaceView")
      }
    }
  }

  override fun setListenerMux(listenerMux: ListenerMux) {
    _listenerMux = listenerMux

    onCompletionListener = listenerMux
    onCompletionListener = listenerMux
    onPreparedListener = listenerMux
    onBufferingUpdateListener = listenerMux
    onSeekCompleteListener = listenerMux
    onErrorListener = listenerMux
  }

  override fun start() {
    if (isReady) {
      mediaPlayer.start()
      currentState = State.PLAYING
    }

    playRequested = true
    _listenerMux?.setNotifiedCompleted(false)
  }

  override fun pause() {
    if (isReady && mediaPlayer.isPlaying) {
      mediaPlayer.pause()
      currentState = State.PAUSED
    }

    playRequested = false
  }

  override fun seekTo(milliseconds: Long) {
    if (isReady) {
      mediaPlayer.seekTo(milliseconds.toInt())
      requestedSeek = 0
    } else {
      requestedSeek = milliseconds
    }
  }

  override fun setPlaybackSpeed(speed: Float): Boolean {
    // Marshmallow+ support setting the playback speed natively
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      mediaPlayer.playbackParams = PlaybackParams().apply {
        this.speed = speed
      }
      return true
    }

    return false
  }

  override fun stop() {
    stop(false)
  }

  override fun setCaptionListener(listener: CaptionListener?) {
    // Not Supported
  }

  override fun stop(clearSurface: Boolean) {
    currentState = State.IDLE

    if (isReady) {
      try {
        mediaPlayer.stop()
      } catch (e: Exception) {
        Log.d(ContentValues.TAG, "stopPlayback: error calling mediaPlayer.stop()", e)
      }

    }

    playRequested = false
    if (clearSurface) {
      _listenerMux?.clearSurfaceWhenReady(surface)
    }
  }

  /**
   * Cleans up the resources being held.  This should only be called when
   * destroying the video view
   */
  override fun release() {
    currentState = State.IDLE

    try {
      mediaPlayer.reset()
      mediaPlayer.release()
    } catch (e: Exception) {
      Log.d(ContentValues.TAG, "stopPlayback: error calling mediaPlayer.reset() or mediaPlayer.release()", e)
    }

    playRequested = false
  }

  override fun reset() {
    mediaPlayer.reset()
  }

  override fun restart(): Boolean {
    if (currentState != State.COMPLETED) {
      return false
    }

    seekTo(0)
    start()

    //Makes sure the listeners get the onPrepared callback
    _listenerMux?.setNotifiedPrepared(false)
    _listenerMux?.setNotifiedCompleted(false)

    return true
  }

  override fun trackSelectionAvailable(): Boolean {
    return false
  }

  override fun setSelectedTrack(type: RendererType, groupIndex: Int, trackIndex: Int) {
    // Not supported
  }

  override fun getSelectedTrackIndex(type: RendererType, groupIndex: Int): Int {
    return -1
  }

  override fun clearSelectedTracks(type: RendererType) {
    // Not supported
  }

  override fun setRendererEnabled(type: RendererType, enabled: Boolean) {
    // Not supported
  }

  override fun isRendererEnabled(type: RendererType): Boolean {
    return false
  }

  override fun setAudioStreamType(streamType: Int) {
    mediaPlayer.setAudioStreamType(streamType)
  }

  override fun setWakeLevel(levelAndFlags: Int) {
    mediaPlayer.setWakeMode(context, levelAndFlags)
  }

  override fun onMediaPrepared() {
    TODO("Start playback?")
  }

  override fun setRepeatMode(repeatMode: Int) {
    // Not Supported
  }

  fun onSurfaceSizeChanged(width: Int, height: Int) {
    if (width <= 0 || height <= 0) {
      return
    }

    if (requestedSeek != 0L) {
      seekTo(requestedSeek)
    }

    if (playRequested) {
      start()
    }
  }

  fun onSurfaceReady(surface: Surface) {
    mediaPlayer.setSurface(surface)
    if (playRequested) {
      start()
    }
  }

  override fun setMedia(uri: Uri?, mediaSource: MediaSource?) {
    if (uri == null) {
      return
    }

    currentBufferPercent = 0

    try {
      mediaPlayer.reset()
      mediaPlayer.setDataSource(context.applicationContext, uri, headers)
      mediaPlayer.prepareAsync()

      currentState = State.PREPARING
    } catch (ex: IOException) {
      Log.w(ContentValues.TAG, "Unable to open content: $uri", ex)
      currentState = State.ERROR

      internalListeners.onError(mediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0)
    } catch (ex: IllegalArgumentException) {
      Log.w(ContentValues.TAG, "Unable to open content: $uri", ex)
      currentState = State.ERROR
      internalListeners.onError(mediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0)
    }
  }

  protected inner class TextureVideoViewSurfaceListener : TextureView.SurfaceTextureListener {
    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
      onSurfaceReady(Surface(surfaceTexture))
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
      onSurfaceSizeChanged(width, height)
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
      surface.release()
      release()
      return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
      //Purposefully left blank
    }
  }

  protected inner class HolderCallback : SurfaceHolder.Callback {
    override fun surfaceCreated(holder: SurfaceHolder) {
      onSurfaceReady(holder.surface)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
      onSurfaceSizeChanged(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
      holder.surface.release()
      release()
    }
  }

  inner class InternalListeners:
      MediaPlayer.OnBufferingUpdateListener,
      MediaPlayer.OnErrorListener,
      MediaPlayer.OnPreparedListener,
      MediaPlayer.OnCompletionListener,
      MediaPlayer.OnSeekCompleteListener,
      MediaPlayer.OnInfoListener,
      MediaPlayer.OnVideoSizeChangedListener {
    override fun onBufferingUpdate(mp: MediaPlayer, percent: Int) {
      currentBufferPercent = percent
      onBufferingUpdateListener?.onBufferingUpdate(mp, percent)
    }

    override fun onCompletion(mp: MediaPlayer) {
      currentState = State.COMPLETED
      onCompletionListener?.onCompletion(mediaPlayer)
    }

    override fun onSeekComplete(mp: MediaPlayer) {
      onSeekCompleteListener?.onSeekComplete(mp)
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
      Log.d(ContentValues.TAG, "Error: $what,$extra")
      currentState = State.ERROR

      return onErrorListener?.onError(mediaPlayer, what, extra) == true
    }

    override fun onPrepared(mp: MediaPlayer) {
      currentState = State.PREPARED
      onPreparedListener?.onPrepared(mediaPlayer)

      // TODO: why does the ExoVideoPlayer not do this
      surface.updateVideoSize(mp.videoWidth, mp.videoHeight)

      if (requestedSeek != 0L) {
        seekTo(requestedSeek)
      }

      if (playRequested) {
        start()
      }
    }

    override fun onInfo(mp: MediaPlayer, what: Int, extra: Int): Boolean {
      return onInfoListener?.onInfo(mp, what, extra) == true
    }

    override fun onVideoSizeChanged(mp: MediaPlayer, width: Int, height: Int) {
      surface.updateVideoSize(mp.videoWidth, mp.videoHeight)
    }
  }
}