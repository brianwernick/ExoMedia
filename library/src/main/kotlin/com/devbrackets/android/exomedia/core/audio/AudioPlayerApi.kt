package com.devbrackets.android.exomedia.core.audio

import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.media3.common.AudioAttributes
import androidx.media3.common.Player
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.source.TrackGroupArray
import com.devbrackets.android.exomedia.core.ListenerMux
import com.devbrackets.android.exomedia.core.renderer.RendererType
import com.devbrackets.android.exomedia.nmp.manager.window.WindowInfo

/**
 * The basic APIs expected in the backing media player
 * implementations to allow us to create an abstraction
 * between the Native (Android) MediaPlayer and the AudioPlayer
 * using the ExoPlayer.
 */
interface AudioPlayerApi {

  /**
   * Returns if a media item is currently in playback
   *
   * @return True if a media item is playing
   */
  val isPlaying: Boolean

  /**
   * Retrieves the duration of the current audio item.
   *
   * @return The millisecond duration of the video
   */
  @get:IntRange(from = 0)
  val duration: Long

  /**
   * Retrieves the current position of the audio playback.  If an audio item is not currently
   * in playback then the value will be 0.
   *
   * @return The millisecond value for the current position
   */
  @get:IntRange(from = 0)
  val currentPosition: Long

  /**
   * Retrieves the current percent [0, 100] of the media that is buffered
   */
  @get:IntRange(from = 0, to = 100)
  val bufferedPercent: Int

  /**
   * Retrieves the information associated with the current [androidx.media3.common.Timeline.Window]
   * used by the ExoPlayer backed implementation. When the [android.media.MediaPlayer] backed
   * implementation is being used this will be null.
   *
   * @return The current Window information or null
   */
  val windowInfo: WindowInfo?

  /**
   * Returns the audio session ID.
   *
   * @return the audio session ID.
   * Note that the audio session ID is 0 only if a problem occurred when the AudioPlayer was constructed or the audio stream hasn't been
   * instantiated.
   */
  val audioSessionId: Int

  /**
   * Retrieves the current speed the media is playing at.
   *
   * @return The current playback speed
   */
  val playbackSpeed: Float

  /**
   * Retrieves a list of available tracks to select from.  Typically [.trackSelectionAvailable]
   * should be called before this.
   *
   * @return A list of available tracks associated with each track type
   */
  val availableTracks: Map<RendererType, TrackGroupArray>?

  /**
   * The volume of the audio playback, between [`0.0`, `1.0`]
   */
  @get:FloatRange(from = 0.0, to = 1.0)
  var volume: Float

  var drmSessionManagerProvider: DrmSessionManagerProvider?

  /**
   * Sets the source for the media item. The uri can be a web address (e.g. https://) or
   * an absolute local path (e.g. file://)
   *
   * @param mediaItem The [MediaItem] that specifies the media for playback. If both the uri and mediaSource
   *                  are defined then the mediaSource will take precedence when supported.
   */
  fun setMedia(mediaItem: MediaItem?)

  /**
   * Starts the playback for the media specified in [setMedia].
   * This should be called after the Player is correctly prepared (see [setMedia])
   */
  fun start()

  /**
   * If an audio item is currently in playback, it will be paused
   */
  fun pause()

  /**
   * If an audio item is currently in playback then the playback will be stopped
   */
  fun stop()

  /**
   * Prepares the media previously specified for playback.  This should only be called after
   * the playback has completed to restart playback from the beginning.
   *
   * @return `true` if the media was successfully restarted
   */
  fun restart(): Boolean

  /**
   * Releases the resources associated with this media player
   */
  fun release()

  /**
   * Stops the current media playback and resets the listener states
   * so that we receive the callbacks for events like onPrepared
   */
  fun reset()

  /**
   * Sets the playback speed for this MediaPlayer.
   *
   * @param speed The speed to play the media back at
   * @return True if the speed was set
   */
  fun setPlaybackSpeed(speed: Float): Boolean

  /**
   * Sets the audio attributes for this [AudioPlayerApi].
   * You must call this method before loading media with [setMedia] in order
   * for the audio attributes to become effective.
   *
   * @param attributes The [AudioAttributes] to apply
   */
  fun setAudioAttributes(attributes: AudioAttributes)

  /**
   * Determines if the current video player implementation supports
   * track selection for audio or video tracks.
   *
   * @return True if tracks can be manually specified
   */
  fun trackSelectionAvailable(): Boolean

  /**
   * Changes to the track with `trackIndex` for the specified
   * `trackType`
   *
   * @param type The type for the track to switch to the selected index
   * @param groupIndex The index for the group in the [TrackGroupArray] specified by the `trackType`
   * @param trackIndex The index for the track to switch to
   */
  fun setSelectedTrack(type: RendererType, groupIndex: Int, trackIndex: Int)

  /**
   * Gets the selected track index for the specified type and group
   */
  fun getSelectedTrackIndex(type: RendererType, groupIndex: Int): Int

  /**
   * Clear all selected tracks for the specified renderer.
   * @param type The renderer type
   */
  fun clearSelectedTracks(type: RendererType)

  /**
   * Enables or disables the track associated with the `type`. Note, by default all
   * tracks are enabled
   *
   * @param type The [RendererType] to enable or disable the track for
   * @param enabled `true` if the track should be enabled.
   */
  fun setRendererEnabled(type: RendererType, enabled: Boolean)

  /**
   * Return true if at least one renderer for the given type is enabled
   *
   * @param type The renderer type
   * @return true if at least one renderer for the given type is enabled
   */
  fun isRendererEnabled(type: RendererType): Boolean

  /**
   * Moves the current audio progress to the specified location.
   * This method should only be called after the AudioPlayer is
   * prepared.
   *
   * @param milliseconds The time to move the playback to
   */
  fun seekTo(@IntRange(from = 0) milliseconds: Long)

  /**
   * Set the low-level power management behavior for this AudioPlayer.
   *
   * This function has the AudioPlayer access the low-level power manager
   * service to control the device's power usage while playing is occurring.
   * The parameter is a combination of [android.os.PowerManager] wake flags.
   * Use of this method requires [android.Manifest.permission.WAKE_LOCK]
   * permission.
   * By default, no attempt is made to keep the device awake during playback.
   *
   * @param levelAndFlags the power/wake mode to set
   * @see android.os.PowerManager
   */
  fun setWakeLevel(levelAndFlags: Int)

  fun setListenerMux(listenerMux: ListenerMux)

  /**
   * Sets the repeat mode for this MediaPlayer.
   * **Note:** This will only change the ExoPlayer implementation
   *
   * @param repeatMode The repeat mode to use
   */
  fun setRepeatMode(@Player.RepeatMode repeatMode: Int)
}