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

package com.devbrackets.android.exomedia.core.video.mp

import android.content.ContentValues.TAG
import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import android.support.annotation.FloatRange
import android.util.Log
import android.view.Surface
import com.devbrackets.android.exomedia.core.ListenerMux
import com.devbrackets.android.exomedia.core.exoplayer.WindowInfo
import com.devbrackets.android.exomedia.core.video.ClearableSurface
import java.io.IOException

/**
 * A delegated object used to handle the majority of the
 * functionality for the "Native" video view implementation
 * to simplify support for both the [android.view.TextureView]
 * and [android.view.SurfaceView] implementations
 */
class NativeVideoDelegate(protected var context: Context, protected var callback: Callback, protected var clearableSurface: ClearableSurface) {

    protected var headers: Map<String, String>? = null

    protected var currentState = State.IDLE

    protected var mediaPlayer: MediaPlayer? = null

    protected var playRequested = false
    protected var requestedSeek: Long = 0
    protected var currentBufferPercent: Int = 0

    @FloatRange(from = 0.0, to = 1.0)
    protected var requestedVolume = 1.0f

    var listenerMux: ListenerMux? = null
        set(value) {
            field = value
            onCompletionListener = value
            onCompletionListener = value
            onPreparedListener = value
            onBufferingUpdateListener = value
            onSeekCompleteListener = value
            onErrorListener = value
        }

    var internalListeners = InternalListeners()

    /**
     * Register a callback to be invoked when the end of a media file
     * has been reached during playback.
     */
    var onCompletionListener: MediaPlayer.OnCompletionListener? = null

    /**
     * Register a callback to be invoked when the media file
     * is loaded and ready to go.
     */
    var onPreparedListener: MediaPlayer.OnPreparedListener? = null

    /**
     * Register a callback to be invoked when the status of a network
     * stream's buffer has changed.
     */
    var onBufferingUpdateListener: MediaPlayer.OnBufferingUpdateListener? = null

    /**
     * Register a callback to be invoked when a seek operation has been
     * completed.
     */
    var onSeekCompleteListener: MediaPlayer.OnSeekCompleteListener? = null

    /**
     * Register a callback to be invoked when an error occurs
     * during playback or setup.  If no repeatListener is specified,
     * or if the repeatListener returned false, TextureVideoView will inform
     * the user of any errors.
     */
    var onErrorListener: MediaPlayer.OnErrorListener? = null

    /**
     * Register a callback to be invoked when an informational event
     * occurs during playback or setup.
     */
    var onInfoListener: MediaPlayer.OnInfoListener? = null

    val duration: Long
        get() = if (listenerMux?.isPrepared != true || !isReady) {
            0
        } else mediaPlayer!!.duration.toLong()

    val currentPosition: Long
        get() = if (listenerMux?.isPrepared  != true || !isReady) {
            0
        } else mediaPlayer!!.currentPosition.toLong()

    val isPlaying: Boolean
        get() = isReady && mediaPlayer!!.isPlaying

    val bufferPercentage: Int
        get() = if (mediaPlayer != null) {
            currentBufferPercent
        } else 0

    val windowInfo: WindowInfo?
        get() = null

