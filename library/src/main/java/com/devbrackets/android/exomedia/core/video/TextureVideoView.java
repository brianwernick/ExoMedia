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

package com.devbrackets.android.exomedia.core.video;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.widget.MediaController;

import java.io.IOException;
import java.util.Map;

/**
 * A "Native" VideoView implementation using the {@link android.view.TextureView}
 * as a backing instead of the older {@link android.view.SurfaceView}.  This
 * resolves issues with the SurfaceView because the TextureView is an actual
 * View that follows the normal drawing paths; allowing the view to be animated,
 * scaled, etc.
 * <p/>
 * NOTE: This does remove some of the functionality from the VideoView including:
 * <ul>
 * <li>The {@link MediaController}</li>
 * </ul>
 */
public class TextureVideoView extends VideoTextureView implements MediaController.MediaPlayerControl {
    private static final String TAG = "TextureVideoView";

    protected enum State {
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

    protected MediaPlayer mediaPlayer;
    protected Point videoSize = new Point(0, 0);

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

    public TextureVideoView(Context context) {
        super(context);
        setup(context, null);
    }

    public TextureVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup(context, attrs);
    }

    public TextureVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public TextureVideoView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setup(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = getDefaultSize(videoSize.x, widthMeasureSpec);
        int height = getDefaultSize(videoSize.y, heightMeasureSpec);
        if (videoSize.x > 0 && videoSize.y > 0) {

            int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
            int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
            int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
            int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);

            if (widthSpecMode == MeasureSpec.EXACTLY && heightSpecMode == MeasureSpec.EXACTLY) {
                // the size is fixed
                width = widthSpecSize;
                height = heightSpecSize;

                // for compatibility, we adjust size based on aspect ratio
                if (videoSize.x * height < width * videoSize.y) {
                    width = height * videoSize.x / videoSize.y;
                } else if (videoSize.x * height > width * videoSize.y) {
                    height = width * videoSize.y / videoSize.x;
                }
            } else if (widthSpecMode == MeasureSpec.EXACTLY) {
                // only the width is fixed, adjust the height to match aspect ratio if possible
                width = widthSpecSize;
                height = width * videoSize.y / videoSize.x;
                if (heightSpecMode == MeasureSpec.AT_MOST && height > heightSpecSize) {
                    // couldn't match aspect ratio within the constraints
                    height = heightSpecSize;
                }
            } else if (heightSpecMode == MeasureSpec.EXACTLY) {
                // only the height is fixed, adjust the width to match aspect ratio if possible
                height = heightSpecSize;
                width = height * videoSize.x / videoSize.y;
                if (widthSpecMode == MeasureSpec.AT_MOST && width > widthSpecSize) {
                    // couldn't match aspect ratio within the constraints
                    width = widthSpecSize;
                }
            } else {
                // neither the width nor the height are fixed, try to use actual video size
                width = videoSize.x;
                height = videoSize.y;
                if (heightSpecMode == MeasureSpec.AT_MOST && height > heightSpecSize) {
                    // too tall, decrease both width and height
                    height = heightSpecSize;
                    width = height * videoSize.x / videoSize.y;
                }
                if (widthSpecMode == MeasureSpec.AT_MOST && width > widthSpecSize) {
                    // too wide, decrease both width and height
                    width = widthSpecSize;
                    height = width * videoSize.y / videoSize.x;
                }
            }
        }

        setMeasuredDimension(width, height);
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

    public void stopPlayback() {
        mediaPlayer.stop();
        currentState = State.IDLE;

        playRequested = false;
        AudioManager am = (AudioManager) getContext().getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        am.abandonAudioFocus(null);

        clearSurface();
    }

    public void suspend() {
        mediaPlayer.reset();
        mediaPlayer.release();

        currentState = State.IDLE;

        playRequested = false;
        AudioManager am = (AudioManager) getContext().getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        am.abandonAudioFocus(null);
    }

    /**
     * Sets video URI.
     *
     * @param uri the URI of the video.
     */
    public void setVideoURI(Uri uri) {
        setVideoURI(uri, null);
    }

    /**
     * Sets video URI using specific headers.
     *
     * @param uri     The Uri for the video to play
     * @param headers the headers for the URI request.
     *                Note that the cross domain redirection is allowed by default, but that can be
     *                changed with key/value pairs through the headers parameter with
     *                "android-allow-cross-domain-redirect" as the key and "0" or "1" as the value
     *                to disallow or allow cross domain redirection.
     */
    public void setVideoURI(Uri uri, @Nullable Map<String, String> headers) {
        this.headers = headers;
        requestedSeek = 0;

        openVideo(uri);
        requestLayout();
        invalidate();
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

    private boolean isReady() {
        return currentState != State.ERROR && currentState != State.IDLE && currentState != State.PREPARING;
    }

    public int getAudioSessionId() {
        return mediaPlayer.getAudioSessionId();
    }

    protected void openVideo(@Nullable Uri uri) {
        if (uri == null) {
            return;
        }

        AudioManager am = (AudioManager) getContext().getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        currentBufferPercent = 0;

        try {
            mediaPlayer.setDataSource(getContext().getApplicationContext(), uri, headers);
            mediaPlayer.prepareAsync();

            currentState = State.PREPARING;
        } catch (IOException | IllegalArgumentException ex) {
            Log.w(TAG, "Unable to open content: " + uri, ex);
            currentState = State.ERROR;

            internalListeners.onError(mediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
        }
    }

    protected void setup(Context context, @Nullable AttributeSet attrs) {
        initMediaPlayer();
        setSurfaceTextureListener(new TextureVideoViewSurfaceListener());

        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();

        videoSize.x = 0;
        videoSize.y = 0;
        currentState = State.IDLE;
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

    protected class InternalListeners implements MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener,
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

            videoSize.x = mp.getVideoWidth();
            videoSize.y = mp.getVideoHeight();

            if (requestedSeek != 0) {
                seekTo(requestedSeek);
            }

            if (videoSize.x != 0 && videoSize.y != 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                getSurfaceTexture().setDefaultBufferSize(videoSize.x, videoSize.y);
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
            videoSize.x = mp.getVideoWidth();
            videoSize.y = mp.getVideoHeight();
            if (videoSize.x == 0 || videoSize.y == 0) {
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                getSurfaceTexture().setDefaultBufferSize(videoSize.x, videoSize.y);
            }

            requestLayout();
        }
    }

    protected class TextureVideoViewSurfaceListener implements SurfaceTextureListener {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            surface = new Surface(surfaceTexture);
            mediaPlayer.setSurface(surface);
            if (playRequested) {
                mediaPlayer.prepareAsync();
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            if (mediaPlayer == null || width <= 0 || height <= 0) {
                return;
            }

            if (requestedSeek != 0) {
                seekTo(requestedSeek);
            }

            start();
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            surface.release();
            mediaPlayer.release();
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            //Purposefully left blank
        }
    }
}