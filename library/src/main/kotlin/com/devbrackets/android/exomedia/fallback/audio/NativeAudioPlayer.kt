package com.devbrackets.android.exomedia.fallback.audio

import android.content.Context
import android.net.Uri
import androidx.annotation.IntRange
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.TrackGroupArray
import com.devbrackets.android.exomedia.core.ListenerMux
import com.devbrackets.android.exomedia.core.audio.AudioPlayerApi
import com.devbrackets.android.exomedia.core.audio.MediaItem
import com.devbrackets.android.exomedia.core.renderer.RendererType
import com.devbrackets.android.exomedia.fallback.FallbackMediaPlayer
import com.devbrackets.android.exomedia.fallback.FallbackMediaPlayerImpl
import com.devbrackets.android.exomedia.nmp.manager.window.WindowInfo

/**
 * A simple [AudioPlayerApi] implementation that uses the fallback (system) MediaPlayer
 * to add integration with the [ListenerMux] and to mitigate state errors.
 */
class NativeAudioPlayer(context: Context) : AudioPlayerApi {
  private val mediaPlayer: FallbackMediaPlayer by lazy {
    FallbackMediaPlayerImpl(context).apply {
      setAudioAttributes(getAudioAttributes(C.USAGE_MEDIA, C.AUDIO_CONTENT_TYPE_MUSIC))
    }
  }

  private var _listenerMux: ListenerMux? = null

  override val bufferedPercent = mediaPlayer.bufferedPercent

  override var volume: Float
    get() = mediaPlayer.volume
    set(value) {
      mediaPlayer.volume = value
    }

  override val isPlaying: Boolean
    get() = mediaPlayer.playing

  override val duration: Long
    get() = mediaPlayer.duration

  override val currentPosition: Long
    get() = mediaPlayer.currentPosition

  override val windowInfo: WindowInfo?
    get() = null

  override val audioSessionId: Int
    get() = mediaPlayer.audioSessionId

  override val playbackSpeed: Float
    get() = mediaPlayer.playbackSpeed

  override val availableTracks: Map<RendererType, TrackGroupArray>?
    get() = null

  override var drmSessionManagerProvider: DrmSessionManagerProvider?
    get() = null
    set(_) {}

  override fun setMedia(mediaItem: MediaItem?) {
    mediaPlayer.setMedia(mediaItem?.uri)

    //Makes sure the listeners get the onPrepared callback
    _listenerMux?.setNotifiedPrepared(false)
    _listenerMux?.setNotifiedCompleted(false)
  }

  override fun reset() {
    mediaPlayer.reset()
  }

  override fun seekTo(@IntRange(from = 0) milliseconds: Long) {
    mediaPlayer.seekTo(milliseconds)
  }

  override fun start() {
    mediaPlayer.start()
    _listenerMux?.setNotifiedCompleted(false)
  }

  override fun pause() {
    mediaPlayer.pause()
  }

  override fun stop() {
    mediaPlayer.stop()
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

  override fun release() {
    mediaPlayer.release()
  }

  override fun setPlaybackSpeed(speed: Float): Boolean {
    mediaPlayer.playbackSpeed = speed
    return true
  }

  override fun setAudioAttributes(attributes: AudioAttributes) {
    mediaPlayer.setAudioAttributes(attributes)
  }

  override fun setWakeLevel(levelAndFlags: Int) {
    mediaPlayer.setWakeLevel(levelAndFlags)
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

  override fun setListenerMux(listenerMux: ListenerMux) {
    _listenerMux = listenerMux
    mediaPlayer.setListener(listenerMux)
  }

  override fun setRepeatMode(@Player.RepeatMode repeatMode: Int) {
    // Purposefully left blank
  }

  @Suppress("SameParameterValue")
  private fun getAudioAttributes(@C.AudioUsage usage: Int, @C.AudioContentType contentType: Int): AudioAttributes {
    return AudioAttributes.Builder()
      .setUsage(usage)
      .setContentType(contentType)
      .build()
  }
}
