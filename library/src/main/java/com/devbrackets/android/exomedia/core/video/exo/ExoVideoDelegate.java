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
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Surface;

import com.devbrackets.android.exomedia.ExoMedia;
import com.devbrackets.android.exomedia.core.EMListenerMux;
import com.devbrackets.android.exomedia.core.exoplayer.EMExoPlayer;
import com.devbrackets.android.exomedia.core.listener.MetadataListener;
import com.devbrackets.android.exomedia.core.video.ClearableSurface;
import com.devbrackets.android.exomedia.listener.OnBufferUpdateListener;
import com.devbrackets.android.exomedia.util.DrmProvider;
import com.google.android.exoplayer2.audio.AudioCapabilities;
import com.google.android.exoplayer2.audio.AudioCapabilitiesReceiver;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;

import java.util.List;
import java.util.Map;


public class ExoVideoDelegate implements AudioCapabilitiesReceiver.Listener {
    protected EMExoPlayer emExoPlayer;
    protected AudioCapabilities audioCapabilities;
    protected AudioCapabilitiesReceiver audioCapabilitiesReceiver;

    protected EMListenerMux listenerMux;
    protected boolean playRequested = false;

    protected Context context;
    protected ClearableSurface clearableSurface;

    @Nullable
    protected DrmProvider drmProvider;
    @NonNull
    protected InternalListeners internalListeners = new InternalListeners();

    public ExoVideoDelegate(@NonNull Context context, @NonNull ClearableSurface clearableSurface) {
        this.context = context.getApplicationContext();
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
        setVideoUri(uri, null);
    }

    public void setVideoUri(@Nullable Uri uri, @Nullable MediaSource mediaSource) {
        if (mediaSource != null) {
            emExoPlayer.setMediaSource(mediaSource);
            listenerMux.setNotifiedCompleted(false);
        } else if (uri != null) {
            emExoPlayer.setUri(uri);
            listenerMux.setNotifiedCompleted(false);
        } else {
            emExoPlayer.setMediaSource(null);
        }

        //Makes sure the listeners get the onPrepared callback
        listenerMux.setNotifiedPrepared(false);
        emExoPlayer.seekTo(0);
    }

    /**
     * Sets the {@link DrmProvider} to use when handling DRM for media.
     * This should be called before specifying the videos uri or path
     * <br>
     * <b>NOTE:</b> DRM is only supported on API 18 +
     *
     * @param drmProvider The provider to use when handling DRM media
     */
    public void setDrmProvider(@Nullable DrmProvider drmProvider) {
        this.drmProvider = drmProvider;
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

    public void setTrack(ExoMedia.RendererType trackType, int trackIndex) {
        emExoPlayer.setSelectedTrack(trackType, trackIndex);
    }

    @Nullable
    public Map<ExoMedia.RendererType, TrackGroupArray> getAvailableTracks() {
        return emExoPlayer.getAvailableTracks();
    }

    public boolean setPlaybackSpeed(float speed) {
        return emExoPlayer.setPlaybackSpeed(speed);
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

    protected void setup() {
        initExoPlayer();
        audioCapabilitiesReceiver = new AudioCapabilitiesReceiver(context, this);
        audioCapabilitiesReceiver.register();
    }

    protected void initExoPlayer() {
        emExoPlayer = new EMExoPlayer(context);

        emExoPlayer.setMetadataListener(internalListeners);
        emExoPlayer.setBufferUpdateListener(internalListeners);
    }

    protected class InternalListeners implements MetadataListener, OnBufferUpdateListener {
        @Override
        public void onMetadata(Metadata metadata) {
            listenerMux.onMetadata(metadata);
        }

        @Override
        public void onBufferingUpdate(@IntRange(from = 0, to = 100) int percent) {
            listenerMux.onBufferingUpdate(percent);
        }
    }
}
