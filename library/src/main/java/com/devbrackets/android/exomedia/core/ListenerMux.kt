/*
 * Copyright (C) 2015-2018 ExoMedia Contributors
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

package com.devbrackets.android.exomedia.core

import android.media.MediaPlayer
import android.os.Handler
import android.support.annotation.IntRange
import android.view.Surface
import com.devbrackets.android.exomedia.core.exception.NativeMediaPlaybackException
import com.devbrackets.android.exomedia.core.exoplayer.ExoMediaPlayer
import com.devbrackets.android.exomedia.core.listener.ExoPlayerListener
import com.devbrackets.android.exomedia.core.listener.MetadataListener
import com.devbrackets.android.exomedia.core.video.ClearableSurface
import com.devbrackets.android.exomedia.listener.*
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.decoder.DecoderCounters
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.source.MediaSourceEventListener
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import java.io.IOException
import java.lang.ref.WeakReference

/**
 * An internal Listener that implements the listeners for the [ExoMediaPlayer],
 * Android VideoView, and the Android MediaPlayer to output to the correct
 * error listeners.
 */
class ListenerMux(private val muxNotifier: Notifier) : ExoPlayerListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnSeekCompleteListener, OnBufferUpdateListener, MetadataListener, AnalyticsListener {

    private val delayedHandler = Handler()

    private var preparedListener: OnPreparedListener? = null
    private var completionListener: OnCompletionListener? = null
    private var bufferUpdateListener: OnBufferUpdateListener? = null
    private var seekCompletionListener: OnSeekCompletionListener? = null
    private var errorListener: OnErrorListener? = null
    private var metadataListener: MetadataListener? = null
    private var analyticsListener: AnalyticsListener? = null

    private var clearableSurfaceRef = WeakReference<ClearableSurface>(null)

    /**
     * Retrieves if the player was prepared
     *
     * @return True if the player was prepared
     */
    var isPrepared = false
        private set
    private var notifiedCompleted = false
    private var clearRequested = false

    override fun onBufferingUpdate(mp: MediaPlayer, percent: Int) {
        onBufferingUpdate(percent)
    }

