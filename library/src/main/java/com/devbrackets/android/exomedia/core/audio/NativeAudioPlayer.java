/*
 * Copyright (C) 2016-2017 Brian Wernick
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

import android.content.Context;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.devbrackets.android.exomedia.ExoMedia;
import com.devbrackets.android.exomedia.core.ListenerMux;
import com.devbrackets.android.exomedia.core.api.AudioPlayerApi;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.drm.MediaDrmCallback;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;

import java.util.Map;

/**
 * A simple MediaPlayer implementation that extends the
 * one provided by the system to add integration with
 * the {@link ListenerMux} and to mitigate state errors.
 * <p>
 * NOTE: The <code>listenerMux</code> shouldn't be null when any
 * method utilizing it is called, however there are some cases on
 * Amazon devices where they incorrectly call these methods when
 * setting up the MediaPlayer (when in IDLE state)
 */
public class NativeAudioPlayer implements AudioPlayerApi {
    private static final String TAG = "NativeMediaPlayer";

    @NonNull
    protected final Context context;
    @NonNull
    protected final MediaPlayer mediaPlayer;
    @NonNull
    protected InternalListeners internalListeners = new InternalListeners();

    protected ListenerMux listenerMux;

    protected long requestedSeek;
    protected int currentBufferPercent = 0;

    @FloatRange(from = 0.0, to = 1.0)
    protected float volumeLeft = 1.0f;

    @FloatRange(from = 0.0, to = 1.0)
    protected float volumeRight = 1.0f;

    public NativeAudioPlayer(@NonNull Context context) {
        this.context = context;

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnBufferingUpdateListener(internalListeners);
    }

    @Override
    public void setDataSource(@Nullable Uri uri) {
        setDataSource(uri, null);
    }

    @Override
    public void setDataSource(@Nullable Uri uri, @Nullable MediaSource mediaSource) {
        try {
            requestedSeek = 0;
            mediaPlayer.setDataSource(context, uri);
        } catch (Exception e) {
            Log.d(TAG, "MediaPlayer: error setting data source", e);
        }
    }

    @Override
    public void setDrmCallback(@Nullable MediaDrmCallback drmCallback) {
        //Purposefully left blank
    }

    @Override
    public void prepareAsync() {
        try {
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            //Purposefully left blank
        }
    }

    @Override
    public void reset() {
        mediaPlayer.reset();
    }

    @Override
    public float getVolumeLeft() {
        return volumeLeft;
    }

    @Override
    public float getVolumeRight() {
        return volumeRight;
    }

    @Override
    public void setVolume(@FloatRange(from = 0.0, to = 1.0) float left, @FloatRange(from = 0.0, to = 1.0) float right) {
        volumeLeft = left;
        volumeRight = right;

        mediaPlayer.setVolume(left, right);
    }

    @Override
    public void seekTo(@IntRange(from = 0) long milliseconds) {
        if (listenerMux != null && listenerMux.isPrepared()) {
            mediaPlayer.seekTo((int) milliseconds);
            requestedSeek = 0;
        } else {
            requestedSeek = milliseconds;
        }
    }

    @Override
    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }

    @Override
    public void start() {
        mediaPlayer.start();

        if (listenerMux != null) {
            listenerMux.setNotifiedCompleted(false);
        }
    }

    @Override
    public void pause() {
        mediaPlayer.pause();
    }

    @Override
    public void stopPlayback() {
        mediaPlayer.stop();
    }

    @Override
    public boolean restart() {
        if (listenerMux == null || !listenerMux.isPrepared()) {
            return false;
        }

        mediaPlayer.seekTo(0);
        mediaPlayer.start();

        listenerMux.setNotifiedCompleted(false);

        return true;
    }

    @Override
    public long getDuration() {
        if (listenerMux == null || !listenerMux.isPrepared()) {
            return 0;
        }

        return mediaPlayer.getDuration();
    }

    @Override
    public long getCurrentPosition() {
        if (listenerMux == null || !listenerMux.isPrepared()) {
            return 0;
        }

        return mediaPlayer.getCurrentPosition();
    }

    @Override
    public int getBufferedPercent() {
        return currentBufferPercent;
    }

    @Override
    public void release() {
        mediaPlayer.release();
    }

    @Override
    public int getAudioSessionId() {
        return mediaPlayer.getAudioSessionId();
    }

    @Override
    public boolean setPlaybackSpeed(float speed) {
        // Marshmallow+ support setting the playback speed natively
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PlaybackParams params = new PlaybackParams();
            params.setSpeed(speed);

            mediaPlayer.setPlaybackParams(params);
            return true;
        }

        return false;
    }

    @Override
    public void setAudioStreamType(int streamType) {
        mediaPlayer.setAudioStreamType(streamType);
    }

    @Override
    public void setWakeMode(Context context, int mode) {
        mediaPlayer.setWakeMode(context, mode);
    }

    @Override
    public boolean trackSelectionAvailable() {
        return false;
    }

    @Override
    public void setTrack(ExoMedia.RendererType trackType, int trackIndex) {
        //Purposefully left blank
    }

    @Nullable
    @Override
    public Map<ExoMedia.RendererType, TrackGroupArray> getAvailableTracks() {
        return null;
    }

    @Override
    public void setListenerMux(ListenerMux listenerMux) {
        this.listenerMux = listenerMux;

        mediaPlayer.setOnCompletionListener(listenerMux);
        mediaPlayer.setOnPreparedListener(listenerMux);
        mediaPlayer.setOnBufferingUpdateListener(listenerMux);
        mediaPlayer.setOnSeekCompleteListener(listenerMux);
        mediaPlayer.setOnErrorListener(listenerMux);
    }

    @Override
    public void onMediaPrepared() {
        if (requestedSeek != 0) {
            seekTo(requestedSeek);
        }
    }

    @Override
    public void setRepeatMode(@Player.RepeatMode int repeatMode) {
        // Purposefully left blank
    }

    protected class InternalListeners implements MediaPlayer.OnBufferingUpdateListener {
        @Override
        public void onBufferingUpdate(MediaPlayer mediaPlayer, int percent) {
            listenerMux.onBufferingUpdate(percent);
            currentBufferPercent = percent;
        }
    }
}
