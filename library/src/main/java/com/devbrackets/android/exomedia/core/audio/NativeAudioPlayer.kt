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

package com.devbrackets.android.exomedia.core.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import android.support.annotation.FloatRange
import android.support.annotation.IntRange
import android.util.Log

import com.devbrackets.android.exomedia.ExoMedia
import com.devbrackets.android.exomedia.core.ListenerMux
import com.devbrackets.android.exomedia.core.api.AudioPlayerApi
import com.devbrackets.android.exomedia.core.exoplayer.WindowInfo
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.drm.MediaDrmCallback
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray

/**
 * A simple MediaPlayer implementation that extends the
 * one provided by the system to add integration with
 * the [ListenerMux] and to mitigate state errors.
 *
 *
 * NOTE: The `listenerMux` shouldn't be null when any
 * method utilizing it is called, however there are some cases on
 * Amazon devices where they incorrectly call these methods when
 * setting up the MediaPlayer (when in IDLE state)
 */
class NativeAudioPlayer(protected val context: Context) : AudioPlayerApi {
    protected val mediaPlayer: MediaPlayer
    protected var internalListeners = InternalListeners()

    protected var _listenerMux: ListenerMux? = null

    protected var requestedSeek: Long = 0
    override var bufferedPercent = 0
        protected set

    @FloatRange(from = 0.0, to = 1.0)
    override var volumeLeft = 1.0f
        protected set

    @FloatRange(from = 0.0, to = 1.0)
    override var volumeRight = 1.0f
        protected set

    override val isPlaying: Boolean
        get() = mediaPlayer.isPlaying

    override val duration: Long
        get() = if (_listenerMux?.isPrepared != true) {
            0
        } else mediaPlayer.duration.toLong()

    override val currentPosition: Long
        get() = if (_listenerMux?.isPrepared != true) {
            0
        } else mediaPlayer.currentPosition.toLong()

    override val windowInfo: WindowInfo?
        get() = null

    override val audioSessionId: Int
        get() = mediaPlayer.audioSessionId

    // Marshmallow+ support setting the playback speed natively
    override val playbackSpeed: Float
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mediaPlayer.playbackParams.speed
        } else 1f

    override val availableTracks: Map<ExoMedia.RendererType, TrackGroupArray>?
        get() = null

    init {
        mediaPlayer = MediaPlayer()
        mediaPlayer.setOnBufferingUpdateListener(internalListeners)
    }

    override fun setDataSource(uri: Uri?) {
        setDataSource(uri, null)
    }

    override fun setDataSource(uri: Uri?, mediaSource: MediaSource?) {
        try {
            requestedSeek = 0
            mediaPlayer.setDataSource(context, uri!!)
        } catch (e: Exception) {
            Log.d(TAG, "MediaPlayer: error setting data source", e)
        }

    }

    override fun setDrmCallback(drmCallback: MediaDrmCallback?) {
        //Purposefully left blank
    }

    override fun prepareAsync() {
        try {
            mediaPlayer.prepareAsync()
        } catch (e: Exception) {
            //Purposefully left blank
        }

    }

    override fun reset() {
        mediaPlayer.reset()
    }

    override fun setVolume(@FloatRange(from = 0.0, to = 1.0) left: Float, @FloatRange(from = 0.0, to = 1.0) right: Float) {
        volumeLeft = left
        volumeRight = right

        mediaPlayer.setVolume(left, right)
    }

    override fun seekTo(@IntRange(from = 0) milliseconds: Long) {
        if (_listenerMux?.isPrepared == true) {
            mediaPlayer.seekTo(milliseconds.toInt())
            requestedSeek = 0
        } else {
            requestedSeek = milliseconds
        }
    }

    override fun start() {
        mediaPlayer.start()
        _listenerMux?.setNotifiedCompleted(false)
    }

    override fun pause() {
        mediaPlayer.pause()
    }

    override fun stopPlayback() {
        mediaPlayer.stop()
    }

    override fun restart(): Boolean {
        if (_listenerMux?.isPrepared != true) {
            return false
        }

        mediaPlayer.seekTo(0)
        mediaPlayer.start()

        _listenerMux?.setNotifiedCompleted(false)

        return true
    }

    override fun release() {
        mediaPlayer.release()
    }

    override fun setPlaybackSpeed(speed: Float): Boolean {
        // Marshmallow+ support setting the playback speed natively
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val params = PlaybackParams()
            params.speed = speed

            mediaPlayer.playbackParams = params
            return true
        }

        return false
    }

    override fun setAudioStreamType(streamType: Int) {
        mediaPlayer.setAudioStreamType(streamType)
    }

    override fun setWakeMode(context: Context, mode: Int) {
        mediaPlayer.setWakeMode(context, mode)
    }

    override fun trackSelectionAvailable(): Boolean {
        return false
    }

    override fun setTrack(type: ExoMedia.RendererType, groupIndex: Int, trackIndex: Int) {
        // Purposefully left blank
    }

    override fun getSelectedTrackIndex(type: ExoMedia.RendererType, groupIndex: Int): Int {
        return -1
    }

    override fun setListenerMux(listenerMux: ListenerMux) {
        _listenerMux = listenerMux

        mediaPlayer.setOnCompletionListener(listenerMux)
        mediaPlayer.setOnPreparedListener(listenerMux)
        mediaPlayer.setOnBufferingUpdateListener(listenerMux)
        mediaPlayer.setOnSeekCompleteListener(listenerMux)
        mediaPlayer.setOnErrorListener(listenerMux)
    }

    override fun onMediaPrepared() {
        if (requestedSeek != 0L) {
            seekTo(requestedSeek)
        }
    }

    override fun setRepeatMode(@Player.RepeatMode repeatMode: Int) {
        // Purposefully left blank
    }

    protected inner class InternalListeners : MediaPlayer.OnBufferingUpdateListener {
        override fun onBufferingUpdate(mediaPlayer: MediaPlayer, percent: Int) {
            _listenerMux?.onBufferingUpdate(percent)
            bufferedPercent = percent
        }
    }

    companion object {
        private val TAG = "NativeMediaPlayer"
    }
}
