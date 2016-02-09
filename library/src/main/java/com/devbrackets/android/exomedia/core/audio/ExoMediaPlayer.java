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

package com.devbrackets.android.exomedia.core.audio;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;

import com.devbrackets.android.exomedia.BuildConfig;
import com.devbrackets.android.exomedia.core.EMListenerMux;
import com.devbrackets.android.exomedia.core.builder.DashRenderBuilder;
import com.devbrackets.android.exomedia.core.builder.HlsRenderBuilder;
import com.devbrackets.android.exomedia.core.builder.RenderBuilder;
import com.devbrackets.android.exomedia.core.builder.SmoothStreamRenderBuilder;
import com.devbrackets.android.exomedia.core.exoplayer.EMExoPlayer;
import com.devbrackets.android.exomedia.core.api.MediaPlayerApi;
import com.devbrackets.android.exomedia.type.MediaSourceType;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.audio.AudioCapabilitiesReceiver;

/**
 * A {@link MediaPlayerApi} implementation that uses the ExoPlayer
 * as the backing media player.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class ExoMediaPlayer implements MediaPlayerApi, AudioCapabilitiesReceiver.Listener {
    protected static final String USER_AGENT_FORMAT = "EMAudioPlayer %s / Android %s / %s";

    protected EMExoPlayer emExoPlayer;
    protected AudioCapabilities audioCapabilities;
    protected AudioCapabilitiesReceiver audioCapabilitiesReceiver;

    protected Context context;
    protected EMListenerMux listenerMux;
    protected boolean playRequested = false;

    protected int audioStreamType = AudioManager.STREAM_MUSIC;

    public ExoMediaPlayer(Context context) {
        this.context = context;

        audioCapabilitiesReceiver = new AudioCapabilitiesReceiver(context, this);
        audioCapabilitiesReceiver.register();
        emExoPlayer = new EMExoPlayer(null);

        //Sets the internal listener
        emExoPlayer.setMetadataListener(null);
    }

    @Override
    public void setDataSource(Context context, Uri uri) {
        RenderBuilder builder = uri == null ? null : getRendererBuilder(MediaSourceType.get(uri), uri);
        setDataSource(context, uri, builder);
    }

    @Override
    public void setDataSource(Context context, Uri uri, RenderBuilder renderBuilder) {
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
    public void prepareAsync() {
        emExoPlayer.prepare();
    }

    @Override
    public void reset() {
        //Purposefully left blank
    }

    @Override
    public void setVolume(@FloatRange(from = 0.0, to = 1.0) float left, @FloatRange(from = 0.0, to = 1.0) float right) {
        //Averages the volume since the ExoPlayer only takes a single channel
        emExoPlayer.setVolume((left + right) / 2);
    }

    @Override
    public void seekTo(@IntRange(from = 0) int milliseconds) {
        if (!listenerMux.isPrepared()) {
            return;
        }

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
        emExoPlayer.setPlayWhenReady(false);
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
    public void release() {
        emExoPlayer.release();

        if (audioCapabilitiesReceiver != null) {
            audioCapabilitiesReceiver.unregister();
            audioCapabilitiesReceiver = null;
        }
    }


    @Override
    public int getAudioSessionId() {
        return emExoPlayer.getAudioSessionId();
    }

    @Override
    public void setAudioStreamType(int streamType) {
        this.audioStreamType = streamType;
    }

    @Override
    public void setWakeMode(Context context, int mode) {
        emExoPlayer.setWakeMode(context, mode);
    }

    @Override
    public void setListenerMux(EMListenerMux listenerMux) {
        this.listenerMux = listenerMux;
        emExoPlayer.addListener(listenerMux);
    }

    @Override
    public void onAudioCapabilitiesChanged(AudioCapabilities audioCapabilities) {
        if (!audioCapabilities.equals(this.audioCapabilities)) {
            this.audioCapabilities = audioCapabilities;
        }
    }

    /**
     * Creates and returns the correct render builder for the specified AudioType and uri.
     *
     * @param renderType The RenderType to use for creating the correct RenderBuilder
     * @param uri The audio item's Uri
     * @return The appropriate RenderBuilder
     */
    protected RenderBuilder getRendererBuilder(MediaSourceType renderType, Uri uri) {
        switch (renderType) {
            case HLS:
                return new HlsRenderBuilder(context, getUserAgent(), uri.toString(), audioStreamType);
            case DASH:
                return new DashRenderBuilder(context, getUserAgent(), uri.toString(), audioStreamType);
            case SMOOTH_STREAM:
                return new SmoothStreamRenderBuilder(context, getUserAgent(), uri.toString(), audioStreamType);
            default:
                return new RenderBuilder(context, getUserAgent(), uri.toString(), audioStreamType);
        }
    }

    /**
     * Retrieves the user agent that the EMAudioPlayer will use when communicating
     * with media servers
     *
     * @return The String user agent for the EMAudioPlayer
     */
    protected String getUserAgent() {
        return String.format(USER_AGENT_FORMAT, BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")", Build.VERSION.RELEASE, Build.MODEL);
    }
}