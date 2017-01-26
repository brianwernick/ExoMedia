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

import android.content.Context;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.devbrackets.android.exomedia.ExoMedia;
import com.devbrackets.android.exomedia.core.ListenerMux;
import com.devbrackets.android.exomedia.core.api.MediaPlayerApi;
import com.devbrackets.android.exomedia.util.DrmProvider;
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
public class NativeMediaPlayer extends MediaPlayer implements MediaPlayerApi, MediaPlayer.OnBufferingUpdateListener {
    private static final String TAG = "NativeMediaPlayer";

    protected int currentBufferPercent = 0;
    protected ListenerMux listenerMux;

    protected int requestedSeek;

    public NativeMediaPlayer(Context context) {
        super();
        setOnBufferingUpdateListener(this);
    }

    @Override
    public void setDataSource(@NonNull Context context, @Nullable Uri uri) {
        setDataSource(context, uri, (MediaSource) null);
    }

    @Override
    public void setDataSource(@NonNull Context context, @Nullable Uri uri, @Nullable MediaSource mediaSource) {
        try {
            requestedSeek = 0;
            super.setDataSource(context, uri);
        } catch (Exception e) {
            Log.d(TAG, "MediaPlayer: error setting data source", e);
        }
    }

    @Override
    public void setDrmProvider(@Nullable DrmProvider drmProvider) {
        //Purposefully left blank
    }

    @Override
    public void prepare() {
        try {
            super.prepare();
        } catch (Exception e) {
            //Purposefully left blank
        }
    }

    @Override
    public void start() {
        super.start();

        if (listenerMux != null) {
            listenerMux.setNotifiedCompleted(false);
        }
    }

    @Override
    public void stopPlayback() {
        stop();
    }

    @Override
    public boolean restart() {
        if (listenerMux == null || !listenerMux.isPrepared()) {
            return false;
        }

        super.seekTo(0);
        super.start();

        listenerMux.setNotifiedCompleted(false);

        return true;
    }

    @Override
    public int getDuration() {
        if (listenerMux == null || !listenerMux.isPrepared()) {
            return 0;
        }

        return super.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        if (listenerMux == null || !listenerMux.isPrepared()) {
            return 0;
        }

        return super.getCurrentPosition();
    }

    @Override
    public int getBufferedPercent() {
        return currentBufferPercent;
    }

    @Override
    public void seekTo(int msec) {
        if (listenerMux != null && listenerMux.isPrepared()) {
            super.seekTo(msec);
            requestedSeek = 0;
        } else {
            requestedSeek = msec;
        }
    }

    @Override
    public boolean setPlaybackSpeed(float speed) {
        // Marshmallow+ support setting the playback speed natively
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PlaybackParams params = new PlaybackParams();
            params.setSpeed(speed);

            setPlaybackParams(params);
            return true;
        }

        return false;
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

        setOnCompletionListener(listenerMux);
        setOnPreparedListener(listenerMux);
        setOnBufferingUpdateListener(listenerMux);
        setOnSeekCompleteListener(listenerMux);
        setOnErrorListener(listenerMux);
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        currentBufferPercent = percent;
    }

    @Override
    public void onMediaPrepared() {
        if (requestedSeek != 0) {
            seekTo(requestedSeek);
        }
    }
}
