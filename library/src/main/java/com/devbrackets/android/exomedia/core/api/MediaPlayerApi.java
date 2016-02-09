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

package com.devbrackets.android.exomedia.core.api;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;

import com.devbrackets.android.exomedia.core.EMListenerMux;
import com.devbrackets.android.exomedia.core.builder.RenderBuilder;

/**
 * The basic APIs expected in the backing media player
 * implementations to allow us to create an abstraction
 * between the Native (Android) MediaPlayer and the AudioPlayer
 * using the ExoPlayer.
 */
public interface MediaPlayerApi {
    void setDataSource(Context context, Uri uri);

    void setDataSource(Context context, Uri uri, RenderBuilder renderBuilder);

    void prepareAsync();

    boolean isPlaying();

    void start();

    void pause();

    void stopPlayback();

    void release();

    void reset();

    @IntRange(from = 0)
    int getDuration();

    @IntRange(from = 0)
    int getCurrentPosition();

    @IntRange(from = 0, to = 100)
    int getBufferedPercent();

    int getAudioSessionId();

    void setAudioStreamType(int streamType);

    void setVolume(@FloatRange(from = 0.0, to = 1.0) float left, @FloatRange(from = 0.0, to = 1.0) float right);

    void seekTo(@IntRange(from = 0) int milliseconds);

    void setWakeMode(Context context, int mode);

    void setListenerMux(EMListenerMux listenerMux);
}
