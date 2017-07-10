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
import com.devbrackets.android.exomedia.core.ListenerMux;
import com.devbrackets.android.exomedia.core.exoplayer.ExoMediaPlayer;
import com.devbrackets.android.exomedia.core.listener.MetadataListener;
import com.devbrackets.android.exomedia.core.video.ClearableSurface;
import com.devbrackets.android.exomedia.listener.OnBufferUpdateListener;
import com.google.android.exoplayer2.drm.MediaDrmCallback;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;

import java.util.Map;

public class ExoVideoDelegate {
    protected ExoMediaPlayer exoMediaPlayer;

    protected ListenerMux listenerMux;
    protected boolean playRequested = false;

    protected Context context;
    protected ClearableSurface clearableSurface;

    @NonNull
    protected InternalListeners internalListeners = new InternalListeners();

    public ExoVideoDelegate(@NonNull Context context, @NonNull ClearableSurface clearableSurface) {
        this.context = context.getApplicationContext();
        this.clearableSurface = clearableSurface;

        setup();
    }

    public void setVideoUri(@Nullable Uri uri) {
        setVideoUri(uri, null);
    }

    public void setVideoUri(@Nullable Uri uri, @Nullable MediaSource mediaSource) {
        //Makes sure the listeners get the onPrepared callback
        listenerMux.setNotifiedPrepared(false);
        exoMediaPlayer.seekTo(0);

        if (mediaSource != null) {
            exoMediaPlayer.setMediaSource(mediaSource);
            listenerMux.setNotifiedCompleted(false);
        } else if (uri != null) {
            exoMediaPlayer.setUri(uri);
            listenerMux.setNotifiedCompleted(false);
        } else {
            exoMediaPlayer.setMediaSource(null);
        }
    }

    /**
     * Sets the {@link MediaDrmCallback} to use when handling DRM for media.
     * This should be called before specifying the videos uri or path
     * <br>
     * <b>NOTE:</b> DRM is only supported on API 18 +
     *
     * @param drmCallback The callback to use when handling DRM media
     */
    public void setDrmCallback(@Nullable MediaDrmCallback drmCallback) {
        exoMediaPlayer.setDrmCallback(drmCallback);
    }

    public boolean restart() {
        if(!exoMediaPlayer.restart()) {
            return false;
        }

        //Makes sure the listeners get the onPrepared callback
        listenerMux.setNotifiedPrepared(false);
        listenerMux.setNotifiedCompleted(false);

        return true;
    }

    public boolean setVolume(@FloatRange(from = 0.0, to = 1.0) float volume) {
        exoMediaPlayer.setVolume(volume);
        return true;
    }

    public void seekTo(@IntRange(from = 0) long milliseconds) {
        exoMediaPlayer.seekTo(milliseconds);
    }

    public boolean isPlaying() {
        return exoMediaPlayer.getPlayWhenReady();
    }

    public void start() {
        exoMediaPlayer.setPlayWhenReady(true);
        listenerMux.setNotifiedCompleted(false);
        playRequested = true;
    }

    public void pause() {
        exoMediaPlayer.setPlayWhenReady(false);
        playRequested = false;
    }

    /**
     * Performs the functionality to stop the video in playback
     *
     * @param clearSurface <code>true</code> if the surface should be cleared
     */
    public void stopPlayback(boolean clearSurface) {
        exoMediaPlayer.stop();
        playRequested = false;

        if (clearSurface) {
            listenerMux.clearSurfaceWhenReady(clearableSurface);
        }
    }

    public void suspend() {
        exoMediaPlayer.release();
        playRequested = false;
    }

    public long getDuration() {
        if (!listenerMux.isPrepared()) {
            return 0;
        }

        return exoMediaPlayer.getDuration();
    }

    public long getCurrentPosition() {
        if (!listenerMux.isPrepared()) {
            return 0;
        }

        return exoMediaPlayer.getCurrentPosition();
    }

    public int getBufferedPercent() {
        return exoMediaPlayer.getBufferedPercentage();
    }

    public boolean trackSelectionAvailable() {
        return true;
    }

    public void setTrack(ExoMedia.RendererType trackType, int trackIndex) {
        exoMediaPlayer.setSelectedTrack(trackType, trackIndex);
    }

    @Nullable
    public Map<ExoMedia.RendererType, TrackGroupArray> getAvailableTracks() {
        return exoMediaPlayer.getAvailableTracks();
    }

    public boolean setPlaybackSpeed(float speed) {
        return exoMediaPlayer.setPlaybackSpeed(speed);
    }

    public void release() {
        exoMediaPlayer.release();
    }

    public void setListenerMux(ListenerMux listenerMux) {
        if (this.listenerMux != null) {
            exoMediaPlayer.removeListener(this.listenerMux);
        }

        this.listenerMux = listenerMux;
        exoMediaPlayer.addListener(listenerMux);
    }

    public void onSurfaceReady(Surface surface) {
        exoMediaPlayer.setSurface(surface);
        if (playRequested) {
            exoMediaPlayer.setPlayWhenReady(true);
        }
    }

    public void onSurfaceDestroyed() {
        exoMediaPlayer.blockingClearSurface();
    }

    protected void setup() {
        initExoPlayer();
    }

    protected void initExoPlayer() {
        exoMediaPlayer = new ExoMediaPlayer(context);

        exoMediaPlayer.setMetadataListener(internalListeners);
        exoMediaPlayer.setBufferUpdateListener(internalListeners);
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
