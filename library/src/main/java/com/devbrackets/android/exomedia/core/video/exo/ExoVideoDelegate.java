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

package com.devbrackets.android.exomedia.core.video.exo;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Surface;

import com.devbrackets.android.exomedia.BuildConfig;
import com.devbrackets.android.exomedia.annotation.TrackRenderType;
import com.devbrackets.android.exomedia.core.EMListenerMux;
import com.devbrackets.android.exomedia.core.builder.DashRenderBuilder;
import com.devbrackets.android.exomedia.core.builder.HlsRenderBuilder;
import com.devbrackets.android.exomedia.core.builder.RenderBuilder;
import com.devbrackets.android.exomedia.core.builder.SmoothStreamRenderBuilder;
import com.devbrackets.android.exomedia.core.exoplayer.EMExoPlayer;
import com.devbrackets.android.exomedia.core.video.delegate.ClearableSurface;
import com.devbrackets.android.exomedia.type.MediaSourceType;
import com.devbrackets.android.exomedia.util.MediaSourceUtil;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.audio.AudioCapabilitiesReceiver;

import java.util.List;
import java.util.Map;


public class ExoVideoDelegate implements AudioCapabilitiesReceiver.Listener {
    protected static final String USER_AGENT_FORMAT = "EMVideoView %s / Android %s / %s";

    protected EMExoPlayer emExoPlayer;
    protected AudioCapabilities audioCapabilities;
    protected AudioCapabilitiesReceiver audioCapabilitiesReceiver;

    protected EMListenerMux listenerMux;
    protected boolean playRequested = false;

    protected Context context;
    protected ClearableSurface clearableSurface;

    public ExoVideoDelegate(@NonNull Context context, @NonNull ClearableSurface clearableSurface) {
        this.context = context;
        this.clearableSurface = clearableSurface;

        setup();
    }

    @Override
    public void onAudioCapabilitiesChanged(AudioCapabilities audioCapabilities) {
        if (!audioCapabilities.equals(this.audioCapabilities)) {
            this.audioCapabilities = audioCapabilities;
        }
    }

    public void setVideoUri(@Nullable Uri uri) {
        RenderBuilder builder = uri == null ? null : getRendererBuilder(MediaSourceUtil.getType(uri), uri);
        setVideoUri(uri, builder);
    }

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

    public boolean restart() {
        if(!emExoPlayer.restart()) {
            return false;
        }

        //Makes sure the listeners get the onPrepared callback
        listenerMux.setNotifiedPrepared(false);
        listenerMux.setNotifiedCompleted(false);

        return true;
    }

    public boolean setVolume(@FloatRange(from = 0.0, to = 1.0) float volume) {
        emExoPlayer.setVolume(volume);
        return true;
    }

    public void seekTo(@IntRange(from = 0) int milliseconds) {
        emExoPlayer.seekTo(milliseconds);
    }

    public boolean isPlaying() {
        return emExoPlayer.getPlayWhenReady();
    }

    public void start() {
        emExoPlayer.setPlayWhenReady(true);
        listenerMux.setNotifiedCompleted(false);
        playRequested = true;
    }

    public void pause() {
        emExoPlayer.setPlayWhenReady(false);
        playRequested = false;
    }

    public void stopPlayback() {
        emExoPlayer.stop();
        playRequested = false;
        listenerMux.clearSurfaceWhenReady(clearableSurface);
    }

    public void suspend() {
        emExoPlayer.release();
        playRequested = false;
    }

    public int getDuration() {
        if (!listenerMux.isPrepared()) {
            return 0;
        }

        return (int)emExoPlayer.getDuration();
    }

    public int getCurrentPosition() {
        if (!listenerMux.isPrepared()) {
            return 0;
        }

        return (int)emExoPlayer.getCurrentPosition();
    }

    public int getBufferedPercent() {
        return emExoPlayer.getBufferedPercentage();
    }

    public boolean trackSelectionAvailable() {
        return true;
    }

    public void setTrack(@TrackRenderType int trackType, int trackIndex) {
        emExoPlayer.setSelectedTrack(trackType, trackIndex);
    }

    @Nullable
    public Map<Integer, List<MediaFormat>> getAvailableTracks() {
        return emExoPlayer.getAvailableTracks();
    }

    public void release() {
        emExoPlayer.release();

        if (audioCapabilitiesReceiver != null) {
            audioCapabilitiesReceiver.unregister();
            audioCapabilitiesReceiver = null;
        }
    }

    public void setListenerMux(EMListenerMux listenerMux) {
        this.listenerMux = listenerMux;
        emExoPlayer.addListener(listenerMux);
    }

    public void onSurfaceReady(Surface surface) {
        emExoPlayer.setSurface(surface);
        if (playRequested) {
            emExoPlayer.setPlayWhenReady(true);
        }
    }

    public void onSurfaceDestroyed() {
        emExoPlayer.blockingClearSurface();
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

    protected void setup() {
        audioCapabilitiesReceiver = new AudioCapabilitiesReceiver(context.getApplicationContext(), this);
        audioCapabilitiesReceiver.register();
        emExoPlayer = new EMExoPlayer(null);

        //Sets the internal listener
        emExoPlayer.setMetadataListener(null);
    }

    /**
     * Creates and returns the correct render builder for the specified VideoType and uri.
     *
     * @param renderType The RenderType to use for creating the correct RenderBuilder
     * @param uri The video's Uri
     * @return The appropriate RenderBuilder
     */
    protected RenderBuilder getRendererBuilder(@NonNull MediaSourceType renderType, @NonNull Uri uri) {
        switch (renderType) {
            case HLS:
                return new HlsRenderBuilder(context.getApplicationContext(), getUserAgent(), uri.toString());
            case DASH:
                return new DashRenderBuilder(context.getApplicationContext(), getUserAgent(), uri.toString());
            case SMOOTH_STREAM:
                return new SmoothStreamRenderBuilder(context.getApplicationContext(), getUserAgent(), uri.toString());
            default:
                return new RenderBuilder(context.getApplicationContext(), getUserAgent(), uri.toString());
        }
    }
}
