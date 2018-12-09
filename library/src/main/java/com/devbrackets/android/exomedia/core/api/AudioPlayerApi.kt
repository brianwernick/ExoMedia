/*
 * Copyright (C) 2016 - 2018 ExoMedia Contributors
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
import android.support.annotation.FloatRange
import android.support.annotation.IntRange

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

    val isPlaying: Boolean

    @get:IntRange(from = 0)
    val duration: Long

    @get:IntRange(from = 0)
    val currentPosition: Long

    @get:IntRange(from = 0, to = 100)
    val bufferedPercent: Int

    val windowInfo: WindowInfo?

    val audioSessionId: Int

    val playbackSpeed: Float

    /**
     * Retrieves a list of available tracks to select from.  Typically [.trackSelectionAvailable]
     * should be called before this.
     *
     * @return A list of available tracks associated with each track type
     */
    val availableTracks: Map<ExoMedia.RendererType, TrackGroupArray>?

    @get:FloatRange(from = 0.0, to = 1.0)
    val volumeLeft: Float

    @get:FloatRange(from = 0.0, to = 1.0)
    val volumeRight: Float

    fun setDataSource(uri: Uri?)

    fun setDataSource(uri: Uri?, mediaSource: MediaSource?)

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

    fun start()

    fun pause()

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

    fun trackSelectionAvailable(): Boolean

    fun setTrack(type: ExoMedia.RendererType, groupIndex: Int, trackIndex: Int)

    fun getSelectedTrackIndex(type: ExoMedia.RendererType, groupIndex: Int): Int

    fun setVolume(@FloatRange(from = 0.0, to = 1.0) left: Float, @FloatRange(from = 0.0, to = 1.0) right: Float)

    fun seekTo(@IntRange(from = 0) milliseconds: Long)

    fun setWakeMode(context: Context, mode: Int)

    fun setListenerMux(listenerMux: ListenerMux)

    fun onMediaPrepared()

    fun setRepeatMode(@Player.RepeatMode repeatMode: Int)
}