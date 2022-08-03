package com.devbrackets.android.exomedia.fallback

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.Surface
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.media3.common.AudioAttributes
import com.devbrackets.android.exomedia.core.state.PlaybackState
import java.io.IOException

class FallbackMediaPlayerImpl(
  private val context: Context
):
  FallbackMediaPlayer,
  MediaPlayer.OnBufferingUpdateListener,
  MediaPlayer.OnErrorListener,
  MediaPlayer.OnPreparedListener,
  MediaPlayer.OnCompletionListener,
  MediaPlayer.OnSeekCompleteListener,
  MediaPlayer.OnInfoListener,
  MediaPlayer.OnVideoSizeChangedListener
{
  companion object {
    private const val TAG = "FallbackMediaPlayerImpl"

    // States representing we have access to media information / control
    private val accessibleStates = setOf(
      PlaybackState.BUFFERING,
      PlaybackState.SEEKING,
      PlaybackState.READY,
      PlaybackState.PLAYING,
      PlaybackState.PAUSED
    )
  }


  private val mediaPlayer: MediaPlayer by lazy {
    MediaPlayer().apply {
      setOnInfoListener(this@FallbackMediaPlayerImpl)
      setOnErrorListener(this@FallbackMediaPlayerImpl)
      setOnPreparedListener(this@FallbackMediaPlayerImpl)
      setOnCompletionListener(this@FallbackMediaPlayerImpl)
      setOnSeekCompleteListener(this@FallbackMediaPlayerImpl)
      setOnBufferingUpdateListener(this@FallbackMediaPlayerImpl)
      setOnVideoSizeChangedListener(this@FallbackMediaPlayerImpl)
    }
  }

  override var playWhenReady = false

  private var prepared: Boolean = false
  private var requestedSeek: Long = 0
  private var currentBufferPercent: Int = 0

  @FloatRange(from = 0.0, to = 1.0)
  private var requestedVolume = 1.0f

  private var listener: FallbackMediaPlayer.Listener? = null

  private var headers: Map<String, String>? = null

  override var volume: Float
    get() = requestedVolume
    set(value) {
      requestedVolume = value
      mediaPlayer.setVolume(value, value)
    }

  override val duration: Long
    get() = if (!prepared || !mediaAccessible) {
      0
    } else mediaPlayer.duration.toLong()

  override val currentPosition: Long
    get() = if (!prepared || !mediaAccessible) {
      0
    } else mediaPlayer.currentPosition.toLong()

  override val playing: Boolean
    get() = mediaAccessible && mediaPlayer.isPlaying

  override val bufferedPercent: Int
    get() = currentBufferPercent

  override val audioSessionId: Int
    get() = mediaPlayer.audioSessionId

  override var playbackSpeed: Float
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      mediaPlayer.playbackParams.speed
    } else 1f
    set(value) {
      updatePlaybackParams(value, playbackPitch)
    }

  override var playbackPitch: Float
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      mediaPlayer.playbackParams.pitch
    } else 1f
    set(value) {
      updatePlaybackParams(playbackSpeed, value)
    }

  override var playbackState = PlaybackState.IDLE
    private set

  private val mediaAccessible: Boolean
    get() = accessibleStates.contains(playbackState)

  override var surface: Surface? = null
    set(value) {
      field = value
      mediaPlayer.setSurface(surface)
    }

  override fun seekTo(@IntRange(from = 0) positionMs: Long) {
    if (!mediaAccessible) {
      requestedSeek = positionMs
      return
    }

    reportPlaybackState(PlaybackState.SEEKING)
    mediaPlayer.seekTo(positionMs.toInt())
    requestedSeek = 0
  }

  override fun start() {
    if (mediaAccessible) {
      mediaPlayer.start()
      reportPlaybackState(PlaybackState.PLAYING)
    }

    playWhenReady = true
  }

  override fun pause() {
    if (mediaAccessible && mediaPlayer.isPlaying) {
      mediaPlayer.pause()
      reportPlaybackState(PlaybackState.PAUSED)
    }

    playWhenReady = false
  }

  override fun stop() {
    tryWhenAccessible {
      mediaPlayer.stop()
    }

    prepared = false
    playWhenReady = false
    reportPlaybackState(PlaybackState.STOPPED)
  }

  override fun restart(): Boolean {
    if (!prepared || !(accessibleStates.contains(playbackState) || playbackState == PlaybackState.COMPLETED)) {
      return false
    }

    seekTo(0)
    start()

    return true
  }

  override fun reset() {
    mediaPlayer.reset()

    prepared = false
    playWhenReady = false
    reportPlaybackState(PlaybackState.IDLE)
  }

  override fun release() {
    tryWhenAccessible {
      mediaPlayer.reset()
      mediaPlayer.release()
    }

    prepared = false
    playWhenReady = false
    reportPlaybackState(PlaybackState.RELEASED)
  }

  override fun setAudioAttributes(attributes: AudioAttributes) {
    mediaPlayer.setAudioAttributes(attributes.audioAttributesV21.audioAttributes)
  }

  override fun setWakeLevel(levelAndFlags: Int) {
    mediaPlayer.setWakeMode(context, levelAndFlags)
  }

  override fun setMedia(uri: Uri?) {
    prepared = false
    if (uri == null) {
      return
    }

    currentBufferPercent = 0

    try {
      mediaPlayer.reset()
      mediaPlayer.setDataSource(context.applicationContext, uri, headers)
      mediaPlayer.prepareAsync()

      reportPlaybackState(PlaybackState.PREPARING)
    } catch (ex: IOException) {
      Log.w(TAG, "Unable to open content: $uri", ex)
      reportPlaybackState(PlaybackState.ERROR)

      listener?.onError(this, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0)
    } catch (ex: IllegalArgumentException) {
      Log.w(TAG, "Unable to open content: $uri", ex)
      reportPlaybackState(PlaybackState.ERROR)
      listener?.onError(this, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0)
    }
  }

  override fun setListener(listener: FallbackMediaPlayer.Listener?) {
    this.listener = listener
  }

  override fun onBufferingUpdate(mp: MediaPlayer, percent: Int) {
    currentBufferPercent = percent
    listener?.onBufferUpdate(this, percent)
  }

  override fun onCompletion(mp: MediaPlayer) {
    reportPlaybackState(PlaybackState.COMPLETED)
  }

  override fun onSeekComplete(mp: MediaPlayer) {
    listener?.onSeekComplete(this)

    if (playWhenReady) {
      start()
    } else if (prepared) {
      reportPlaybackState(PlaybackState.PAUSED)
    }
  }

  override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
    reportPlaybackState(PlaybackState.ERROR)

    return listener?.onError(this, what, extra) == true
  }

  override fun onPrepared(mp: MediaPlayer) {
    prepared = true
    reportPlaybackState(PlaybackState.READY)
    listener?.onVideoSizeChanged(this, mp.videoWidth, mp.videoHeight)

    if (requestedSeek != 0L) {
      seekTo(requestedSeek)
    } else if (playWhenReady) {
      start()
    }
  }

  override fun onInfo(mp: MediaPlayer, what: Int, extra: Int): Boolean {
    handleMediaInfo(what)
    return false
  }

  override fun onVideoSizeChanged(mp: MediaPlayer, width: Int, height: Int) {
    listener?.onVideoSizeChanged(this, mp.videoWidth, mp.videoHeight)
  }

  private fun reportPlaybackState(state: PlaybackState) {
    playbackState = state
    listener?.onPlaybackStateChange(state)
  }

  private fun updatePlaybackParams(speed: Float, pitch: Float) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      return
    }

    mediaPlayer.playbackParams = PlaybackParams().apply {
      this.speed = speed
      this.pitch = pitch
    }
  }

  private fun tryWhenAccessible(action: () -> Unit) {
    if (!mediaAccessible) {
      return
    }

    try {
      action()
    } catch (e: Exception) {
      Log.d(TAG, "error calling action()", e)
    }
  }

  private fun handleMediaInfo(mediaInfo: Int) {
    if (mediaInfo == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
      reportPlaybackState(PlaybackState.BUFFERING)
      return
    }

    if (mediaInfo != MediaPlayer.MEDIA_INFO_BUFFERING_END) {
      return
    }

    // Double-Check if the media is actually playing (resumed on buffer completion)
    if (playing) {
      reportPlaybackState(PlaybackState.PLAYING)
      return
    }

    if (playWhenReady) {
      start()
      return
    }

    // We assume that the media was already playing when buffering ends
    reportPlaybackState(PlaybackState.PAUSED)
  }
}