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

import android.net.Uri;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.devbrackets.android.exomedia.annotation.TrackRenderType;
import com.devbrackets.android.exomedia.core.EMListenerMux;
import com.devbrackets.android.exomedia.core.builder.RenderBuilder;
import com.devbrackets.android.exomedia.core.video.scale.ScaleType;
import com.google.android.exoplayer.MediaFormat;

import java.util.List;
import java.util.Map;

/**
 * The basic APIs expected in the backing video view
 * implementations to allow us to create an abstraction
 * between the Native (Android) VideoView and the VideoView
 * using the ExoPlayer.
 */
public interface VideoViewApi {

    interface OnSurfaceSizeChanged {
        void onSurfaceSizeChanged(int width, int height);
    }

    int getHeight();

    int getWidth();

    void setVideoUri(@Nullable Uri uri);

    void setVideoUri(@Nullable Uri uri, @Nullable RenderBuilder renderBuilder);

    boolean setVolume(@FloatRange(from = 0.0, to = 1.0) float volume);

    void seekTo(@IntRange(from = 0) int milliseconds);

    boolean isPlaying();

    void start();

    void pause();

    void stopPlayback();

    boolean restart();

    void suspend();

    void release();

    @IntRange(from = 0)
    int getDuration();

    @IntRange(from = 0)
    int getCurrentPosition();

    @IntRange(from = 0, to = 100)
    int getBufferedPercent();

    boolean trackSelectionAvailable();

    void setTrack(@TrackRenderType int trackType, int trackIndex);

    /**
     * Retrieves a list of available tracks to select from.  Typically {@link #trackSelectionAvailable()}
     * should be called before this.
     *
     * @return A list of available tracks associated with each track type (see {@link com.devbrackets.android.exomedia.annotation.TrackRenderType})
     */
    @Nullable
    Map<Integer, List<MediaFormat>> getAvailableTracks();

    void setScaleType(@NonNull ScaleType scaleType);

    ScaleType getScaleType();

    void setMeasureBasedOnAspectRatioEnabled(boolean doNotMeasureBasedOnAspectRatio);

    /**
     * Sets the rotation for the Video
     *
     * @param rotation The rotation to apply to the video
     * @param fromUser True if the rotation was requested by the user, false if it is from a video configuration
     */
    void setVideoRotation(@IntRange(from = 0, to = 359) int rotation, boolean fromUser);

    void setOnTouchListener(View.OnTouchListener listener);

    void setListenerMux(EMListenerMux listenerMux);

    void onVideoSizeChanged(int width, int height);

    void setOnSizeChangedListener(@Nullable OnSurfaceSizeChanged listener);
}