    // Marshmallow+ support setting the playback speed natively
    val playbackSpeed: Float
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mediaPlayer?.playbackParams?.speed ?: 1f
        } else 1f

    protected val isReady: Boolean
        get() = currentState != State.ERROR && currentState != State.IDLE && currentState != State.PREPARING

    interface Callback {
        fun videoSizeChanged(width: Int, height: Int)
    }

    enum class State {
        ERROR,
        IDLE,
        PREPARING,
        PREPARED,
        PLAYING,
        PAUSED,
        COMPLETED
    }

    init {
        initMediaPlayer()
        currentState = State.IDLE
    }

    fun start() {
        if (isReady) {
            mediaPlayer?.start()
            currentState = State.PLAYING
        }

        playRequested = true
        listenerMux?.setNotifiedCompleted(false)
    }

    fun pause() {
        if (isReady && mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
            currentState = State.PAUSED
        }

        playRequested = false
    }

    @FloatRange(from = 0.0, to = 1.0)
    fun getVolume(): Float {
        return requestedVolume
    }

    fun setVolume(@FloatRange(from = 0.0, to = 1.0) volume: Float): Boolean {
        requestedVolume = volume
        mediaPlayer?.setVolume(volume, volume)
        return true
    }

    fun seekTo(milliseconds: Long) {
        if (isReady) {
            mediaPlayer?.seekTo(milliseconds.toInt())
            requestedSeek = 0
        } else {
            requestedSeek = milliseconds
        }
    }

    fun setPlaybackSpeed(speed: Float): Boolean {
        // Marshmallow+ support setting the playback speed natively
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mediaPlayer?.playbackParams = PlaybackParams().apply {
                this.speed = speed
            }
            return true
        }

        return false
    }

    /**
     * Performs the functionality to stop the video in playback
     *
     * @param clearSurface `true` if the surface should be cleared
     */
    fun stopPlayback(clearSurface: Boolean) {
        currentState = State.IDLE

        if (isReady) {
            try {
                mediaPlayer?.stop()
            } catch (e: Exception) {
                Log.d(TAG, "stopPlayback: error calling mediaPlayer.stop()", e)
            }

        }

        playRequested = false
        if (clearSurface) {
            listenerMux?.clearSurfaceWhenReady(clearableSurface)
        }
    }

    /**
     * Cleans up the resources being held.  This should only be called when
     * destroying the video view
     */
    fun suspend() {
        currentState = State.IDLE

        try {
            mediaPlayer?.reset()
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.d(TAG, "stopPlayback: error calling mediaPlayer.reset() or mediaPlayer.release()", e)
        }

        playRequested = false
    }

    fun restart(): Boolean {
        if (currentState != State.COMPLETED) {
            return false
        }

        seekTo(0)
        start()

        //Makes sure the listeners get the onPrepared callback
        listenerMux?.setNotifiedPrepared(false)
        listenerMux?.setNotifiedCompleted(false)

        return true
    }

    /**
     * Sets video URI using specific headers.
     *
     * @param uri The Uri for the video to play
     * @param headers The headers for the URI request.
     * Note that the cross domain redirection is allowed by default, but that can be
     * changed with key/value pairs through the headers parameter with
     * "android-allow-cross-domain-redirect" as the key and "0" or "1" as the value
     * to disallow or allow cross domain redirection.
     */
    fun setVideoURI(uri: Uri, headers: Map<String, String>?) {
        this.headers = headers
        requestedSeek = 0
        playRequested = false

        openVideo(uri)
    }

    fun onSurfaceSizeChanged(width: Int, height: Int) {
        if (mediaPlayer == null || width <= 0 || height <= 0) {
            return
        }

        if (requestedSeek != 0L) {
            seekTo(requestedSeek)
        }

        if (playRequested) {
            start()
        }
    }

    fun onSurfaceReady(surface: Surface) {
        mediaPlayer?.setSurface(surface)
        if (playRequested) {
            start()
        }
    }

    protected fun initMediaPlayer() {
        mediaPlayer = MediaPlayer().apply {
            setOnInfoListener(internalListeners)
            setOnErrorListener(internalListeners)
            setOnPreparedListener(internalListeners)
            setOnCompletionListener(internalListeners)
            setOnSeekCompleteListener(internalListeners)
            setOnBufferingUpdateListener(internalListeners)
            setOnVideoSizeChangedListener(internalListeners)

            setAudioStreamType(AudioManager.STREAM_MUSIC)
            setScreenOnWhilePlaying(true)
        }
    }

    protected fun openVideo(uri: Uri?) {
        if (uri == null) {
            return
        }

        currentBufferPercent = 0

        try {
            mediaPlayer?.reset()
            mediaPlayer?.setDataSource(context.applicationContext, uri, headers)
            mediaPlayer?.prepareAsync()

            currentState = State.PREPARING
        } catch (ex: IOException) {
            Log.w(TAG, "Unable to open content: $uri", ex)
            currentState = State.ERROR

            internalListeners.onError(mediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0)
        } catch (ex: IllegalArgumentException) {
            Log.w(TAG, "Unable to open content: $uri", ex)
            currentState = State.ERROR
            internalListeners.onError(mediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0)
        }

    }

    inner class InternalListeners : MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnInfoListener, MediaPlayer.OnVideoSizeChangedListener {
        override fun onBufferingUpdate(mp: MediaPlayer, percent: Int) {
            currentBufferPercent = percent
            onBufferingUpdateListener?.onBufferingUpdate(mp, percent)
        }

        override fun onCompletion(mp: MediaPlayer) {
            currentState = State.COMPLETED
            onCompletionListener?.onCompletion(mediaPlayer)
        }

        override fun onSeekComplete(mp: MediaPlayer) {
            onSeekCompleteListener?.onSeekComplete(mp)
        }

        override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
            Log.d(TAG, "Error: $what,$extra")
            currentState = State.ERROR

            return onErrorListener?.onError(mediaPlayer, what, extra) == true
        }

        override fun onPrepared(mp: MediaPlayer) {
            currentState = State.PREPARED

            onPreparedListener?.onPrepared(mediaPlayer)

            callback.videoSizeChanged(mp.videoWidth, mp.videoHeight)

            if (requestedSeek != 0L) {
                seekTo(requestedSeek)
            }

            if (playRequested) {
                start()
            }
        }

        override fun onInfo(mp: MediaPlayer, what: Int, extra: Int): Boolean {
            return onInfoListener?.onInfo(mp, what, extra) == true
        }

        override fun onVideoSizeChanged(mp: MediaPlayer, width: Int, height: Int) {
            callback.videoSizeChanged(mp.videoWidth, mp.videoHeight)
        }
    }
}
