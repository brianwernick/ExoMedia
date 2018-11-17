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

package com.devbrackets.android.exomedia.core.api;

import android.net.Uri;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.devbrackets.android.exomedia.ExoMedia;
import com.devbrackets.android.exomedia.core.ListenerMux;
import com.devbrackets.android.exomedia.core.exoplayer.WindowInfo;
import com.devbrackets.android.exomedia.core.listener.CaptionListener;
import com.devbrackets.android.exomedia.core.video.scale.ScaleType;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.drm.MediaDrmCallback;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;

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

    void setVideoUri(@Nullable Uri uri, @Nullable MediaSource mediaSource);

    /**
     * Sets the {@link MediaDrmCallback} to use when handling DRM for media.
     * This should be called before specifying the videos uri or path
     * <br>
     * <b>NOTE:</b> DRM is only supported on API 18 +
     *
     * @param drmCallback The callback to use when handling DRM media
     */
    void setDrmCallback(@Nullable MediaDrmCallback drmCallback);

    @FloatRange(from = 0.0, to = 1.0)
    float getVolume();

    boolean setVolume(@FloatRange(from = 0.0, to = 1.0) float volume);

    void seekTo(@IntRange(from = 0) long milliseconds);

    boolean isPlaying();

    void start();

    void pause();

    /**
     * Performs the functionality to stop the video in playback
     *
     * @param clearSurface <code>true</code> if the surface should be cleared
     */
    void stopPlayback(boolean clearSurface);

    /**
     * Prepares the media previously specified for playback.  This should only be called after
     * the playback has completed to restart playback from the beginning.
     *
     * @return {@code true} if the media was successfully restarted
     */
    boolean restart();

    void suspend();

    void release();

    @IntRange(from = 0)
    long getDuration();

    @IntRange(from = 0)
    long getCurrentPosition();

    @IntRange(from = 0, to = 100)
    int getBufferedPercent();

    @Nullable
    WindowInfo getWindowInfo();

    /**
     * Sets the playback speed for this MediaPlayer.
     *
     * @param speed The speed to play the media back at
     * @return True if the speed was set
     */
    boolean setPlaybackSpeed(float speed);

    float getPlaybackSpeed();

    void setCaptionListener(@Nullable CaptionListener listener);

    boolean trackSelectionAvailable();

    /**
     * @deprecated use {@link #setTrack(ExoMedia.RendererType, int, int)}
     */
    @Deprecated
    void setTrack(@NonNull ExoMedia.RendererType type, int trackIndex);

    void setTrack(@NonNull ExoMedia.RendererType type, int groupIndex, int trackIndex);

    int getSelectedTrackIndex(@NonNull ExoMedia.RendererType type, int groupIndex);

    /**
     * Clear all selected tracks for the specified renderer.
     * @param type The renderer type
     */
    void clearSelectedTracks(@NonNull ExoMedia.RendererType type);

    /**
     * Retrieves a list of available tracks to select from.  Typically {@link #trackSelectionAvailable()}
     * should be called before this.
     *
     * @return A list of available tracks associated with each track type
     */
    @Nullable
    Map<ExoMedia.RendererType, TrackGroupArray> getAvailableTracks();

    /**
     * Enables or disables the track associated with the <code>type</code>. Note, by default all
     * tracks are enabled
     *
     * @param type The {@link com.devbrackets.android.exomedia.ExoMedia.RendererType} to enable or disable the track for
     * @param enabled <code>true</code> if the track should be enabled.
     */
    void setRendererEnabled(@NonNull ExoMedia.RendererType type, boolean enabled);

    /**
     * Return true if at least one renderer for the given type is enabled
     * @param type The renderer type
     * @return true if at least one renderer for the given type is enabled
     */
    boolean isRendererEnabled(@NonNull ExoMedia.RendererType type);

    void setScaleType(@NonNull ScaleType scaleType);

    @NonNull
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

    void setListenerMux(ListenerMux listenerMux);

    void onVideoSizeChanged(int width, int height, float pixelWidthHeightRatio);

    void setRepeatMode(@Player.RepeatMode int repeatMode);
}