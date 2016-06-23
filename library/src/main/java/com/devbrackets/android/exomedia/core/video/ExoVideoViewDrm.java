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
import android.net.Uri;
import android.os.Build;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.appcompat.BuildConfig;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;

import com.devbrackets.android.exomedia.annotation.TrackRenderType;
import com.devbrackets.android.exomedia.core.EMListenerMux;
import com.devbrackets.android.exomedia.core.EMListenerMuxDrm;
import com.devbrackets.android.exomedia.core.api.VideoViewApi;
import com.devbrackets.android.exomedia.core.builder.DashRenderBuilderWV;
import com.devbrackets.android.exomedia.core.builder.HlsRenderBuilder;
import com.devbrackets.android.exomedia.core.builder.RenderBuilder;
import com.devbrackets.android.exomedia.core.builder.SmoothStreamRenderBuilder;
import com.devbrackets.android.exomedia.core.builder.WidevineTestMediaDrmCallback;
import com.devbrackets.android.exomedia.core.exoplayer.EMExoPlayer;
import com.devbrackets.android.exomedia.type.MediaSourceType;
import com.devbrackets.android.exomedia.util.MediaSourceUtil;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.audio.AudioCapabilitiesReceiver;

import java.util.List;
import java.util.Map;

//import com.devbrackets.android.exomedia.BuildConfig;

