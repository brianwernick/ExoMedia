package com.devbrackets.android.exomedia.fallback

import android.net.Uri
import android.os.PowerManager
import android.view.Surface
import androidx.annotation.IntRange
import androidx.media3.common.AudioAttributes
import com.devbrackets.android.exomedia.core.state.PlaybackState
import com.devbrackets.android.exomedia.core.state.PlaybackStateListener

interface FallbackMediaPlayer {

  /**
   * Represents the session ID for the audio tracks.
   */
  val audioSessionId: Int

  /**
   * The current volume for the Audio tracks. This will be between `0.0` and `1.0`
   * inclusive
   */
  var volume: Float

  /**
   * Returns `true` if any media is currently playing
   */
  val playing: Boolean

  /**
   * Determines if media specified by [setMedia] should
   * start playing when the content is ready (buffered)
   */
  var playWhenReady: Boolean

  /**
   * Returns the duration of the loaded media in milliseconds, if no media is loaded then
   * `0` will be returned.
   */
  val duration: Long

  /**
   * Returns the playback position in milliseconds
   */
  val currentPosition: Long

  /**
   * Returns an estimated percentage of the loaded media that has been buffered,
   * this will be a value between `0` and `100`. If no media is loaded then `0` will
   * be returned
   */
  val bufferedPercent: Int

  /**
   * The speed of playback for the media; must be greater than `0` and between
   * `0` and `1`
   */
  var playbackSpeed: Float

  /**
   * The correction to apply for the pitch. It's recommended to set the pitch correction
   * to the same amount as the `playbackSpeed`
   */
  var playbackPitch: Float

  /**
   * The current media playback state
   */
  val playbackState: PlaybackState

  /**
   * The surface to play video media on
   */
  var surface: Surface?

  /**
   * Defines the [Uri] for media to play
   */
  fun setMedia(uri: Uri?)

  /**
   * Seeks the media playback to the specified position in milliseconds
   */
  fun seekTo(@IntRange(from = 0) positionMs: Long)

  /**
   * Starts or resumes playback of the media specified by [setMedia]
   */
  fun start()

  /**
   * Stops the media playback in a way that it can quickly be resumed (see [start])
   */
  fun pause()

  /**
   * Stops the media playback, clears any cached data, and disconnects from
   * the media specified by [setMedia]
   */
  fun stop()

  /**
   * Moves the playback position of the media specified by [setMedia]
   * to the start (`0`).
   *
   * @return `true` if the media was successfully restarted
   */
  fun restart(): Boolean

  /**
   * Stops the current media playback and resets the listener states
   * so that we receive the callbacks for events like onPrepared
   */
  fun reset()

  /**
   * Releases any resources being held by this player. In order to play media after
   * a `release` a new player will need to be instantiated
   */
  fun release()

  fun setAudioAttributes(attributes: AudioAttributes)

  /**
   * This function has the MediaPlayer access the low-level power manager
   * service to control the device's power usage while playing is occurring.
   * The parameter is a combination of [android.os.PowerManager] wake flags.
   *
   * Use of this method requires [android.Manifest.permission.WAKE_LOCK]
   * permission.
   *
   * By default, no attempt is made to keep the device awake during playback.
   *
   * @param levelAndFlags The wake lock level and any flags to apply, see [PowerManager.newWakeLock]
   */
  fun setWakeLevel(levelAndFlags: Int)

  fun setListener(listener: Listener?)

  interface Listener: PlaybackStateListener {
    fun onBufferUpdate(mediaPlayer: FallbackMediaPlayer, percent: Int)

    fun onSeekComplete(mediaPlayer: FallbackMediaPlayer)

    fun onError(mediaPlayer: FallbackMediaPlayer, what: Int, extra: Int): Boolean

    fun onVideoSizeChanged(mediaPlayer: FallbackMediaPlayer, width: Int, height: Int)
  }
}