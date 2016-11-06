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

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.Surface;
import android.widget.MediaController;

import com.devbrackets.android.exomedia.annotation.TrackRenderType;
import com.devbrackets.android.exomedia.core.EMListenerMux;
import com.devbrackets.android.exomedia.core.api.VideoViewApi;
import com.devbrackets.android.exomedia.core.builder.RenderBuilder;
import com.devbrackets.android.exomedia.core.video.ResizingTextureView;
import com.devbrackets.android.exomedia.util.DrmProvider;
import com.google.android.exoplayer.MediaFormat;

import java.util.List;
import java.util.Map;

/**
 * A "Native" VideoView implementation using the {@link android.view.TextureView}
 * as a backing instead of the older {@link android.view.SurfaceView}.  This
 * resolves issues with the SurfaceView because the TextureView is an actual
 * View that follows the normal drawing paths; allowing the view to be animated,
 * scaled, etc.
 * <br><br>
 * NOTE: This does remove some of the functionality from the VideoView including:
 * <ul>
 * <li>The {@link MediaController}</li>
 * </ul>
 */
public class NativeTextureVideoView extends ResizingTextureView implements MediaController.MediaPlayerControl, NativeVideoDelegate.Callback, VideoViewApi {
    protected OnTouchListener touchListener;
    protected NativeVideoDelegate delegate;

    public NativeTextureVideoView(Context context) {
        super(context);
        setup(context, null);
    }

    public NativeTextureVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup(context, attrs);
    }

    public NativeTextureVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public NativeTextureVideoView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setup(context, attrs);
    }

    @Override
    public void setDrmProvider(@Nullable DrmProvider drmProvider) {
        //Purposefully left blank
    }

    @Override
    public void start() {
        delegate.start();
        requestFocus();
    }

    @Override
    public void pause() {
        delegate.pause();
    }

    @Override
    public int getDuration() {
        return delegate.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        return delegate.getCurrentPosition();
    }

    @Override
    public void seekTo(int msec) {
        delegate.seekTo(msec);
    }

    @Override
    public boolean isPlaying() {
        return delegate.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        return delegate.getBufferPercentage();
    }

    @Override
    public boolean canPause() {
        return delegate.canPause();
    }

    @Override
    public boolean canSeekBackward() {
        return delegate.canSeekBackward();
    }

    @Override
    public boolean canSeekForward() {
        return delegate.canSeekForward();
    }

    @Override
    public int getAudioSessionId() {
        return delegate.getAudioSessionId();
    }

    @Override
    public void videoSizeChanged(int width, int height) {
        if (updateVideoSize(width, height)) {
            requestLayout();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        boolean flag = false;
        if (touchListener != null) {
            flag = touchListener.onTouch(this, ev);
        }

        return flag || super.onTouchEvent(ev);
    }

    @Override
    public void setOnTouchListener(OnTouchListener listener) {
        touchListener = listener;
        super.setOnTouchListener(listener);
    }

    @Override
    public void setVideoUri(@Nullable Uri uri) {
        setVideoUri(uri, null);
    }

    @Override
    public void setVideoUri(@Nullable Uri uri, @Nullable RenderBuilder renderBuilder) {
        setVideoURI(uri);
    }

    @Override
    public boolean setVolume(@FloatRange(from = 0.0, to = 1.0) float volume) {
        return false;
    }

    @Override
    public int getBufferedPercent() {
        return getBufferPercentage();
    }

    /**
     * If the video has completed playback, calling {@code restart} will seek to the beginning of the video, and play it.
     *
     * @return {@code true} if the video was successfully restarted, otherwise {@code false}
     */
    @Override
    public boolean restart() {
        return delegate.restart();
    }

    @Override
    public void stopPlayback() {
        delegate.stopPlayback();
    }

    @Override
    public void release() {
        //Purposefully left blank
    }

    @Override
    public boolean trackSelectionAvailable() {
        return false;
    }

    @Override
    public void setTrack(@TrackRenderType int trackType, int trackIndex) {
        //Purposefully left blank
    }

    @Nullable
    @Override
    public Map<Integer, List<MediaFormat>> getAvailableTracks() {
        return null;
    }

    @Override
    public void setListenerMux(EMListenerMux listenerMux) {
        delegate.setListenerMux(listenerMux);
    }

    @Override
    public void onVideoSizeChanged(int width, int height) {
        if (updateVideoSize(width, height)) {
            requestLayout();
        }
    }

    /**
     * Cleans up the resources being held.  This should only be called when
     * destroying the video view
     */
    public void suspend() {
        delegate.suspend();
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
     * @param uri The Uri for the video to play
     * @param headers The headers for the URI request.
     * Note that the cross domain redirection is allowed by default, but that can be
     * changed with key/value pairs through the headers parameter with
     * "android-allow-cross-domain-redirect" as the key and "0" or "1" as the value
     * to disallow or allow cross domain redirection.
     */
    public void setVideoURI(Uri uri, @Nullable Map<String, String> headers) {
        delegate.setVideoURI(uri, headers);

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
        delegate.setOnPreparedListener(listener);
    }

    /**
     * Register a callback to be invoked when the end of a media file
     * has been reached during playback.
     *
     * @param listener The callback that will be run
     */
    public void setOnCompletionListener(@Nullable MediaPlayer.OnCompletionListener listener) {
        delegate.setOnCompletionListener(listener);
    }

    /**
     * Register a callback to be invoked when the status of a network
     * stream's buffer has changed.
     *
     * @param listener the callback that will be run.
     */
    public void setOnBufferingUpdateListener(@Nullable MediaPlayer.OnBufferingUpdateListener listener) {
        delegate.setOnBufferingUpdateListener(listener);
    }

    /**
     * Register a callback to be invoked when a seek operation has been
     * completed.
     *
     * @param listener the callback that will be run
     */
    public void setOnSeekCompleteListener(@Nullable MediaPlayer.OnSeekCompleteListener listener) {
        delegate.setOnSeekCompleteListener(listener);
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
        delegate.setOnErrorListener(listener);
    }

    /**
     * Register a callback to be invoked when an informational event
     * occurs during playback or setup.
     *
     * @param listener The callback that will be run
     */
    public void setOnInfoListener(@Nullable MediaPlayer.OnInfoListener listener) {
        delegate.setOnInfoListener(listener);
    }

    protected void setup(@NonNull Context context, @Nullable AttributeSet attrs) {
        delegate = new NativeVideoDelegate(context, this, this);

        setSurfaceTextureListener(new TextureVideoViewSurfaceListener());

        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();

        updateVideoSize(0, 0);
    }

    protected class TextureVideoViewSurfaceListener implements SurfaceTextureListener {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            delegate.onSurfaceReady(new Surface(surfaceTexture));
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            delegate.onSurfaceSizeChanged(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            surface.release();
            suspend();
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            //Purposefully left blank
        }
    }
}