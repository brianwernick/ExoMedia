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

package com.devbrackets.android.exomedia

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.support.annotation.FloatRange

import com.devbrackets.android.exomedia.core.ListenerMux
import com.devbrackets.android.exomedia.core.api.AudioPlayerApi
import com.devbrackets.android.exomedia.core.audio.ExoAudioPlayer
import com.devbrackets.android.exomedia.core.audio.NativeAudioPlayer
import com.devbrackets.android.exomedia.core.exoplayer.ExoMediaPlayer
import com.devbrackets.android.exomedia.core.exoplayer.WindowInfo
import com.devbrackets.android.exomedia.core.listener.MetadataListener
import com.devbrackets.android.exomedia.listener.OnBufferUpdateListener
import com.devbrackets.android.exomedia.listener.OnCompletionListener
import com.devbrackets.android.exomedia.listener.OnErrorListener
import com.devbrackets.android.exomedia.listener.OnPreparedListener
import com.devbrackets.android.exomedia.listener.OnSeekCompletionListener
import com.devbrackets.android.exomedia.util.DeviceUtil
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.drm.MediaDrmCallback
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray

/**
 * An AudioPlayer that uses the ExoPlayer as the backing architecture.  If the current device
 * does *NOT* pass the Android Compatibility Test Suite (CTS) then the backing architecture
 * will fall back to using the default Android MediaPlayer.
 *
 *
 * To help with quick conversions from the Android MediaPlayer this class follows the APIs
 * the Android MediaPlayer provides.
 */
class AudioPlayer(protected val audioPlayerImpl: AudioPlayerApi) {
    protected val listenerMux = ListenerMux(MuxNotifier())

    protected var overriddenDuration: Long = -1

    @JvmOverloads
    constructor(context: Context, deviceUtil: DeviceUtil = DeviceUtil()) : this (if (deviceUtil.supportsExoPlayer(context)) ExoAudioPlayer(context) else NativeAudioPlayer(context))

    init {
        audioPlayerImpl.setListenerMux(listenerMux)
    }


    /**
     * Returns the audio session ID.
     *
     * @return the audio session ID.
     * Note that the audio session ID is 0 only if a problem occurred when the AudioPlayer was constructed or the audio stream hasn't been
     * instantiated.
     */
    val audioSessionId: Int
        get() = audioPlayerImpl.audioSessionId

    /**
     * Retrieves the current speed the media is playing at.
     *
     * @return The current playback speed
     */
    val playbackSpeed: Float
        get() = audioPlayerImpl.playbackSpeed

    /**
     * Retrieves the current left volume
     *
     * @return the volume for the left channel
     */
    val volumeLeft: Float
        @FloatRange(from = 0.0, to = 1.0)
        get() = audioPlayerImpl.volumeLeft

    /**
     * Retrieves the current right volume
     *
     * @return the volume for the right channel
     */
    internal val volumeRight: Float
        @FloatRange(from = 0.0, to = 1.0)
        get() = audioPlayerImpl.volumeRight

    /**
     * Returns if an audio item is currently in playback
     *
     * @return True if an audio item is playing
     */
    val isPlaying: Boolean
        get() = audioPlayerImpl.isPlaying

    /**
     * Retrieves the duration of the current audio item.  This should only be called after
     * the item is prepared (see [.setOnPreparedListener]).
     * If [.overrideDuration] is set then that value will be returned.
     *
     * @return The millisecond duration of the video
     */
    val duration: Long
        get() = if (overriddenDuration >= 0) {
            overriddenDuration
        } else audioPlayerImpl.duration

    /**
     * Retrieves the current position of the audio playback.  If an audio item is not currently
     * in playback then the value will be 0.  This should only be called after the item is
     * prepared (see [.setOnPreparedListener])
     *
     * @return The millisecond value for the current position
     */
    val currentPosition: Long
        get() = audioPlayerImpl.currentPosition

    /**
     * Retrieves the current buffer percent of the audio item.  If an audio item is not currently
     * prepared or buffering the value will be 0.  This should only be called after the audio item is
     * prepared (see [.setOnPreparedListener])
     *
     * @return The integer percent that is buffered [0, 100] inclusive
     */
    val bufferPercentage: Int
        get() = audioPlayerImpl.bufferedPercent

