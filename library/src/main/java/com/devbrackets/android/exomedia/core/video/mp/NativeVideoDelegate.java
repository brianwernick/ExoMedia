/*
 * Copyright (C) 2016 Brian Wernick
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

package com.devbrackets.android.exomedia.core.video.mp;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Surface;

import com.devbrackets.android.exomedia.core.ListenerMux;
import com.devbrackets.android.exomedia.core.video.ClearableSurface;

import java.io.IOException;
import java.util.Map;

import static android.content.ContentValues.TAG;

/**
 * A delegated object used to handle the majority of the
 * functionality for the "Native" video view implementation
 * to simplify support for both the {@link android.view.TextureView}
 * and {@link android.view.SurfaceView} implementations
 */
@SuppressWarnings("WeakerAccess")
public class NativeVideoDelegate {
    public interface Callback {
        void videoSizeChanged(int width, int height);
    }

    public enum State {
        ERROR,
        IDLE,
        PREPARING,
        PREPARED,
        PLAYING,
        PAUSED,
        COMPLETED
    }

    protected Map<String, String> headers;

    protected State currentState = State.IDLE;

    protected Context context;
    protected Callback callback;
    protected ClearableSurface clearableSurface;

    protected MediaPlayer mediaPlayer;

    protected boolean playRequested = false;
    protected long requestedSeek;
    protected int currentBufferPercent;

    protected ListenerMux listenerMux;

    @NonNull
    protected InternalListeners internalListeners = new InternalListeners();

    @Nullable
    protected MediaPlayer.OnCompletionListener onCompletionListener;
    @Nullable
    protected MediaPlayer.OnPreparedListener onPreparedListener;
    @Nullable
    protected MediaPlayer.OnBufferingUpdateListener onBufferingUpdateListener;
    @Nullable
    protected MediaPlayer.OnSeekCompleteListener onSeekCompleteListener;
    @Nullable
    protected MediaPlayer.OnErrorListener onErrorListener;
    @Nullable
    protected MediaPlayer.OnInfoListener onInfoListener;

    public NativeVideoDelegate(@NonNull Context context, @NonNull Callback callback, @NonNull ClearableSurface clearableSurface) {
        this.context = context;
        this.callback = callback;
        this.clearableSurface = clearableSurface;

        initMediaPlayer();
        currentState = State.IDLE;
    }

    public void start() {
        if (isReady()) {
            mediaPlayer.start();
            currentState = State.PLAYING;
        }

        playRequested = true;
        listenerMux.setNotifiedCompleted(false);
    }

    public void pause() {
        if (isReady() && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            currentState = State.PAUSED;
        }

        playRequested = false;
    }

    public long getDuration() {
        if (!listenerMux.isPrepared() || !isReady()) {
            return 0;
        }

        return mediaPlayer.getDuration();
    }

    public long getCurrentPosition() {
        if (!listenerMux.isPrepared() || !isReady()) {
            return 0;
        }

        return mediaPlayer.getCurrentPosition();
    }

    public void seekTo(long milliseconds) {
        if (isReady()) {
            mediaPlayer.seekTo((int) milliseconds);
            requestedSeek = 0;
        } else {
            requestedSeek = milliseconds;
        }
    }

    public boolean isPlaying() {
        return isReady() && mediaPlayer.isPlaying();
    }

    public int getBufferPercentage() {
        if (mediaPlayer != null) {
            return currentBufferPercent;
        }

        return 0;
    }

    public boolean setPlaybackSpeed(float speed) {
        // Marshmallow+ support setting the playback speed natively
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PlaybackParams params = new PlaybackParams();
            params.setSpeed(speed);

            mediaPlayer.setPlaybackParams(params);
            return true;
        }

