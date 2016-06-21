package com.devbrackets.android.exomedia.core.video.delegate;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Surface;
import android.widget.MediaController;

import java.io.IOException;
import java.util.Map;

import static android.content.ContentValues.TAG;

/**
 * A delegated object used to handle the majority of the
 * functionality for the "Native" video view implementation
 * to simplify support for both the {@link android.view.TextureView}
 * and {@link android.view.SurfaceView} implementations
 */
public class NativeDelegate implements MediaController.MediaPlayerControl {
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

    protected MediaPlayer mediaPlayer;

    protected boolean playRequested = false;
    protected int requestedSeek;
    protected int currentBufferPercent;

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

    public NativeDelegate(@NonNull Context context, @NonNull Callback callback) {
        this.context = context;
        this.callback = callback;

        initMediaPlayer();
        currentState = State.IDLE;
    }

    @Override
    public void start() {
        if (isReady()) {
            mediaPlayer.start();
            currentState = State.PLAYING;
        }

        playRequested = true;
    }

    @Override
    public void pause() {
        if (isReady() && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            currentState = State.PAUSED;
        }

        playRequested = false;
    }

    @Override
    public int getDuration() {
        if (isReady()) {
            return mediaPlayer.getDuration();
        }

        return 0;
    }

    @Override
    public int getCurrentPosition() {
        if (isReady()) {
            return mediaPlayer.getCurrentPosition();
        }

        return 0;
    }

    @Override
    public void seekTo(int msec) {
        if (isReady()) {
            mediaPlayer.seekTo(msec);
            requestedSeek = 0;
        } else {
            requestedSeek = msec;
        }
    }

    @Override
    public boolean isPlaying() {
        return isReady() && mediaPlayer.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        if (mediaPlayer != null) {
            return currentBufferPercent;
        }

        return 0;
    }

    @Override
    public boolean canPause() {
        return currentState == State.PREPARED || currentState == State.PLAYING;
    }

    @Override
    public boolean canSeekBackward() {
        return currentState == State.PREPARED || currentState == State.PLAYING || currentState == State.PAUSED;
    }

    @Override
    public boolean canSeekForward() {
        return currentState == State.PREPARED || currentState == State.PLAYING || currentState == State.PAUSED;
    }

    @Override
    public int getAudioSessionId() {
        return mediaPlayer.getAudioSessionId();
    }

    /**
     * Performs the functionality to stop the video in playback
     */
    public void stopPlayback() {
        currentState = State.IDLE;

        if (isReady()) {
            try {
                mediaPlayer.stop();
            } catch (Exception e) {
                Log.d(TAG, "stopPlayback: error calling mediaPlayer.stop()", e);
            }
        }

        playRequested = false;
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
        if(currentState != State.COMPLETED) {
            return false;
        }

        seekTo(0);
        start();

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
