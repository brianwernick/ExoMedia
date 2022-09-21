package com.devbrackets.android.exomedia.core.audio

import androidx.annotation.IntRange
import androidx.media3.common.AudioAttributes
import androidx.media3.common.Metadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.source.TrackGroupArray
import com.devbrackets.android.exomedia.core.ListenerMux
import com.devbrackets.android.exomedia.core.listener.MetadataListener
import com.devbrackets.android.exomedia.core.renderer.RendererType
import com.devbrackets.android.exomedia.listener.OnBufferUpdateListener
import com.devbrackets.android.exomedia.nmp.ExoMediaPlayerImpl
import com.devbrackets.android.exomedia.nmp.config.PlayerConfig
import com.devbrackets.android.exomedia.nmp.manager.window.WindowInfo

/**
 * A [AudioPlayerApi] implementation that uses the ExoPlayer
 * as the backing media player.
 */
open class ExoAudioPlayer(protected val config: PlayerConfig) : AudioPlayerApi {
  protected val corePlayer: ExoMediaPlayerImpl = ExoMediaPlayerImpl(config)

  protected var mux: ListenerMux? = null

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
    //Purposefully left blank
  }

  override fun seekTo(@IntRange(from = 0) milliseconds: Long) {
    corePlayer.seekTo(milliseconds)
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

    mux?.setNotifiedCompleted(false)
    mux?.setNotifiedPrepared(false)

    return true
  }

  override fun release() {
    corePlayer.release()
  }

  override fun setPlaybackSpeed(speed: Float): Boolean {
    corePlayer.playbackSpeed = speed
    return true
  }

  override fun setAudioAttributes(attributes: AudioAttributes) {
    corePlayer.setAudioAttributes(attributes)
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
    this.mux?.let { oldListenerMux ->
      corePlayer.removeListener(oldListenerMux)
      corePlayer.removeAnalyticsListener(oldListenerMux)
    }

    this.mux = listenerMux
    corePlayer.addListener(listenerMux)
    corePlayer.addAnalyticsListener(listenerMux)
  }

  override fun setRepeatMode(@Player.RepeatMode repeatMode: Int) {
    corePlayer.setRepeatMode(repeatMode)
  }

  protected inner class InternalListeners : MetadataListener, OnBufferUpdateListener {
    override fun onMetadata(metadata: Metadata) {
      mux?.onMetadata(metadata)
    }

    override fun onBufferingUpdate(@IntRange(from = 0, to = 100) percent: Int) {
      mux?.onBufferingUpdate(percent)
    }
  }
}