        return false;
    }

    /**
     * Performs the functionality to stop the video in playback
     *
     * @param clearSurface <code>true</code> if the surface should be cleared
     */
    public void stopPlayback(boolean clearSurface) {
        currentState = State.IDLE;

        if (isReady()) {
            try {
                mediaPlayer.stop();
            } catch (Exception e) {
                Log.d(TAG, "stopPlayback: error calling mediaPlayer.stop()", e);
            }
        }

        playRequested = false;
        if (clearSurface) {
            listenerMux.clearSurfaceWhenReady(clearableSurface);
        }
    }

    /**
     * Cleans up the resources being held.  This should only be called when
     * destroying the video view
     */
    public void suspend() {
        currentState = State.IDLE;

        try {
            mediaPlayer.reset();
            mediaPlayer.release();
        } catch (Exception e) {
            Log.d(TAG, "stopPlayback: error calling mediaPlayer.reset() or mediaPlayer.release()", e);
        }

        playRequested = false;
    }

    public boolean restart() {
        if (currentState != State.COMPLETED) {
            return false;
        }

        seekTo(0);
        start();

        //Makes sure the listeners get the onPrepared callback
        listenerMux.setNotifiedPrepared(false);
        listenerMux.setNotifiedCompleted(false);

        return true;
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
    public void setVideoURI(Uri uri, @Nullable Map<String, String> headers) {
        this.headers = headers;
        requestedSeek = 0;
        playRequested = false;

        openVideo(uri);
    }

    public void setListenerMux(ListenerMux listenerMux) {
        this.listenerMux = listenerMux;

        setOnCompletionListener(listenerMux);
        setOnPreparedListener(listenerMux);
        setOnBufferingUpdateListener(listenerMux);
        setOnSeekCompleteListener(listenerMux);
        setOnErrorListener(listenerMux);
    }

    /**
     * Register a callback to be invoked when the media file
     * is loaded and ready to go.
     *
     * @param listener The callback that will be run
     */
    public void setOnPreparedListener(@Nullable MediaPlayer.OnPreparedListener listener) {
        onPreparedListener = listener;
    }

    /**
     * Register a callback to be invoked when the end of a media file
     * has been reached during playback.
     *
     * @param listener The callback that will be run
     */
    public void setOnCompletionListener(@Nullable MediaPlayer.OnCompletionListener listener) {
        onCompletionListener = listener;
    }

    /**
     * Register a callback to be invoked when the status of a network
     * stream's buffer has changed.
     *
     * @param listener the callback that will be run.
     */
    public void setOnBufferingUpdateListener(@Nullable MediaPlayer.OnBufferingUpdateListener listener) {
        onBufferingUpdateListener = listener;
    }

    /**
     * Register a callback to be invoked when a seek operation has been
     * completed.
     *
     * @param listener the callback that will be run
     */
    public void setOnSeekCompleteListener(@Nullable MediaPlayer.OnSeekCompleteListener listener) {
        onSeekCompleteListener = listener;
    }

    /**
     * Register a callback to be invoked when an error occurs
     * during playback or setup.  If no listener is specified,
     * or if the listener returned false, TextureVideoView will inform
     * the user of any errors.
     *
     * @param listener The callback that will be run
     */
    public void setOnErrorListener(@Nullable MediaPlayer.OnErrorListener listener) {
        onErrorListener = listener;
    }

    /**
     * Register a callback to be invoked when an informational event
     * occurs during playback or setup.
     *
     * @param listener The callback that will be run
     */
    public void setOnInfoListener(@Nullable MediaPlayer.OnInfoListener listener) {
        onInfoListener = listener;
    }

    public void onSurfaceSizeChanged(int width, int height) {
        if (mediaPlayer == null || width <= 0 || height <= 0) {
            return;
        }

        if (requestedSeek != 0) {
            seekTo(requestedSeek);
        }

        if (playRequested) {
            start();
        }
    }

    public void onSurfaceReady(Surface surface) {
        mediaPlayer.setSurface(surface);
        if (playRequested) {
            start();
        }
    }

    protected void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();

        mediaPlayer.setOnInfoListener(internalListeners);
        mediaPlayer.setOnErrorListener(internalListeners);
        mediaPlayer.setOnPreparedListener(internalListeners);
        mediaPlayer.setOnCompletionListener(internalListeners);
        mediaPlayer.setOnSeekCompleteListener(internalListeners);
        mediaPlayer.setOnBufferingUpdateListener(internalListeners);
        mediaPlayer.setOnVideoSizeChangedListener(internalListeners);

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setScreenOnWhilePlaying(true);
    }

    protected boolean isReady() {
        return currentState != State.ERROR && currentState != State.IDLE && currentState != State.PREPARING;
    }

    protected void openVideo(@Nullable Uri uri) {
        if (uri == null) {
            return;
        }

        currentBufferPercent = 0;

        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(context.getApplicationContext(), uri, headers);
            mediaPlayer.prepareAsync();

            currentState = State.PREPARING;
        } catch (IOException | IllegalArgumentException ex) {
            Log.w(TAG, "Unable to open content: " + uri, ex);
            currentState = State.ERROR;

            internalListeners.onError(mediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
        }
    }

    public class InternalListeners implements MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener,
            MediaPlayer.OnCompletionListener, MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnInfoListener, MediaPlayer.OnVideoSizeChangedListener {
        @Override
        public void onBufferingUpdate(MediaPlayer mp, int percent) {
            currentBufferPercent = percent;
            if (onBufferingUpdateListener != null) {
                onBufferingUpdateListener.onBufferingUpdate(mp, percent);
            }
        }

        @Override
        public void onCompletion(MediaPlayer mp) {
            currentState = State.COMPLETED;
            if (onCompletionListener != null) {
                onCompletionListener.onCompletion(mediaPlayer);
            }
        }

        @Override
        public void onSeekComplete(MediaPlayer mp) {
            if (onSeekCompleteListener != null) {
                onSeekCompleteListener.onSeekComplete(mp);
            }
        }

        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            Log.d(TAG, "Error: " + what + "," + extra);
            currentState = State.ERROR;

            return onErrorListener == null || onErrorListener.onError(mediaPlayer, what, extra);
        }

        @Override
        public void onPrepared(MediaPlayer mp) {
            currentState = State.PREPARED;

            if (onPreparedListener != null) {
                onPreparedListener.onPrepared(mediaPlayer);
            }

            callback.videoSizeChanged(mp.getVideoWidth(), mp.getVideoHeight());

            if (requestedSeek != 0) {
                seekTo(requestedSeek);
            }

            if (playRequested) {
                start();
            }
        }

        @Override
        public boolean onInfo(MediaPlayer mp, int what, int extra) {
            return onInfoListener == null || onInfoListener.onInfo(mp, what, extra);
        }

        @Override
        public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
            callback.videoSizeChanged(mp.getVideoWidth(), mp.getVideoHeight());
        }
    }
}
