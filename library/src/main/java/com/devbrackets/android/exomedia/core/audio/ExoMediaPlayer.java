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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.devbrackets.android.exomedia.ExoMedia;
import com.devbrackets.android.exomedia.core.ListenerMux;
import com.devbrackets.android.exomedia.core.api.MediaPlayerApi;
import com.devbrackets.android.exomedia.core.exoplayer.EMExoPlayer;
import com.devbrackets.android.exomedia.core.listener.MetadataListener;
import com.devbrackets.android.exomedia.listener.OnBufferUpdateListener;
import com.devbrackets.android.exomedia.util.DrmProvider;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;

import java.util.Map;

/**
 * A {@link MediaPlayerApi} implementation that uses the ExoPlayer
 * as the backing media player.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class ExoMediaPlayer implements MediaPlayerApi {
    @NonNull
    protected final EMExoPlayer emExoPlayer;
    @NonNull
    protected final Context context;

    protected ListenerMux listenerMux;

    @Nullable
    protected DrmProvider drmProvider;
    @NonNull
    protected InternalListeners internalListeners = new InternalListeners();

    protected boolean playRequested = false;
    protected int audioStreamType = AudioManager.STREAM_MUSIC;

    public ExoMediaPlayer(@NonNull Context context) {
        this.context = context;

        emExoPlayer = new EMExoPlayer(context);
        emExoPlayer.setMetadataListener(internalListeners);
        emExoPlayer.setBufferUpdateListener(internalListeners);
    }

    @Override
    public void setDataSource(@Nullable Uri uri) {
        setDataSource(uri, null);
    }

    @Override
    public void setDataSource(@Nullable Uri uri, @Nullable MediaSource mediaSource) {
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

    @Override
    public void setDrmProvider(@Nullable DrmProvider drmProvider) {
        this.drmProvider = drmProvider;
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
    public void seekTo(@IntRange(from = 0) long milliseconds) {
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
    }

    /**
     * If the media has completed playback, calling {@code restart} will seek to the beginning of the media, and play it.
     *
     * @return {@code true} if the media was successfully restarted, otherwise {@code false}
     */
    @Override
    public boolean restart() {
        if(!emExoPlayer.restart()) {
            return false;
        }

        listenerMux.setNotifiedCompleted(false);
        listenerMux.setNotifiedPrepared(false);

        return true;
    }

    @Override
    public long getDuration() {
        if (!listenerMux.isPrepared()) {
            return 0;
        }

        return emExoPlayer.getDuration();
    }

    @Override
    public long getCurrentPosition() {
        if (!listenerMux.isPrepared()) {
            return 0;
        }

        return emExoPlayer.getCurrentPosition();
    }

    @Override
    public int getBufferedPercent() {
        return emExoPlayer.getBufferedPercentage();
    }

    @Override
    public void release() {
        emExoPlayer.release();
    }

    @Override
    public int getAudioSessionId() {
        return emExoPlayer.getAudioSessionId();
    }

    @Override
    public boolean setPlaybackSpeed(float speed) {
        return emExoPlayer.setPlaybackSpeed(speed);
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
    public boolean trackSelectionAvailable() {
        return true;
    }

    @Override
    public void setTrack(ExoMedia.RendererType type, int trackIndex) {
        emExoPlayer.setSelectedTrack(type, trackIndex);
    }

    @Nullable
    @Override
    public Map<ExoMedia.RendererType, TrackGroupArray> getAvailableTracks() {
        return emExoPlayer.getAvailableTracks();
    }

    @Override
    public void setListenerMux(ListenerMux listenerMux) {
        this.listenerMux = listenerMux;
        emExoPlayer.addListener(listenerMux);
    }

    @Override
    public void onMediaPrepared() {
        //Purposefully left blank
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