    override fun onCompletion(mp: MediaPlayer) {
        completionListener?.onCompletion()
    }

    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        return notifyErrorListener(NativeMediaPlaybackException(what, extra))
    }

    override fun onSeekComplete(mp: MediaPlayer) {
        seekCompletionListener?.onSeekComplete()
    }

    override fun onPrepared(mp: MediaPlayer) {
        notifyPreparedListener()
    }

    override fun onError(exoMediaPlayer: ExoMediaPlayer, e: Exception?) {
        muxNotifier.onMediaPlaybackEnded()
        muxNotifier.onExoPlayerError(exoMediaPlayer, e)
        notifyErrorListener(e)
    }

    override fun onStateChanged(playWhenReady: Boolean, playbackState: Int) {
        when (playbackState) {
            Player.STATE_READY -> {
                if (!isPrepared) {
                    notifyPreparedListener()
                }
                if (playWhenReady) {
                    //Updates the previewImage
                    muxNotifier.onPreviewImageStateChanged(false)
                }
            }
            Player.STATE_IDLE -> {
                if (clearRequested) {
                    //Clears the textureView when requested
                    clearRequested = false
                    val clearableSurface = clearableSurfaceRef.get()

                    if (clearableSurface != null) {
                        clearableSurface.clearSurface()
                        clearableSurfaceRef = WeakReference<ClearableSurface>(null)
                    }
                }
            }
            Player.STATE_ENDED -> {
                muxNotifier.onMediaPlaybackEnded()

                if (!notifiedCompleted) {
                    notifyCompletionListener()
                }
            }
        }
    }

    override fun onSeekComplete() {
        muxNotifier.onSeekComplete()
        seekCompletionListener?.onSeekComplete()
    }

    override fun onBufferingUpdate(@IntRange(from = 0, to = 100) percent: Int) {
        muxNotifier.onBufferUpdated(percent)
        bufferUpdateListener?.onBufferingUpdate(percent)
    }

    override fun onMetadata(metadata: Metadata) {
        metadataListener?.onMetadata(metadata)
    }

    override fun onVideoSizeChanged(width: Int, height: Int, unAppliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
        muxNotifier.onVideoSizeChanged(width, height, unAppliedRotationDegrees, pixelWidthHeightRatio)
    }

    // Analytics
    override fun onPlayerStateChanged(eventTime: AnalyticsListener.EventTime?, playWhenReady: Boolean, playbackState: Int) {
        analyticsListener?.onPlayerStateChanged(eventTime, playWhenReady, playbackState)
    }

    override fun onTimelineChanged(eventTime: AnalyticsListener.EventTime?, reason: Int) {
        analyticsListener?.onTimelineChanged(eventTime, reason)
    }

    override fun onPositionDiscontinuity(eventTime: AnalyticsListener.EventTime?, reason: Int) {
        analyticsListener?.onPositionDiscontinuity(eventTime, reason)
    }

    override fun onSeekStarted(eventTime: AnalyticsListener.EventTime?) {
        analyticsListener?.onSeekStarted(eventTime)
    }

    override fun onSeekProcessed(eventTime: AnalyticsListener.EventTime?) {
        analyticsListener?.onSeekProcessed(eventTime)
    }

    override fun onPlaybackParametersChanged(eventTime: AnalyticsListener.EventTime?, playbackParameters: PlaybackParameters?) {
        analyticsListener?.onPlaybackParametersChanged(eventTime, playbackParameters)
    }

    override fun onRepeatModeChanged(eventTime: AnalyticsListener.EventTime?, repeatMode: Int) {
        analyticsListener?.onRepeatModeChanged(eventTime, repeatMode)
    }

    override fun onShuffleModeChanged(eventTime: AnalyticsListener.EventTime?, shuffleModeEnabled: Boolean) {
        analyticsListener?.onShuffleModeChanged(eventTime, shuffleModeEnabled)
    }

    override fun onLoadingChanged(eventTime: AnalyticsListener.EventTime?, isLoading: Boolean) {
        analyticsListener?.onLoadingChanged(eventTime, isLoading)
    }

    override fun onPlayerError(eventTime: AnalyticsListener.EventTime?, error: ExoPlaybackException?) {
        analyticsListener?.onPlayerError(eventTime, error)
    }

    override fun onTracksChanged(eventTime: AnalyticsListener.EventTime?, trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
        analyticsListener?.onTracksChanged(eventTime, trackGroups, trackSelections)
    }

    override fun onLoadStarted(eventTime: AnalyticsListener.EventTime?, loadEventInfo: MediaSourceEventListener.LoadEventInfo?, mediaLoadData: MediaSourceEventListener.MediaLoadData?) {
        analyticsListener?.onLoadStarted(eventTime, loadEventInfo, mediaLoadData)
    }

    override fun onLoadCompleted(eventTime: AnalyticsListener.EventTime?, loadEventInfo: MediaSourceEventListener.LoadEventInfo?, mediaLoadData: MediaSourceEventListener.MediaLoadData?) {
        analyticsListener?.onLoadCompleted(eventTime, loadEventInfo, mediaLoadData)
    }

    override fun onLoadCanceled(eventTime: AnalyticsListener.EventTime?, loadEventInfo: MediaSourceEventListener.LoadEventInfo?, mediaLoadData: MediaSourceEventListener.MediaLoadData?) {
        analyticsListener?.onLoadCanceled(eventTime, loadEventInfo, mediaLoadData)
    }

    override fun onLoadError(eventTime: AnalyticsListener.EventTime?, loadEventInfo: MediaSourceEventListener.LoadEventInfo?, mediaLoadData: MediaSourceEventListener.MediaLoadData?, error: IOException?, wasCanceled: Boolean) {
        analyticsListener?.onLoadError(eventTime, loadEventInfo, mediaLoadData, error, wasCanceled)
    }

    override fun onDownstreamFormatChanged(eventTime: AnalyticsListener.EventTime?, mediaLoadData: MediaSourceEventListener.MediaLoadData?) {
        analyticsListener?.onDownstreamFormatChanged(eventTime, mediaLoadData)
    }

    override fun onUpstreamDiscarded(eventTime: AnalyticsListener.EventTime?, mediaLoadData: MediaSourceEventListener.MediaLoadData?) {
        analyticsListener?.onUpstreamDiscarded(eventTime, mediaLoadData)
    }

    override fun onMediaPeriodCreated(eventTime: AnalyticsListener.EventTime?) {
        analyticsListener?.onMediaPeriodCreated(eventTime)
    }

    override fun onMediaPeriodReleased(eventTime: AnalyticsListener.EventTime?) {
        analyticsListener?.onMediaPeriodReleased(eventTime)
    }

    override fun onReadingStarted(eventTime: AnalyticsListener.EventTime?) {
        analyticsListener?.onReadingStarted(eventTime)
    }

    override fun onBandwidthEstimate(eventTime: AnalyticsListener.EventTime?, totalLoadTimeMs: Int, totalBytesLoaded: Long, bitrateEstimate: Long) {
        analyticsListener?.onBandwidthEstimate(eventTime, totalLoadTimeMs, totalBytesLoaded, bitrateEstimate)
    }

    override fun onSurfaceSizeChanged(eventTime: AnalyticsListener.EventTime?, width: Int, height: Int) {
        analyticsListener?.onSurfaceSizeChanged(eventTime, width, height)
    }

    override fun onVolumeChanged(eventTime: AnalyticsListener.EventTime?, volume: Float) {
        analyticsListener?.onVolumeChanged(eventTime, volume)
    }

    override fun onDrmSessionAcquired(eventTime: AnalyticsListener.EventTime?) {
        analyticsListener?.onDrmSessionAcquired(eventTime)
    }

    override fun onDrmSessionReleased(eventTime: AnalyticsListener.EventTime?) {
        analyticsListener?.onDrmSessionReleased(eventTime)
    }

    override fun onAudioAttributesChanged(eventTime: AnalyticsListener.EventTime?, audioAttributes: AudioAttributes?) {
        analyticsListener?.onAudioAttributesChanged(eventTime, audioAttributes)
    }

    override fun onMetadata(eventTime: AnalyticsListener.EventTime?, metadata: Metadata?) {
        analyticsListener?.onMetadata(eventTime, metadata)
    }

    override fun onDecoderEnabled(eventTime: AnalyticsListener.EventTime?, trackType: Int, decoderCounters: DecoderCounters?) {
        analyticsListener?.onDecoderEnabled(eventTime, trackType, decoderCounters)
    }

    override fun onDecoderInitialized(eventTime: AnalyticsListener.EventTime?, trackType: Int, decoderName: String?, initializationDurationMs: Long) {
        analyticsListener?.onDecoderInitialized(eventTime, trackType, decoderName, initializationDurationMs)
    }

    override fun onDecoderInputFormatChanged(eventTime: AnalyticsListener.EventTime?, trackType: Int, format: Format?) {
        analyticsListener?.onDecoderInputFormatChanged(eventTime, trackType, format)
    }

    override fun onDecoderDisabled(eventTime: AnalyticsListener.EventTime?, trackType: Int, decoderCounters: DecoderCounters?) {
        analyticsListener?.onDecoderDisabled(eventTime, trackType, decoderCounters)
    }

    override fun onAudioSessionId(eventTime: AnalyticsListener.EventTime?, audioSessionId: Int) {
        analyticsListener?.onAudioSessionId(eventTime, audioSessionId)
    }

    override fun onAudioUnderrun(eventTime: AnalyticsListener.EventTime?, bufferSize: Int, bufferSizeMs: Long, elapsedSinceLastFeedMs: Long) {
        analyticsListener?.onAudioUnderrun(eventTime, bufferSize, bufferSizeMs, elapsedSinceLastFeedMs)
    }

    override fun onDroppedVideoFrames(eventTime: AnalyticsListener.EventTime?, droppedFrames: Int, elapsedMs: Long) {
        analyticsListener?.onDroppedVideoFrames(eventTime, droppedFrames, elapsedMs)
    }

    override fun onVideoSizeChanged(eventTime: AnalyticsListener.EventTime?, width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
        analyticsListener?.onVideoSizeChanged(eventTime, width, height, unappliedRotationDegrees, pixelWidthHeightRatio)
    }

    override fun onRenderedFirstFrame(eventTime: AnalyticsListener.EventTime?, surface: Surface?) {
        analyticsListener?.onRenderedFirstFrame(eventTime, surface)
    }

    override fun onDrmKeysLoaded(eventTime: AnalyticsListener.EventTime?) {
        analyticsListener?.onDrmKeysLoaded(eventTime)
    }

    override fun onDrmSessionManagerError(eventTime: AnalyticsListener.EventTime?, error: Exception?) {
        analyticsListener?.onDrmSessionManagerError(eventTime, error)
    }

    override fun onDrmKeysRestored(eventTime: AnalyticsListener.EventTime?) {
        analyticsListener?.onDrmKeysRestored(eventTime)
    }

    override fun onDrmKeysRemoved(eventTime: AnalyticsListener.EventTime?) {
        analyticsListener?.onDrmKeysRemoved(eventTime)
    }

    /**
     * Specifies the surface to clear when the playback reaches an appropriate state.
     * Once the `clearableSurface` is cleared, the reference will be removed
     *
     * @param clearableSurface The [ClearableSurface] to clear when the playback reaches an appropriate state
     */
    fun clearSurfaceWhenReady(clearableSurface: ClearableSurface?) {
        clearRequested = true
        clearableSurfaceRef = WeakReference<ClearableSurface>(clearableSurface)
    }

    /**
     * Sets the repeatListener to inform of VideoPlayer prepared events
     *
     * @param listener The repeatListener to inform
     */
    fun setOnPreparedListener(listener: OnPreparedListener?) {
        preparedListener = listener
    }

    /**
     * Sets the repeatListener to inform of VideoPlayer completion events
     *
     * @param listener The repeatListener to inform
     */
    fun setOnCompletionListener(listener: OnCompletionListener?) {
        completionListener = listener
    }

    /**
     * Sets the repeatListener to inform of buffering updates
     *
     * @param listener The repeatListener to inform
     */
    fun setOnBufferUpdateListener(listener: OnBufferUpdateListener?) {
        bufferUpdateListener = listener
    }

    /**
     * Sets the repeatListener to inform of VideoPlayer seek completion events
     *
     * @param listener The repeatListener to inform
     */
    fun setOnSeekCompletionListener(listener: OnSeekCompletionListener?) {
        seekCompletionListener = listener
    }

    /**
     * Sets the repeatListener to inform of playback errors
     *
     * @param listener The repeatListener to inform
     */
    fun setOnErrorListener(listener: OnErrorListener?) {
        errorListener = listener
    }

    /**
     * Sets the repeatListener to inform of ID3 metadata updates
     *
     * @param listener The repeatListener to inform
     */
    fun setMetadataListener(listener: MetadataListener?) {
        metadataListener = listener
    }

    /**
     * Sets the repeatListener to inform of Analytics updates
     *
     * @param listener The repeatListener to inform
     */
    fun setAnalyticsListener(listener: AnalyticsListener?) {
        analyticsListener = listener
    }

    /**
     * Sets weather the repeatListener was notified when we became prepared.
     *
     * @param wasNotified True if the onPreparedListener was already notified
     */
    fun setNotifiedPrepared(wasNotified: Boolean) {
        isPrepared = wasNotified
        muxNotifier.onPreviewImageStateChanged(true)
    }

    /**
     * Sets weather the repeatListener was notified when the playback was completed
     * (played through the end).
     *
     * @param wasNotified True if the onCompletionListener was already notified
     */
    fun setNotifiedCompleted(wasNotified: Boolean) {
        notifiedCompleted = wasNotified
    }

    private fun notifyErrorListener(e: Exception?): Boolean {
        return errorListener?.onError(e) == true
    }

    private fun notifyPreparedListener() {
        isPrepared = true

        delayedHandler.post {
            performPreparedHandlerNotification()
        }
    }

    private fun performPreparedHandlerNotification() {
        muxNotifier.onPrepared()
        preparedListener?.onPrepared()
    }

    private fun notifyCompletionListener() {
        if (!muxNotifier.shouldNotifyCompletion(COMPLETED_DURATION_LEEWAY)) {
            return
        }

        notifiedCompleted = true

        delayedHandler.post {
            completionListener?.onCompletion()
        }
    }

    abstract class Notifier {
        open fun onSeekComplete() {
            //Purposefully left blank
        }

        fun onBufferUpdated(percent: Int) {
            //Purposefully left blank
        }

        open fun onVideoSizeChanged(width: Int, height: Int, unAppliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
            //Purposefully left blank
        }

        open fun onPrepared() {
            //Purposefully left blank
        }

        open fun onPreviewImageStateChanged(toVisible: Boolean) {
            //Purposefully left blank
        }

        abstract fun shouldNotifyCompletion(endLeeway: Long): Boolean

        abstract fun onExoPlayerError(exoMediaPlayer: ExoMediaPlayer, e: Exception?)

        abstract fun onMediaPlaybackEnded()
    }

    companion object {
        //The amount of time the current position can be off the duration to call the onCompletion repeatListener
        private val COMPLETED_DURATION_LEEWAY: Long = 1000
    }
}