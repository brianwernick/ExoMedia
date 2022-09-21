package com.devbrackets.android.exomedia.nmp

import android.net.Uri
import android.os.PowerManager
import android.view.Surface
import androidx.annotation.IntRange
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.TrackGroupArray
import com.devbrackets.android.exomedia.core.listener.CaptionListener
import com.devbrackets.android.exomedia.core.listener.ExoPlayerListener
import com.devbrackets.android.exomedia.core.listener.MetadataListener
import com.devbrackets.android.exomedia.core.listener.VideoSizeListener
import com.devbrackets.android.exomedia.core.renderer.RendererType
import com.devbrackets.android.exomedia.core.state.PlaybackState
import com.devbrackets.android.exomedia.listener.OnBufferUpdateListener
import com.devbrackets.android.exomedia.nmp.manager.window.WindowInfo


interface ExoMediaPlayer {

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
   * Determines if media specified by [setMediaUri] or [setMediaSource] should
   * start playing when the content is ready (buffered)
   */
  var playWhenReady: Boolean

  /**
   * Returns the duration of the loaded media in milliseconds, if no media is loaded then
   * [androidx.media3.common.C.TIME_UNSET] will be returned.
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
   * Returns the information representing the current playback window
   */
  val windowInfo: WindowInfo?

  /**
   * Retrieves a list of available tracks
   *
   * @return A list of available tracks associated with each [RendererType]
   */
  val availableTracks: Map<RendererType, TrackGroupArray>?

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
   * The provider to retrieve the `DrmSessionManager` to play protected content
   */
  var drmSessionManagerProvider: DrmSessionManagerProvider?

  /**
   * The current media playback state
   */
  val playbackState: PlaybackState

  /**
   * The surface to play video media on
   */
  var surface: Surface?

  /**
   * Determines the current playback position in the loaded media in milliseconds.
   * If `limitToCurrentWindow` is `true` then the position in the current window will
   * be returned, otherwise the total position across all windows will be returned.
   * These should only be different if the media in playback has multiple windows
   * (e.g. in the case of using a `ConcatenatingMediaSource` with more than 1 source)
   *
   * @param limitToCurrentWindow If `true` the position within the current window will be returned
   * @return The current playback position in milliseconds
   */
  fun getCurrentPosition(limitToCurrentWindow: Boolean): Long

  /**
   * Defines the URI for media to play
   */
  fun setMediaUri(uri: Uri?)

  /**
   * Defines a [MediaSource] for the player to play
   */
  fun setMediaSource(source: MediaSource?)

  /**
   * Seeks the media playback to the specified position in milliseconds
   */
  fun seekTo(@IntRange(from = 0) positionMs: Long)

  /**
   * Seeks to the specified position in the media currently loaded specified by `positionMs`.
   * If `limitToCurrentWindow` is true then a seek won't be allowed to span across windows.
   * This should only be different if the media in playback has multiple windows (e.g. in the case of using a
   * `ConcatenatingMediaSource` with more than 1 source)
   *
   * @param positionMs The position to seek to in the media
   * @param limitToCurrentWindow `true` to only seek in the current window
   */
  fun seekTo(positionMs: Long, limitToCurrentWindow: Boolean)

  /**
   * Performs the functionality to prepare the media player for playback.
   * This should be called _after_ media has been specified via [setMediaUri]
   * or [setMediaSource].
   */
  fun prepare()

  /**
   * Indicates to the media player that the next call to [prepare] should
   * proceed with the initial preparation even if [prepare] the media player
   * has previously been prepared.
   */
  fun forcePrepare()

  /**
   * Starts or resumes playback of the media specified by [setMediaUri] or
   * [setMediaSource]
   */
  fun start()

  /**
   * Stops the media playback in a way that it can quickly be resumed (see [start])
   */
  fun pause()

  /**
   * Stops the media playback, clears any cached data, and disconnects from
   * the media specified by [setMediaUri] or [setMediaSource]
   */
  fun stop()

