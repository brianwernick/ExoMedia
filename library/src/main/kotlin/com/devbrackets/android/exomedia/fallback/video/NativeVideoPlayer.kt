package com.devbrackets.android.exomedia.fallback.video

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.source.TrackGroupArray
import com.devbrackets.android.exomedia.core.ListenerMux
import com.devbrackets.android.exomedia.core.audio.MediaItem
import com.devbrackets.android.exomedia.core.listener.CaptionListener
import com.devbrackets.android.exomedia.core.renderer.RendererType
import com.devbrackets.android.exomedia.core.video.VideoPlayerApi
import com.devbrackets.android.exomedia.core.video.surface.SurfaceEnvelope
import com.devbrackets.android.exomedia.fallback.FallbackMediaPlayer
import com.devbrackets.android.exomedia.fallback.FallbackMediaPlayerImpl
import com.devbrackets.android.exomedia.nmp.manager.window.WindowInfo

class NativeVideoPlayer(
  private var context: Context,
  private val surface: SurfaceEnvelope
) : VideoPlayerApi {

  private val mediaPlayer: FallbackMediaPlayer by lazy {
    FallbackMediaPlayerImpl(context).apply {
      setAudioAttributes(getAudioAttributes(C.USAGE_MEDIA, C.AUDIO_CONTENT_TYPE_MOVIE))
    }
  }

  private var _listenerMux: ListenerMux? = null

  private var surfaceCallback = SurfaceCallback()

  override var volume: Float
    get() = mediaPlayer.volume
    set(value) {
      mediaPlayer.volume = value
    }

  override val duration: Long
    get() = mediaPlayer.duration

  override val currentPosition: Long
    get() = mediaPlayer.currentPosition

  override val isPlaying: Boolean
    get() = mediaPlayer.playing

  override val bufferedPercent: Int
    get() = mediaPlayer.bufferedPercent

  override var drmSessionManagerProvider: DrmSessionManagerProvider?
    get() = null
    set(_) {}

  override val audioSessionId: Int
    get() = mediaPlayer.audioSessionId

  override val availableTracks: Map<RendererType, TrackGroupArray>?
    get() = null

  override val windowInfo: WindowInfo?
    get() = null

  override val playbackSpeed: Float
    get() = mediaPlayer.playbackSpeed

  init {
    surface.addCallback(surfaceCallback)
  }

  override fun setListenerMux(listenerMux: ListenerMux) {
    _listenerMux = listenerMux
    mediaPlayer.setListener(listenerMux)
  }

  override fun start() {
    mediaPlayer.start()
    _listenerMux?.setNotifiedCompleted(false)
  }

  override fun pause() {
    mediaPlayer.pause()
  }

  override fun seekTo(milliseconds: Long) {
    mediaPlayer.seekTo(milliseconds)
  }

  override fun setPlaybackSpeed(speed: Float): Boolean {
    mediaPlayer.playbackSpeed = speed
    return true
  }

  override fun stop() {
    stop(false)
  }

  override fun setCaptionListener(listener: CaptionListener?) {
    // Not Supported
  }

  override fun stop(clearSurface: Boolean) {
    mediaPlayer.stop()
    if (clearSurface) {
      _listenerMux?.clearSurfaceWhenReady(surface)
    }
  }

  /**
   * Cleans up the resources being held. This should only be called when
   * destroying the video view
   */
  override fun release() {
    mediaPlayer.release()
    surface.removeCallback(surfaceCallback)
  }

  override fun reset() {
    mediaPlayer.reset()
  }

  override fun restart(): Boolean {
    if (!mediaPlayer.restart()) {
      return false
    }

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

  override fun setAudioAttributes(attributes: AudioAttributes) {
    mediaPlayer.setAudioAttributes(attributes)
  }

  override fun setWakeLevel(levelAndFlags: Int) {
    mediaPlayer.setWakeLevel(levelAndFlags)
  }

  override fun setRepeatMode(repeatMode: Int) {
    // Not Supported
  }

  override fun setMedia(mediaItem: MediaItem?) {
    mediaPlayer.setMedia(mediaItem?.uri)

    //Makes sure the listeners get the onPrepared callback
    _listenerMux?.setNotifiedPrepared(false)
  }

  @Suppress("SameParameterValue")
  private fun getAudioAttributes(@C.AudioUsage usage: Int, @C.AudioContentType contentType: Int): AudioAttributes {
    return AudioAttributes.Builder()
      .setUsage(usage)
      .setContentType(contentType)
      .build()
  }

  private inner class SurfaceCallback : SurfaceEnvelope.Callback {
    override fun onSurfaceAvailable(envelope: SurfaceEnvelope) {
      mediaPlayer.surface = envelope.getSurface()

      if (mediaPlayer.playWhenReady) {
        mediaPlayer.start()
      }
    }

    override fun onSurfaceDestroyed(envelope: SurfaceEnvelope) {
      envelope.release()
      release()
    }

    override fun onSurfaceSizeChanged(envelope: SurfaceEnvelope, width: Int, height: Int) {
      if (width > 0 && height > 0) {
        start()
      }
    }
  }
}