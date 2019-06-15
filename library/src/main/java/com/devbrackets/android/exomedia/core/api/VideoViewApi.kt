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

package com.devbrackets.android.exomedia.core.api

import android.net.Uri
import android.support.annotation.FloatRange
import android.support.annotation.IntRange
import android.view.View

import com.devbrackets.android.exomedia.ExoMedia
import com.devbrackets.android.exomedia.core.ListenerMux
import com.devbrackets.android.exomedia.core.exoplayer.WindowInfo
import com.devbrackets.android.exomedia.core.listener.CaptionListener
import com.devbrackets.android.exomedia.core.video.scale.ScaleType
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.drm.MediaDrmCallback
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray

/**
 * The basic APIs expected in the backing video view
 * implementations to allow us to create an abstraction
 * between the Native (Android) VideoView and the VideoView
 * using the ExoPlayer.
 */
interface VideoViewApi {
    @get:FloatRange(from = 0.0, to = 1.0)
    var volume: Float

    val isPlaying: Boolean

    @get:IntRange(from = 0)
    val duration: Long

    @get:IntRange(from = 0)
    val currentPosition: Long

    @get:IntRange(from = 0, to = 100)
    val bufferedPercent: Int

    val windowInfo: WindowInfo?

    val playbackSpeed: Float

    /**
     * Retrieves a list of available tracks to select from.  Typically [.trackSelectionAvailable]
     * should be called before this.
     *
     * @return A list of available tracks associated with each track type
     */
    val availableTracks: Map<ExoMedia.RendererType, TrackGroupArray>?

    var scaleType: ScaleType

    interface OnSurfaceSizeChanged {
        fun onSurfaceSizeChanged(width: Int, height: Int)
    }

    fun getHeight(): Int
    fun getWidth(): Int

    fun setVideoUri(uri: Uri?)

    fun setVideoUri(uri: Uri?, mediaSource: MediaSource?)

    /**
     * Sets the [MediaDrmCallback] to use when handling DRM for media.
     * This should be called before specifying the videos uri or path
     * <br></br>
     * **NOTE:** DRM is only supported on API 18 +
     *
     * @param drmCallback The callback to use when handling DRM media
     */
    fun setDrmCallback(drmCallback: MediaDrmCallback?)

    fun setVolume(@FloatRange(from = 0.0, to = 1.0) volume: Float): Boolean

    fun seekTo(@IntRange(from = 0) milliseconds: Long)

    fun start()

    fun pause()

    /**
     * Performs the functionality to stop the video in playback
     *
     * @param clearSurface `true` if the surface should be cleared
     */
    fun stopPlayback(clearSurface: Boolean)

    /**
     * Prepares the media previously specified for playback.  This should only be called after
     * the playback has completed to restart playback from the beginning.
     *
     * @return `true` if the media was successfully restarted
     */
    fun restart(): Boolean

    fun suspend()

    fun release()

    /**
     * Sets the playback speed for this MediaPlayer.
     *
     * @param speed The speed to play the media back at
     * @return True if the speed was set
     */
    fun setPlaybackSpeed(speed: Float): Boolean

    fun setCaptionListener(listener: CaptionListener?)

    fun trackSelectionAvailable(): Boolean

    fun setTrack(type: ExoMedia.RendererType, groupIndex: Int, trackIndex: Int)

    fun getSelectedTrackIndex(type: ExoMedia.RendererType, groupIndex: Int): Int

    /**
     * Clear all selected tracks for the specified renderer.
     * @param type The renderer type
     */
    fun clearSelectedTracks(type: ExoMedia.RendererType)

    /**
     * Enables or disables the track associated with the `type`. Note, by default all
     * tracks are enabled
     *
     * @param type The [com.devbrackets.android.exomedia.ExoMedia.RendererType] to enable or disable the track for
     * @param enabled `true` if the track should be enabled.
     */
    fun setRendererEnabled(type: ExoMedia.RendererType, enabled: Boolean)

    /**
     * Return true if at least one renderer for the given type is enabled
     * @param type The renderer type
     * @return true if at least one renderer for the given type is enabled
     */
    fun isRendererEnabled(type: ExoMedia.RendererType): Boolean

    fun setMeasureBasedOnAspectRatioEnabled(doNotMeasureBasedOnAspectRatio: Boolean)

    /**
     * Sets the rotation for the Video
     *
     * @param rotation The rotation to apply to the video
     * @param fromUser True if the rotation was requested by the user, false if it is from a video configuration
     */
    fun setVideoRotation(@IntRange(from = 0, to = 359) rotation: Int, fromUser: Boolean)

    fun setOnTouchListener(listener: View.OnTouchListener?)

    fun setListenerMux(listenerMux: ListenerMux)

    fun onVideoSizeChanged(width: Int, height: Int, pixelWidthHeightRatio: Float)

    fun setRepeatMode(@Player.RepeatMode repeatMode: Int)
}