/*
 * Copyright (C) 2016 - 2019 ExoMedia Contributors
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

package com.devbrackets.android.exomedia.core.api

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import androidx.annotation.FloatRange
import androidx.annotation.IntRange

import com.devbrackets.android.exomedia.ExoMedia
import com.devbrackets.android.exomedia.core.ListenerMux
import com.devbrackets.android.exomedia.core.exoplayer.WindowInfo
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.drm.MediaDrmCallback
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray

/**
 * The basic APIs expected in the backing media player
 * implementations to allow us to create an abstraction
 * between the Native (Android) MediaPlayer and the AudioPlayer
 * using the ExoPlayer.
 */
interface AudioPlayerApi {

    /**
     * Returns if an audio item is currently in playback
     *
     * @return True if an audio item is playing
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

    @get:IntRange(from = 0, to = 100)
    val bufferedPercent: Int

    /**
     * Retrieves the information associated with the current [com.google.android.exoplayer2.Timeline.Window]
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
    val availableTracks: Map<ExoMedia.RendererType, TrackGroupArray>?

    /**
     * Retrieves the current left volume
     *
     * @return the volume for the left channel
     */
    @get:FloatRange(from = 0.0, to = 1.0)
    val volumeLeft: Float

    /**
     * Retrieves the current right volume
     *
     * @return the volume for the right channel
     */
    @get:FloatRange(from = 0.0, to = 1.0)
    val volumeRight: Float

    /**
     * Sets the source path for the audio item.  This path can be a web address (e.g. http://) or
     * an absolute local path (e.g. file://)
     *
     * @param uri The Uri representing the path to the audio item
     * @param mediaSource The MediaSource to use for audio playback
     */
    fun setDataSource(uri: Uri? = null, mediaSource: MediaSource? = null)

    /**
     * Sets the [MediaDrmCallback] to use when handling DRM for media.
     * This should be called before specifying the videos uri or path
     * <br></br>
     * **NOTE:** DRM is only supported on API 18 +
     *
     * @param drmCallback The callback to use when handling DRM media
     */
    fun setDrmCallback(drmCallback: MediaDrmCallback?)

    /**
     * Prepares the media specified with [.setDataSource] or
     * [.setDataSource] in an asynchronous manner
     */
    fun prepareAsync()

    /**
     * Starts the playback for the audio item specified in [.setDataSource].
     * This should be called after the AudioPlayer is correctly prepared (see [.setOnPreparedListener])
     */
    fun start()

    /**
     * If an audio item is currently in playback, it will be paused
     */
    fun pause()

    /**
     * If an audio item is currently in playback then the playback will be stopped
     */
    fun stopPlayback()

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
     * Stops the current audio playback and resets the repeatListener states
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
     * Sets the audio stream type for this MediaPlayer. See [AudioManager]
     * for a list of stream types. Must call this method before prepare() or
     * prepareAsync() in order for the target stream type to become effective
     * thereafter.
     *
     * @param streamType The audio stream type
     * @see android.media.AudioManager
     */
    fun setAudioStreamType(streamType: Int)

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
     * @param trackType The type for the track to switch to the selected index
     * @param groupIndex The index for the group in the [TrackGroupArray] specified by the `trackType`
     * @param trackIndex The index for the track to switch to
     */
    fun setTrack(type: ExoMedia.RendererType, groupIndex: Int, trackIndex: Int)

    fun getSelectedTrackIndex(type: ExoMedia.RendererType, groupIndex: Int): Int

    /**
     * Sets the volume level for the audio playback.
     *
     * @param leftVolume The volume range [0.0 - 1.0]
     * @param rightVolume The volume range [0.0 - 1.0]
     */
    fun setVolume(@FloatRange(from = 0.0, to = 1.0) left: Float, @FloatRange(from = 0.0, to = 1.0) right: Float)

    /**
     * Moves the current audio progress to the specified location.
     * This method should only be called after the AudioPlayer is
     * prepared. (see [.setOnPreparedListener]
     *
     * @param milliSeconds The time to move the playback to
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
     * @param context the Context to use
     * @param mode the power/wake mode to set
     * @see android.os.PowerManager
     */
    fun setWakeMode(context: Context, mode: Int)

    fun setListenerMux(listenerMux: ListenerMux)

    fun onMediaPrepared()

    /**
     * Sets the repeat mode for this MediaPlayer.
     * **Note:** This will only change the ExoPlayer implementation
     *
     * @param repeatMode The repeat mode to use
     */
    fun setRepeatMode(@Player.RepeatMode repeatMode: Int)
}