    /**
     * Retrieves the information associated with the current [com.google.android.exoplayer2.Timeline.Window]
     * used by the ExoPlayer backed implementation. When the [android.media.MediaPlayer] backed
     * implementation is being used this will be null.
     *
     * @return The current Window information or null
     */
    val windowInfo: WindowInfo?
        get() = audioPlayerImpl.windowInfo

    /**
     * Retrieves a list of available tracks to select from.  Typically [.trackSelectionAvailable]
     * should be called before this.
     *
     * @return A list of available tracks associated with each track type
     */
    val availableTracks: Map<ExoMedia.RendererType, TrackGroupArray>?
        get() = audioPlayerImpl.availableTracks

    /**
     * Sets the repeat mode for this MediaPlayer.
     * **Note:** This will only change the ExoPlayer implementation
     *
     * @param repeatMode The repeat mode to use
     */
    fun setRepeatMode(@Player.RepeatMode repeatMode: Int) {
        audioPlayerImpl.setRepeatMode(repeatMode)
    }

    /**
     * Sets the playback speed for this MediaPlayer.
     *
     * @param speed The speed to play the media back at
     * @return True if the speed was set
     */
    fun setPlaybackSpeed(speed: Float): Boolean {
        return audioPlayerImpl.setPlaybackSpeed(speed)
    }

    /**
     * Sets the audio stream type for this MediaPlayer. See [AudioManager]
     * for a list of stream types. Must call this method before prepare() or
     * prepareAsync() in order for the target stream type to become effective
     * thereafter.
     *
     * @param streamType The audio stream type
     * @see android.media.AudioManager
     */
    fun setAudioStreamType(streamType: Int) {
        audioPlayerImpl.setAudioStreamType(streamType)
    }

    /**
     * Sets the source path for the audio item.  This path can be a web address (e.g. http://) or
     * an absolute local path (e.g. file://)
     *
     * @param uri The Uri representing the path to the audio item
     */
    fun setDataSource(uri: Uri?) {
        audioPlayerImpl.setDataSource(uri)
        overrideDuration(-1)
    }

    /**
     * Sets the source path for the audio item.  This path can be a web address (e.g. http://) or
     * an absolute local path (e.g. file://)
     *
     * @param uri The Uri representing the path to the audio item
     * @param mediaSource The MediaSource to use for audio playback
     */
    fun setDataSource(uri: Uri?, mediaSource: MediaSource?) {
        audioPlayerImpl.setDataSource(uri, mediaSource)
        overrideDuration(-1)
    }

    /**
     * Sets the [MediaDrmCallback] to use when handling DRM for media.
     * This should be called before specifying the videos uri or path
     * <br></br>
     * **NOTE:** DRM is only supported on API 18 +
     *
     * @param drmCallback The callback to use when handling DRM media
     */
    fun setDrmCallback(drmCallback: MediaDrmCallback?) {
        audioPlayerImpl.setDrmCallback(drmCallback)
    }

    /**
     * Prepares the media specified with [.setDataSource] or
     * [.setDataSource] in an asynchronous manner
     */
    fun prepareAsync() {
        audioPlayerImpl.prepareAsync()
    }

    /**
     * Sets the volume level for the audio playback.
     *
     * @param leftVolume The volume range [0.0 - 1.0]
     * @param rightVolume The volume range [0.0 - 1.0]
     */
    fun setVolume(@FloatRange(from = 0.0, to = 1.0) leftVolume: Float, @FloatRange(from = 0.0, to = 1.0) rightVolume: Float) {
        audioPlayerImpl.setVolume(leftVolume, rightVolume)
    }

    /**
     * Set the low-level power management behavior for this AudioPlayer.
     *
     *
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
    fun setWakeMode(context: Context, mode: Int) {
        audioPlayerImpl.setWakeMode(context, mode)
    }

    /**
     * Stops the current audio playback and resets the repeatListener states
     * so that we receive the callbacks for events like onPrepared
     */
    fun reset() {
        stopPlayback()
        setDataSource(null, null)

        audioPlayerImpl.reset()
    }

    /**
     * Moves the current audio progress to the specified location.
     * This method should only be called after the AudioPlayer is
     * prepared. (see [.setOnPreparedListener]
     *
     * @param milliSeconds The time to move the playback to
     */
    fun seekTo(milliSeconds: Long) {
        audioPlayerImpl.seekTo(milliSeconds)
    }

    /**
     * Starts the playback for the audio item specified in [.setDataSource].
     * This should be called after the AudioPlayer is correctly prepared (see [.setOnPreparedListener])
     */
    fun start() {
        audioPlayerImpl.start()
    }

