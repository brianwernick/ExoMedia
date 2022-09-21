package com.devbrackets.android.exomedia.core.video

import android.view.Surface
import androidx.annotation.IntRange
import androidx.media3.common.AudioAttributes
import androidx.media3.common.Metadata
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.source.TrackGroupArray
import com.devbrackets.android.exomedia.core.ListenerMux
import com.devbrackets.android.exomedia.core.audio.MediaItem
import com.devbrackets.android.exomedia.core.listener.CaptionListener
import com.devbrackets.android.exomedia.core.listener.MetadataListener
import com.devbrackets.android.exomedia.core.listener.VideoSizeListener
import com.devbrackets.android.exomedia.core.renderer.RendererType
import com.devbrackets.android.exomedia.core.video.surface.SurfaceEnvelope
import com.devbrackets.android.exomedia.listener.OnBufferUpdateListener
import com.devbrackets.android.exomedia.nmp.ExoMediaPlayerImpl
import com.devbrackets.android.exomedia.nmp.config.PlayerConfig
import com.devbrackets.android.exomedia.nmp.manager.window.WindowInfo

class ExoVideoPlayer(
  private val playerConfig: PlayerConfig,
  private val surface: SurfaceEnvelope
) : VideoPlayerApi {
  val corePlayer: ExoMediaPlayerImpl by lazy {
    ExoMediaPlayerImpl(playerConfig).apply {
      setMetadataListener(internalListeners)
      setBufferUpdateListener(internalListeners)
      setVideoSizeListener(internalListeners)
    }
  }

  var mux: ListenerMux? = null

  protected var playRequested = false

  protected var internalListeners = InternalListeners()
  protected val surfaceCallback = SurfaceCallback()

  override var volume: Float
    get() = corePlayer.volume
    set(value) {
      corePlayer.volume = value
    }

  override val isPlaying: Boolean
    get() = corePlayer.playWhenReady

  override val duration: Long
    get() = if (!mux!!.isPrepared) {
      0
    } else corePlayer.duration

  override val currentPosition: Long
    get() = if (!mux!!.isPrepared) {
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
    surface.addCallback(surfaceCallback)
    surface.setVideoSize(0, 0)
  }

  override fun setMedia(mediaItem: MediaItem?) {
    //Makes sure the listeners get the onPrepared callback
    mux?.setNotifiedPrepared(false)
    corePlayer.seekTo(0)

    mediaItem?.mediaSource?.let {
      corePlayer.setMediaSource(it)
      mux?.setNotifiedCompleted(false)
      corePlayer.prepare()
      return
    }

    mediaItem?.uri?.let {
      corePlayer.setMediaUri(it)
      mux?.setNotifiedCompleted(false)
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
    mux?.setNotifiedPrepared(false)
    mux?.setNotifiedCompleted(false)

    return true
  }

  override fun release() {
    corePlayer.release()
    playRequested = false
    surface.removeCallback(surfaceCallback)
  }

  override fun seekTo(@IntRange(from = 0) milliseconds: Long) {
    corePlayer.seekTo(milliseconds)
  }

  override fun setWakeLevel(levelAndFlags: Int) {
    corePlayer.setWakeLevel(levelAndFlags)
  }

  override fun start() {
    corePlayer.playWhenReady = true
    mux?.setNotifiedCompleted(false)
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
      mux?.clearSurfaceWhenReady(surface)
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

  override fun setAudioAttributes(attributes: AudioAttributes) {
    corePlayer.setAudioAttributes(attributes)
  }

  override fun setRepeatMode(repeatMode: Int) {
    corePlayer.setRepeatMode(repeatMode)
  }

  fun onSurfaceReady(surface: Surface?) {
    corePlayer.surface = surface
    if (playRequested && surface != null) {
      corePlayer.playWhenReady = true
    }
  }

  fun onSurfaceDestroyed() {
    corePlayer.clearSurface()
  }

  override fun setListenerMux(listenerMux: ListenerMux) {
    this.mux?.let { oldListenerMux ->
      corePlayer.removeListener(oldListenerMux)
      corePlayer.removeAnalyticsListener(oldListenerMux)
    }

    this.mux = listenerMux
    corePlayer.addListener(listenerMux)
    corePlayer.addAnalyticsListener(listenerMux)
  }

  protected inner class InternalListeners : MetadataListener, OnBufferUpdateListener, VideoSizeListener {
    override fun onMetadata(metadata: Metadata) {
      mux?.onMetadata(metadata)
    }

    override fun onBufferingUpdate(@IntRange(from = 0, to = 100) percent: Int) {
      mux?.onBufferingUpdate(percent)
    }

    override fun onVideoSizeChanged(videoSize: VideoSize) {
      mux?.onVideoSizeChanged(videoSize.width, videoSize.height, videoSize.unappliedRotationDegrees, videoSize.pixelWidthHeightRatio)
    }
  }

  protected inner class SurfaceCallback: SurfaceEnvelope.Callback {
    override fun onSurfaceAvailable(envelope: SurfaceEnvelope) {
      onSurfaceReady(envelope.getSurface())
    }

    override fun onSurfaceDestroyed(envelope: SurfaceEnvelope) {
      onSurfaceDestroyed()
      envelope.release()
    }

    override fun onSurfaceSizeChanged(envelope: SurfaceEnvelope, width: Int, height: Int) {
      // When working with the ExoPlayer we need to include the width:height ratio when updating
      // the video size, because we don't have that ratio here we will let the VideoSizeListener
      // handle notifying the [surface] of the update (No-Op here)
    }
  }
}