/**
 * A {@link VideoViewApi} implementation that uses the ExoPlayer
 * as the backing media player.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class ExoVideoViewDrm extends ResizingSurfaceView implements VideoViewApi, AudioCapabilitiesReceiver.Listener {
    protected static final String USER_AGENT_FORMAT = "EMVideoView %s / Android %s / %s";

    protected EMExoPlayer emExoPlayer;
    protected AudioCapabilities audioCapabilities;
    protected AudioCapabilitiesReceiver audioCapabilitiesReceiver;

    protected EMListenerMuxDrm listenerMux;
    protected boolean playRequested = false;

    public ExoVideoViewDrm(Context context) {
        super(context);
        setup();
    }

    public ExoVideoViewDrm(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public ExoVideoViewDrm(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup();
    }

    public ExoVideoViewDrm(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setup();
    }

    @Override
    public void setVideoUri(@Nullable Uri uri) {
        RenderBuilder builder = uri == null ? null : getRendererBuilder(MediaSourceUtil.getType(uri), uri);
        setVideoUri(uri, builder);
    }

    @Override
    public void setVideoUri(@Nullable Uri uri, @Nullable RenderBuilder renderBuilder) {
        playRequested = false;

        if (uri == null) {
            emExoPlayer.replaceRenderBuilder(null);
        } else {
            emExoPlayer.replaceRenderBuilder(renderBuilder);
            listenerMux.setNotifiedCompleted(false);
        }

        //Makes sure the listeners get the onPrepared callback
        listenerMux.setNotifiedPrepared(false);
        emExoPlayer.seekTo(0);
    }

    @Override
    public boolean restart() {
        if(!emExoPlayer.restart()) {
            return false;
        }

        //Makes sure the listeners get the onPrepared callback
        listenerMux.setNotifiedPrepared(false);
        listenerMux.setNotifiedCompleted(false);

        return true;
    }

    @Override
    public boolean setVolume(@FloatRange(from = 0.0, to = 1.0) float volume) {
        emExoPlayer.setVolume(volume);
        return true;
    }

    @Override
    public void seekTo(@IntRange(from = 0) int milliseconds) {
        emExoPlayer.seekTo(milliseconds);
    }

    @Override
    public boolean isPlaying() {
        return emExoPlayer.getPlayWhenReady();
    }

    @Override
    public void start() {
        emExoPlayer.setPlayWhenReady(true);
        listenerMux.setNotifiedCompleted(false);
        playRequested = true;
    }

    @Override
    public void pause() {
        emExoPlayer.setPlayWhenReady(false);
        playRequested = false;
    }

    @Override
    public void stopPlayback() {
        emExoPlayer.stop();
        playRequested = false;
        listenerMux.clearSurfaceWhenReady(this);
    }

    @Override
    public void suspend() {
        emExoPlayer.release();
        playRequested = false;
    }

    @Override
    public int getDuration() {
        if (!listenerMux.isPrepared()) {
            return 0;
        }

        return (int)emExoPlayer.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        if (!listenerMux.isPrepared()) {
            return 0;
        }

        return (int)emExoPlayer.getCurrentPosition();
    }

    @Override
    public int getBufferedPercent() {
        return emExoPlayer.getBufferedPercentage();
    }

    @Override
    public boolean trackSelectionAvailable() {
        return true;
    }

    @Override
    public void setTrack(@TrackRenderType int trackType, int trackIndex) {
        emExoPlayer.setSelectedTrack(trackType, trackIndex);
    }

    @Nullable
    @Override
    public Map<Integer, List<MediaFormat>> getAvailableTracks() {
        return emExoPlayer.getAvailableTracks();
    }

    @Override
    public void onAudioCapabilitiesChanged(AudioCapabilities audioCapabilities) {
        if (!audioCapabilities.equals(this.audioCapabilities)) {
            this.audioCapabilities = audioCapabilities;
        }
    }

    @Override
    public void release() {
        emExoPlayer.release();

        if (audioCapabilitiesReceiver != null) {
            audioCapabilitiesReceiver.unregister();
            audioCapabilitiesReceiver = null;
        }
    }

    @Override
    public void setListenerMux(EMListenerMux listenerMux) {
//        this.listenerMux = listenerMux;
//        emExoPlayer.addListener(listenerMux);
    }

    @Override
    public void setListenerMuxDrm(EMListenerMuxDrm listenerMux) {
        this.listenerMux = listenerMux;
        emExoPlayer.addListener(listenerMux);
    }

    @Override
    public void onVideoSizeChanged(int width, int height) {
        if (updateVideoSize(width, height)) {
            requestLayout();
        }
    }

    protected void setup() {
        audioCapabilitiesReceiver = new AudioCapabilitiesReceiver(getContext().getApplicationContext(), this);
        audioCapabilitiesReceiver.register();
        emExoPlayer = new EMExoPlayer(null);

        //Sets the internal listener
        emExoPlayer.setMetadataListener(null);

        //setSurfaceTextureListener(new EMExoVideoSurfaceTextureListener());
        getHolder().addCallback(new EMExoVideoSurfaceTextureListener());


        updateVideoSize(0, 0);
    }

    /**
     * Creates and returns the correct render builder for the specified VideoType and uri.
     *
     * @param renderType The RenderType to use for creating the correct RenderBuilder
     * @param uri The video's Uri
     * @return The appropriate RenderBuilder
     */
    protected RenderBuilder getRendererBuilder(@NonNull MediaSourceType renderType, @NonNull Uri uri) {
        Log.i("WideVineTest", "getRendererBuilder Exovideoview");
        switch (renderType) {
            case HLS:
                return new HlsRenderBuilder(getContext().getApplicationContext(), getUserAgent(), uri.toString());
            case DASH:
                return new DashRenderBuilderWV(getContext(), getUserAgent(), uri.toString(),
                        new WidevineTestMediaDrmCallback("", "widevine_test"));
                //return new DashRenderBuilder(getContext().getApplicationContext(), getUserAgent(), uri.toString());
            case SMOOTH_STREAM:
                return new SmoothStreamRenderBuilder(getContext().getApplicationContext(), getUserAgent(), uri.toString());
            default:
                return new RenderBuilder(getContext().getApplicationContext(), getUserAgent(), uri.toString());
        }
    }

    /**
     * Retrieves the user agent that the EMVideoView will use when communicating
     * with media servers
     *
     * @return The String user agent for the EMVideoView
     */
    public String getUserAgent() {
        return String.format(USER_AGENT_FORMAT, BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")", Build.VERSION.RELEASE, Build.MODEL);
    }

    protected class EMExoVideoSurfaceTextureListener implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            if(emExoPlayer != null){
                emExoPlayer.setSurface(holder.getSurface());
                if (playRequested) {
                    emExoPlayer.setPlayWhenReady(true);
                }
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            if(emExoPlayer != null){
                emExoPlayer.blockingClearSurface();
            }
        }
    }
}