    /**
     * If an audio item is currently in playback, it will be paused
     */
    fun pause() {
        audioPlayerImpl.pause()
    }

    /**
     * If an audio item is currently in playback then the playback will be stopped
     */
    fun stopPlayback() {
        audioPlayerImpl.stopPlayback()
    }

    /**
     * Releases the resources associated with this media player
     */
    fun release() {
        audioPlayerImpl.release()
    }

    /**
     * Setting this will override the duration that the item may actually be.  This method should
     * only be used when the item doesn't return the correct duration such as with audio streams.
     * This only overrides the current audio item.
     *
     * @param duration The duration for the current media item or &lt; 0 to disable
     */
    fun overrideDuration(duration: Long) {
        overriddenDuration = duration
    }

    /**
     * Determines if the current video player implementation supports
     * track selection for audio or video tracks.
     *
     * @return True if tracks can be manually specified
     */
    fun trackSelectionAvailable(): Boolean {
        return audioPlayerImpl.trackSelectionAvailable()
    }

    /**
     * Changes to the track with `trackIndex` for the specified
     * `trackType`
     *
     * @param trackType The type for the track to switch to the selected index
     * @param groupIndex The index for the group in the [TrackGroupArray] specified by the `trackType`
     * @param trackIndex The index for the track to switch to
     */
    fun setTrack(trackType: ExoMedia.RendererType, groupIndex: Int, trackIndex: Int) {
        audioPlayerImpl.setTrack(trackType, groupIndex, trackIndex)
    }


    fun getSelectedTrackIndex(type: ExoMedia.RendererType, groupIndex: Int): Int {
        return audioPlayerImpl.getSelectedTrackIndex(type, groupIndex)
    }

    /**
     * Sets the repeatListener to inform of VideoPlayer prepared events
     *
     * @param listener The repeatListener
     */
    fun setOnPreparedListener(listener: OnPreparedListener?) {
        listenerMux.setOnPreparedListener(listener)
    }

    /**
     * Sets the repeatListener to inform of VideoPlayer completion events
     *
     * @param listener The repeatListener
     */
    fun setOnCompletionListener(listener: OnCompletionListener?) {
        listenerMux.setOnCompletionListener(listener)
    }

    /**
     * Sets the repeatListener to inform of VideoPlayer buffer update events
     *
     * @param listener The repeatListener
     */
    fun setOnBufferUpdateListener(listener: OnBufferUpdateListener?) {
        listenerMux.setOnBufferUpdateListener(listener)
    }

    /**
     * Sets the repeatListener to inform of VideoPlayer seek completion events
     *
     * @param listener The repeatListener
     */
    fun setOnSeekCompletionListener(listener: OnSeekCompletionListener?) {
        listenerMux.setOnSeekCompletionListener(listener)
    }

    /**
     * Sets the repeatListener to inform of playback errors
     *
     * @param listener The repeatListener
     */
    fun setOnErrorListener(listener: OnErrorListener?) {
        listenerMux.setOnErrorListener(listener)
    }

    /**
     * Sets the repeatListener to inform of ID3 metadata updates
     *
     * @param listener The repeatListener to inform
     */
    fun setMetadataListener(listener: MetadataListener?) {
        listenerMux.setMetadataListener(listener)
    }

    /**
     * Sets the repeatListener to inform of Analytics updates
     *
     * @param listener The repeatListener to inform
     */
    fun setAnalyticsListener(listener: AnalyticsListener?) {
        listenerMux.setAnalyticsListener(listener)
    }

    /**
     * Performs the functionality to stop the progress polling, and stop any other
     * procedures from running that we no longer need.
     */
    private fun onPlaybackEnded() {
        pause()
    }

    private inner class MuxNotifier : ListenerMux.Notifier() {
        override fun shouldNotifyCompletion(endLeeway: Long): Boolean {
            val position = currentPosition
            val duration = duration
            return position > 0 && duration > 0 && position + endLeeway >= duration
        }

        override fun onExoPlayerError(exoMediaPlayer: ExoMediaPlayer, e: Exception?) {
            stopPlayback()

            exoMediaPlayer.forcePrepare()
        }

        override fun onMediaPlaybackEnded() {
            onPlaybackEnded()
        }

        override fun onPrepared() {
            audioPlayerImpl.onMediaPrepared()
        }
    }
}
