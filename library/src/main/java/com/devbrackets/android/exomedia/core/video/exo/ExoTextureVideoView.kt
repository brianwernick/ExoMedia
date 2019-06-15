/*
 * Copyright (C) 2016 - 2019 ExoMedia Contributors
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

package com.devbrackets.android.exomedia.core.video.exo

import android.content.Context
import android.graphics.SurfaceTexture
import android.net.Uri
import android.support.annotation.FloatRange
import android.support.annotation.IntRange
import android.util.AttributeSet
import android.view.Surface
import android.view.TextureView
import com.devbrackets.android.exomedia.ExoMedia
import com.devbrackets.android.exomedia.core.ListenerMux
import com.devbrackets.android.exomedia.core.api.VideoViewApi
import com.devbrackets.android.exomedia.core.exoplayer.WindowInfo
import com.devbrackets.android.exomedia.core.listener.CaptionListener
import com.devbrackets.android.exomedia.core.video.ResizingTextureView
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.drm.MediaDrmCallback
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray

/**
 * A [VideoViewApi] implementation that uses the ExoPlayer
 * as the backing media player.
 */
class ExoTextureVideoView : ResizingTextureView, VideoViewApi {
    protected val delegate = ExoVideoDelegate(context, this)

    override var volume: Float = delegate.volume

    override val isPlaying: Boolean
        get() = delegate.isPlaying

    override val duration: Long
        get() = delegate.duration

    override val currentPosition: Long
        get() = delegate.currentPosition

    override val bufferedPercent: Int
        get() = delegate.bufferedPercent

    override val windowInfo: WindowInfo?
        get() = delegate.windowInfo

    override val playbackSpeed: Float
        get() = delegate.playbackSpeed

    override val availableTracks: Map<ExoMedia.RendererType, TrackGroupArray>?
        get() = delegate.availableTracks

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    init {
        surfaceTextureListener = ExoMediaVideoSurfaceTextureListener()
        updateVideoSize(0, 0)
    }

    override fun setVideoUri(uri: Uri?) {
        delegate.setVideoUri(uri)
    }

    override fun setVideoUri(uri: Uri?, mediaSource: MediaSource?) {
        delegate.setVideoUri(uri, mediaSource)
    }

    override fun setDrmCallback(drmCallback: MediaDrmCallback?) {
        delegate.setDrmCallback(drmCallback)
    }

    override fun restart(): Boolean {
        return delegate.restart()
    }

    override fun setVolume(@FloatRange(from = 0.0, to = 1.0) volume: Float): Boolean {
        return delegate.setVolume(volume)
    }

    override fun seekTo(@IntRange(from = 0) milliseconds: Long) {
        delegate.seekTo(milliseconds)
    }

    override fun start() {
        delegate.start()
    }

    override fun pause() {
        delegate.pause()
    }

    override fun stopPlayback(clearSurface: Boolean) {
        delegate.stopPlayback(clearSurface)
    }

    override fun suspend() {
        delegate.suspend()
    }

    override fun setPlaybackSpeed(speed: Float): Boolean {
        return delegate.setPlaybackSpeed(speed)
    }

    override fun setCaptionListener(listener: CaptionListener?) {
        delegate.setCaptionListener(listener)
    }

    override fun trackSelectionAvailable(): Boolean {
        return delegate.trackSelectionAvailable()
    }

    override fun setTrack(type: ExoMedia.RendererType, groupIndex: Int, trackIndex: Int) {
        delegate.setTrack(type, groupIndex, trackIndex)
    }

    override fun getSelectedTrackIndex(type: ExoMedia.RendererType, groupIndex: Int): Int {
        return delegate.getSelectedTrackIndex(type, groupIndex)
    }

    override fun clearSelectedTracks(type: ExoMedia.RendererType) {
        delegate.clearSelectedTracks(type)

    }

    override fun setRendererEnabled(type: ExoMedia.RendererType, enabled: Boolean) {
        delegate.setRendererEnabled(type, enabled)
    }

    override fun isRendererEnabled(type: ExoMedia.RendererType): Boolean {
        return delegate.isRendererEnabled(type)
    }

    override fun release() {
        delegate.release()
    }

    override fun setListenerMux(listenerMux: ListenerMux) {
        delegate.listenerMux = listenerMux
    }

    override fun onVideoSizeChanged(width: Int, height: Int, pixelWidthHeightRatio: Float) {
        if (updateVideoSize((width * pixelWidthHeightRatio).toInt(), height)) {
            requestLayout()
        }
    }

    override fun setRepeatMode(@Player.RepeatMode repeatMode: Int) {
        delegate.setRepeatMode(repeatMode)
    }

    protected inner class ExoMediaVideoSurfaceTextureListener : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
            delegate.onSurfaceReady(Surface(surfaceTexture))
        }

        override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
            delegate.onSurfaceDestroyed()
            surfaceTexture.release()

            return true
        }

        override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
            // Purposefully left blank
        }

        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
            // Purposefully left blank
        }
    }
}