  /**
   * Moves the playback position of the media specified by [setMediaUri] or [setMediaSource]
   * to the start (`0`). This method will not succeed if playback state is not `
   * ExoPlayer.STATE_IDLE` or `ExoPlayer.STATE_ENDED`.
   *
   * @return `true` if the media was successfully restarted
   */
  fun restart(): Boolean

  /**
   * Releases any resources being held by this player. In order to play media after
   * a `release` a new player will need to be instantiated
   */
  fun release()

  /**
   * Sets the `trackIndex` within the `groupIndex` as active for the specified `type`. This is
   * useful to specify requested video quality, audio tracks (such as languages), etc.
   *
   * @param type The type of renderer to specify the selected track for
   * @param groupIndex The group index within the [availableTracks] that `trackIndex` is contained
   * @param trackIndex The index within the `groupIndex` to select
   */
  fun setSelectedTrack(type: RendererType, groupIndex: Int, trackIndex: Int)

  /**
   * Retrieves the active/selected track index for the specified [RendererType] and
   * `groupIndex`
   *
   * @param type The type of renderer to retrieve the selected track for
   * @param groupIndex The group index within the [availableTracks]
   */
  fun getSelectedTrackIndex(type: RendererType, groupIndex: Int): Int

  /**
   * Clear all selected tracks for the specified renderer
   *
   * @param type The type of renderer to clear the selection specified in [setSelectedTrack]
   */
  fun clearSelectedTracks(type: RendererType)

  /**
   * Enables or disables renderers for the specificed [RendererType]. This is
   * useful when enabling or disabling dynamic functionality such as Closed Captions
   *
   * @param type The type of renderer to enable or disable
   * @param enabled `true` if the player should enable renderers of `type`
   */
  fun setRendererEnabled(type: RendererType, enabled: Boolean)

  /**
   * Determines if at least 1 renderer is enabled for the specified [RendererType]
   *
   * @param type The type of renderer to determine if it's enabled
   * @return `true` if at least one renderer for the given type is enabled
   */
  fun isRendererEnabled(type: RendererType): Boolean

  /**
   * Clears the contents that were written to the [surface]
   */
  fun clearSurface()

  /**
   * Specifies the [C.StreamType] for the player
   */
  @Deprecated("Use setAudioAttributes instead")
  fun setAudioStreamType(@C.StreamType streamType: Int)

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

  /**
   * Specifies if and how the player will repeat media playback, by default
   * no repeat mode will be used.
   */
  fun setRepeatMode(@Player.RepeatMode repeatMode: Int)

  /**
   * Specifies the listener to be informed of [ExoMediaPlayer] events
   *
   * @param listener The listener to inform of updates or `null` to clear an existing listener
   */
  fun addListener(listener: ExoPlayerListener)

  /**
   * Removes the specified [ExoPlayerListener]
   *
   * @param listener The [ExoPlayerListener] to be removed
   */
  fun removeListener(listener: ExoPlayerListener)

  /**
   * Specifies the listener to be informed of Buffer updates
   *
   * @param listener The listener to inform of updates or `null` to clear an existing listener
   */
  fun setBufferUpdateListener(listener: OnBufferUpdateListener?)

  /**
   * Specifies the listener to be informed of Metadata updates
   *
   * @param listener The listener to inform of updates or `null` to clear an existing listener
   */
  fun setMetadataListener(listener: MetadataListener?)

  /**
   * Specifies the listener to be informed of Closed Caption updates
   *
   * @param listener The listener to inform of updates or `null` to clear an existing listener
   */
  fun setCaptionListener(listener: CaptionListener?)

  /**
   * Specifies the listener to be informed of video size changes
   *
   * @param listener The listener to inform of updates or `null` to clear an existing listener
   */
  fun setVideoSizeListener(listener: VideoSizeListener?)

  /**
   * Adds an [AnalyticsListener] to receive analytics events.
   *
   * @param listener The listener to be added.
   */
  fun addAnalyticsListener(listener: AnalyticsListener)

  /**
   * Removes an [AnalyticsListener].
   *
   * @param listener The [AnalyticsListener] to be removed.
   */
  fun removeAnalyticsListener(listener: AnalyticsListener)
}