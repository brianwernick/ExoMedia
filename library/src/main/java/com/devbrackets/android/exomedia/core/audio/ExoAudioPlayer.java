/*
 * Copyright (C) 2016 - 2018 ExoMedia Contributors
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
import android.net.Uri;
import android.os.Build;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.devbrackets.android.exomedia.ExoMedia;
import com.devbrackets.android.exomedia.core.ListenerMux;
import com.devbrackets.android.exomedia.core.api.AudioPlayerApi;
import com.devbrackets.android.exomedia.core.exoplayer.ExoMediaPlayer;
import com.devbrackets.android.exomedia.core.exoplayer.WindowInfo;
import com.devbrackets.android.exomedia.core.listener.MetadataListener;
import com.devbrackets.android.exomedia.listener.OnBufferUpdateListener;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.drm.MediaDrmCallback;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;

import java.util.Map;

/**
 * A {@link AudioPlayerApi} implementation that uses the ExoPlayer
 * as the backing media player.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class ExoAudioPlayer implements AudioPlayerApi {
    @NonNull
    protected final ExoMediaPlayer exoMediaPlayer;
    @NonNull
    protected final Context context;

    protected ListenerMux listenerMux;

    @NonNull
    protected InternalListeners internalListeners = new InternalListeners();

    protected boolean playRequested = false;

    public ExoAudioPlayer(@NonNull Context context) {
        this.context = context;

        exoMediaPlayer = new ExoMediaPlayer(context);
        exoMediaPlayer.setMetadataListener(internalListeners);
        exoMediaPlayer.setBufferUpdateListener(internalListeners);
    }

    @Override
    public void setDataSource(@Nullable Uri uri) {
        setDataSource(uri, null);
    }

    @Override
    public void setDataSource(@Nullable Uri uri, @Nullable MediaSource mediaSource) {
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

    @Override
    public void setDrmCallback(@Nullable MediaDrmCallback drmCallback) {
        exoMediaPlayer.setDrmCallback(drmCallback);
    }

    @Override
    public void prepareAsync() {
        exoMediaPlayer.prepare();
    }

    @Override
    public void reset() {
        //Purposefully left blank
    }

    @Override
    public float getVolumeLeft() {
        return exoMediaPlayer.getVolume();
    }

    @Override
    public float getVolumeRight() {
        return exoMediaPlayer.getVolume();
    }

    @Override
    public void setVolume(@FloatRange(from = 0.0, to = 1.0) float left, @FloatRange(from = 0.0, to = 1.0) float right) {
        //Averages the volume since the ExoPlayer only takes a single channel
        exoMediaPlayer.setVolume((left + right) / 2);
    }

    @Override
    public void seekTo(@IntRange(from = 0) long milliseconds) {
        exoMediaPlayer.seekTo(milliseconds);
    }

    @Override
    public boolean isPlaying() {
        return exoMediaPlayer.getPlayWhenReady();
    }

    @Override
    public void start() {
        exoMediaPlayer.setPlayWhenReady(true);
        listenerMux.setNotifiedCompleted(false);
        playRequested = true;
    }

    @Override
    public void pause() {
        exoMediaPlayer.setPlayWhenReady(false);
        playRequested = false;
    }

    @Override
    public void stopPlayback() {
        exoMediaPlayer.stop();
        playRequested = false;
    }

    /**
     * If the media has completed playback, calling {@code restart} will seek to the beginning of the media, and play it.
     *
     * @return {@code true} if the media was successfully restarted, otherwise {@code false}
     */
    @Override
    public boolean restart() {
        if(!exoMediaPlayer.restart()) {
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

        return exoMediaPlayer.getDuration();
    }

    @Override
    public long getCurrentPosition() {
        if (!listenerMux.isPrepared()) {
            return 0;
        }

        return exoMediaPlayer.getCurrentPosition();
    }

    @Override
    public int getBufferedPercent() {
        return exoMediaPlayer.getBufferedPercentage();
    }

    @Nullable
    @Override
    public WindowInfo getWindowInfo() {
        return exoMediaPlayer.getWindowInfo();
    }

    @Override
    public void release() {
        exoMediaPlayer.release();
    }

    @Override
    public int getAudioSessionId() {
        return exoMediaPlayer.getAudioSessionId();
    }

    @Override
    public boolean setPlaybackSpeed(float speed) {
        return exoMediaPlayer.setPlaybackSpeed(speed);
    }

    @Override
    public float getPlaybackSpeed() {
        return exoMediaPlayer.getPlaybackSpeed();
    }

    @Override
    public void setAudioStreamType(int streamType) {
        exoMediaPlayer.setAudioStreamType(streamType);
    }

    @Override
    public void setWakeMode(@NonNull Context context, int mode) {
        exoMediaPlayer.setWakeMode(context, mode);
    }

    @Override
    public boolean trackSelectionAvailable() {
        return true;
    }

    @Override
    public void setTrack(@NonNull ExoMedia.RendererType type, int trackIndex) {
        exoMediaPlayer.setSelectedTrack(type, trackIndex);
    }

    @Override
    public void setTrack(@NonNull ExoMedia.RendererType type, int groupIndex, int trackIndex) {
        exoMediaPlayer.setSelectedTrack(type, groupIndex, trackIndex);
    }

    @Override
    public int getSelectedTrackIndex(@NonNull ExoMedia.RendererType type, int groupIndex) {
        return exoMediaPlayer.getSelectedTrackIndex(type, groupIndex);
    }

    @Nullable
    @Override
    public Map<ExoMedia.RendererType, TrackGroupArray> getAvailableTracks() {
        return exoMediaPlayer.getAvailableTracks();
    }

    @Override
    public void setListenerMux(ListenerMux listenerMux) {
        if (this.listenerMux != null) {
            exoMediaPlayer.removeListener(this.listenerMux);
            exoMediaPlayer.removeAnalyticsListener(this.listenerMux);
        }

        this.listenerMux = listenerMux;
        exoMediaPlayer.addListener(listenerMux);
        exoMediaPlayer.addAnalyticsListener(listenerMux);
    }

    @Override
    public void onMediaPrepared() {
        //Purposefully left blank
    }

    @Override
    public void setRepeatMode(@Player.RepeatMode int repeatMode) {
        exoMediaPlayer.setRepeatMode(repeatMode);
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