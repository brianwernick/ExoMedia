package com.devbrackets.android.exomedia

import android.content.Context
import android.net.Uri
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.MediaSource
import com.devbrackets.android.exomedia.core.ListenerMux
import com.devbrackets.android.exomedia.core.audio.AudioPlayerApi
import com.devbrackets.android.exomedia.core.audio.ExoAudioPlayer
import com.devbrackets.android.exomedia.core.audio.MediaItem
import com.devbrackets.android.exomedia.core.listener.MetadataListener
import com.devbrackets.android.exomedia.core.state.PlaybackState
import com.devbrackets.android.exomedia.core.state.PlaybackStateListener
import com.devbrackets.android.exomedia.listener.*
import com.devbrackets.android.exomedia.nmp.ExoMediaPlayer
import com.devbrackets.android.exomedia.nmp.config.PlayerConfig
import com.devbrackets.android.exomedia.nmp.config.PlayerConfigBuilder

/**
 * An AudioPlayer that uses the ExoPlayer as the backing implementation. If the
 * current device does *NOT* support the ExoPlayer then the AudioPlayer will
 * fallback to using the OS MediaPlayer.
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
open class AudioPlayer(protected val audioPlayerImpl: AudioPlayerApi) : AudioPlayerApi by audioPlayerImpl {
  companion object {
    fun getPlayerImplementation(config: PlayerConfig): AudioPlayerApi {
      return if(config.fallbackManager.useFallback()) {
        config.fallbackManager.getFallbackAudioPlayer(config.context)
      } else {
        ExoAudioPlayer(config)
      }
    }
  }

  protected val listenerMux = ListenerMux(MuxNotifier())

  protected var overriddenDuration: Long = -1

  constructor(context: Context): this(PlayerConfigBuilder(context).build())
  constructor(config: PlayerConfig) : this(getPlayerImplementation(config))

  init {
    audioPlayerImpl.setListenerMux(listenerMux)
  }

  /**
   * Retrieves the duration of the current audio item. This should only be called after
   * the item is prepared (see [.setOnPreparedListener]).
   * If [.overrideDuration] is set then that value will be returned.
   *
   * @return The millisecond duration of the video
   */
  override val duration: Long
    get() = if (overriddenDuration >= 0) {
      overriddenDuration
    } else audioPlayerImpl.duration

  /**
   * Retrieves the current buffer percent of the audio item. If an audio item is not currently
   * prepared or buffering the value will be 0.  This should only be called after the audio item is
   * prepared (see [.setOnPreparedListener])
   *
   * @return The integer percent that is buffered [0, 100] inclusive
   */
  val bufferPercentage: Int
    get() = audioPlayerImpl.bufferedPercent

  /**
   * Gets the [MediaItem] currently used. This is specified by calling [setMedia]
   */
  var mediaItem: MediaItem? = null
    protected set

  /**
   * Sets the [Uri] to play the audio from
   *
   * @param uri The video's [Uri]
   */
  fun setMedia(uri: Uri?) {
    val mediaItem = uri?.let {
      MediaItem(it, null)
    }

    setMedia(mediaItem)
  }

  /**
   * Sets the [MediaSource] to play the audio from
   *
   * @param mediaSource [MediaSource] that should be used
   */
  fun setMedia(mediaSource: MediaSource?) {
    val mediaItem = mediaSource?.let {
      MediaItem(null, it)
    }

    setMedia(mediaItem)
  }

  override fun setMedia(mediaItem: MediaItem?) {
    audioPlayerImpl.setMedia(mediaItem)

    this.mediaItem = mediaItem
    overrideDuration(-1)
  }

  /**
   * Stops the current audio playback and resets the listener states
   * so that we receive the callbacks for events like onPrepared
   */
  override fun reset() {
    stop()
    setMedia(mediaItem = null)

    audioPlayerImpl.reset()
  }

  /**
   * Setting this will override the duration that the item may actually be. This method should
   * only be used when the item doesn't return the correct duration such as with audio streams.
   * This only overrides the current audio item.
   *
   * @param duration The duration for the current media item or &lt; 0 to disable
   */
  fun overrideDuration(duration: Long) {
    overriddenDuration = duration
  }

  /**
   * Sets the listener to inform of VideoPlayer prepared events
   *
   * @param listener The listener
   */
  fun setOnPreparedListener(listener: OnPreparedListener?) {
    listenerMux.setOnPreparedListener(listener)
  }

  /**
   * Sets the listener to inform of VideoPlayer completion events
   *
   * @param listener The listener
   */
  fun setOnCompletionListener(listener: OnCompletionListener?) {
    listenerMux.setOnCompletionListener(listener)
  }

  /**
   * Sets the listener to inform of VideoPlayer buffer update events
   *
   * @param listener The listener
   */
  fun setOnBufferUpdateListener(listener: OnBufferUpdateListener?) {
    listenerMux.setOnBufferUpdateListener(listener)
  }

  /**
   * Sets the listener to inform of VideoPlayer seek completion events
   *
   * @param listener The listener
   */
  fun setOnSeekCompletionListener(listener: OnSeekCompletionListener?) {
    listenerMux.setOnSeekCompletionListener(listener)
  }

  /**
   * Sets the listener to inform of playback errors
   *
   * @param listener The listener
   */
  fun setOnErrorListener(listener: OnErrorListener?) {
    listenerMux.setOnErrorListener(listener)
  }

  /**
   * Sets the listener to inform of ID3 metadata updates
   *
   * @param listener The listener to inform
   */
  fun setMetadataListener(listener: MetadataListener?) {
    listenerMux.setMetadataListener(listener)
  }

  /**
   * Sets the listener to inform of Analytics updates
   *
   * @param listener The listener to inform
   */
  fun setAnalyticsListener(listener: AnalyticsListener?) {
    listenerMux.setAnalyticsListener(listener)
  }

  /**
   * Sets the listener to inform of playback state changes. If only the current value
   * is needed then [getPlaybackState] can be used.
   *
   * @param listener The listener to inform of [PlaybackState] changes
   */
  fun setPlaybackStateListener(listener: PlaybackStateListener?) {
    listenerMux.setPlaybackStateListener(listener)
  }

  /**
   * Retrieves the current [PlaybackState] of this [AudioPlayer]. Changes to this value
   * can also be listened to via the [setPlaybackStateListener].
   *
   * @return The current [PlaybackState] of this [AudioPlayer]
   */
  fun getPlaybackState(): PlaybackState {
    return listenerMux.playbackState
  }

  /**
   * Performs the functionality to stop the progress polling, and stop any other
   * procedures from running that we no longer need.
   */
  private fun onPlaybackEnded() {
    pause()
  }

  private inner class MuxNotifier : ListenerMux.Notifier() {
    override fun onExoPlayerError(exoMediaPlayer: ExoMediaPlayer, e: Exception?) {
      stop()
      exoMediaPlayer.forcePrepare()
    }

    override fun onMediaPlaybackEnded() {
      onPlaybackEnded()
    }
  }
}
