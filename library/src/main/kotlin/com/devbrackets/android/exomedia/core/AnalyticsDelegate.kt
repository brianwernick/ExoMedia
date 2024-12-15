package com.devbrackets.android.exomedia.core

import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.Metadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DecoderCounters
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import java.io.IOException

/**
 * A simple analytics delegate that handles a nullable listener delegate which allows us to use
 * class delegation in the [ListenerMux]
 */
@OptIn(UnstableApi::class)
class AnalyticsDelegate(
  var listener: AnalyticsListener? = null
): AnalyticsListener {
  override fun onMetadata(eventTime: AnalyticsListener.EventTime, metadata: Metadata) {
    listener?.onMetadata(eventTime, metadata)
  }

  @Deprecated(
    "Replace with onVideoSizeChanged(EventTime, VideoSize)",
    replaceWith = ReplaceWith(
      expression = "onVideoSizeChanged(eventTime, VideoSize(width, height, unappliedRotationDegrees, pixelWidthHeightRatio))",
      imports = [
        "androidx.media3.common.VideoSize"
      ]
    )
  )
  override fun onVideoSizeChanged(
    eventTime: AnalyticsListener.EventTime,
    width: Int,
    height: Int,
    unappliedRotationDegrees: Int,
    pixelWidthHeightRatio: Float
  ) {
    @Suppress("DEPRECATION")
    listener?.onVideoSizeChanged(eventTime, width, height, unappliedRotationDegrees, pixelWidthHeightRatio)
  }

  override fun onVideoSizeChanged(eventTime: AnalyticsListener.EventTime, videoSize: VideoSize) {
    listener?.onVideoSizeChanged(eventTime, videoSize)
  }

  @Suppress("DeprecatedCallableAddReplaceWith")
  @Deprecated("Replace with onPlaybackStateChanged(EventTime, Int) or onPlayWhenReadyChanged(EventTime, Boolean, Int)")
  override fun onPlayerStateChanged(eventTime: AnalyticsListener.EventTime, playWhenReady: Boolean, playbackState: Int) {
    @Suppress("DEPRECATION")
    listener?.onPlayerStateChanged(eventTime, playWhenReady, playbackState)
  }

  override fun onTimelineChanged(eventTime: AnalyticsListener.EventTime, reason: Int) {
    listener?.onTimelineChanged(eventTime, reason)
  }

  @Suppress("DeprecatedCallableAddReplaceWith")
  @Deprecated("Replace with onPositionDiscontinuity(EventTime, PositionInfo, PositionInfo, Int)")
  override fun onPositionDiscontinuity(eventTime: AnalyticsListener.EventTime, reason: Int) {
    @Suppress("DEPRECATION")
    listener?.onPositionDiscontinuity(eventTime, reason)
  }

  @Suppress("DeprecatedCallableAddReplaceWith")
  @Deprecated("Replace with onPositionDiscontinuity(EventTime, PositionInfo, PositionInfo, Int)")
  override fun onSeekStarted(eventTime: AnalyticsListener.EventTime) {
    @Suppress("DEPRECATION")
    listener?.onSeekStarted(eventTime)
  }

  override fun onPlaybackParametersChanged(eventTime: AnalyticsListener.EventTime, playbackParameters: PlaybackParameters) {
    listener?.onPlaybackParametersChanged(eventTime, playbackParameters)
  }

  override fun onRepeatModeChanged(eventTime: AnalyticsListener.EventTime, repeatMode: Int) {
    listener?.onRepeatModeChanged(eventTime, repeatMode)
  }

  override fun onShuffleModeChanged(eventTime: AnalyticsListener.EventTime, shuffleModeEnabled: Boolean) {
    listener?.onShuffleModeChanged(eventTime, shuffleModeEnabled)
  }

  @Deprecated(
    "Replace with onIsLoadingChanged(EventTime, Boolean)",
    ReplaceWith(
      expression = "onIsLoadingChanged(eventTime, isLoading)"
    )
  )
  override fun onLoadingChanged(eventTime: AnalyticsListener.EventTime, isLoading: Boolean) {
    @Suppress("DEPRECATION")
    listener?.onLoadingChanged(eventTime, isLoading)
  }

  override fun onPlayerError(eventTime: AnalyticsListener.EventTime, error: PlaybackException) {
    listener?.onPlayerError(eventTime, error)
  }

  override fun onTracksChanged(eventTime: AnalyticsListener.EventTime, tracks: Tracks) {
    listener?.onTracksChanged(eventTime, tracks)
  }

  override fun onLoadStarted(eventTime: AnalyticsListener.EventTime, loadEventInfo: LoadEventInfo, mediaLoadData: MediaLoadData) {
    listener?.onLoadStarted(eventTime, loadEventInfo, mediaLoadData)
  }

  override fun onLoadCompleted(eventTime: AnalyticsListener.EventTime, loadEventInfo: LoadEventInfo, mediaLoadData: MediaLoadData) {
    listener?.onLoadCompleted(eventTime, loadEventInfo, mediaLoadData)
  }

  override fun onLoadCanceled(eventTime: AnalyticsListener.EventTime, loadEventInfo: LoadEventInfo, mediaLoadData: MediaLoadData) {
    listener?.onLoadCanceled(eventTime, loadEventInfo, mediaLoadData)
  }

  override fun onLoadError(
    eventTime: AnalyticsListener.EventTime,
    loadEventInfo: LoadEventInfo,
    mediaLoadData: MediaLoadData,
    error: IOException,
    wasCanceled: Boolean
  ) {
    listener?.onLoadError(eventTime, loadEventInfo, mediaLoadData, error, wasCanceled)
  }

  override fun onDownstreamFormatChanged(eventTime: AnalyticsListener.EventTime, mediaLoadData: MediaLoadData) {
    listener?.onDownstreamFormatChanged(eventTime, mediaLoadData)
  }

  override fun onUpstreamDiscarded(eventTime: AnalyticsListener.EventTime, mediaLoadData: MediaLoadData) {
    listener?.onUpstreamDiscarded(eventTime, mediaLoadData)
  }

  override fun onBandwidthEstimate(eventTime: AnalyticsListener.EventTime, totalLoadTimeMs: Int, totalBytesLoaded: Long, bitrateEstimate: Long) {
    listener?.onBandwidthEstimate(eventTime, totalLoadTimeMs, totalBytesLoaded, bitrateEstimate)
  }

  override fun onSurfaceSizeChanged(eventTime: AnalyticsListener.EventTime, width: Int, height: Int) {
    listener?.onSurfaceSizeChanged(eventTime, width, height)
  }

  override fun onVolumeChanged(eventTime: AnalyticsListener.EventTime, volume: Float) {
    listener?.onVolumeChanged(eventTime, volume)
  }

  @Suppress("DeprecatedCallableAddReplaceWith")
  @Deprecated("Replace with onDrmSessionAcquired(EventTime, Int)")
  override fun onDrmSessionAcquired(eventTime: AnalyticsListener.EventTime) {
    @Suppress("DEPRECATION")
    listener?.onDrmSessionAcquired(eventTime)
  }

  override fun onDrmSessionReleased(eventTime: AnalyticsListener.EventTime) {
    listener?.onDrmSessionReleased(eventTime)
  }

  override fun onAudioAttributesChanged(eventTime: AnalyticsListener.EventTime, audioAttributes: AudioAttributes) {
    listener?.onAudioAttributesChanged(eventTime, audioAttributes)
  }

  override fun onAudioSessionIdChanged(eventTime: AnalyticsListener.EventTime, audioSessionId: Int) {
    listener?.onAudioSessionIdChanged(eventTime, audioSessionId)
  }

  override fun onAudioUnderrun(eventTime: AnalyticsListener.EventTime, bufferSize: Int, bufferSizeMs: Long, elapsedSinceLastFeedMs: Long) {
    listener?.onAudioUnderrun(eventTime, bufferSize, bufferSizeMs, elapsedSinceLastFeedMs)
  }

  override fun onDroppedVideoFrames(eventTime: AnalyticsListener.EventTime, droppedFrames: Int, elapsedMs: Long) {
    listener?.onDroppedVideoFrames(eventTime, droppedFrames, elapsedMs)
  }

  override fun onRenderedFirstFrame(eventTime: AnalyticsListener.EventTime, output: Any, renderTimeMs: Long) {
    listener?.onRenderedFirstFrame(eventTime, output, renderTimeMs)
  }

  override fun onDrmKeysLoaded(eventTime: AnalyticsListener.EventTime) {
    listener?.onDrmKeysLoaded(eventTime)
  }

  override fun onDrmSessionManagerError(eventTime: AnalyticsListener.EventTime, error: java.lang.Exception) {
    listener?.onDrmSessionManagerError(eventTime, error)
  }

  override fun onDrmKeysRestored(eventTime: AnalyticsListener.EventTime) {
    listener?.onDrmKeysRestored(eventTime)
  }

  override fun onDrmKeysRemoved(eventTime: AnalyticsListener.EventTime) {
    listener?.onDrmKeysRemoved(eventTime)
  }

  override fun onPlaybackStateChanged(eventTime: AnalyticsListener.EventTime, state: Int) {
    listener?.onPlaybackStateChanged(eventTime, state)
  }

  override fun onPlayWhenReadyChanged(eventTime: AnalyticsListener.EventTime, playWhenReady: Boolean, reason: Int) {
    listener?.onPlayWhenReadyChanged(eventTime, playWhenReady, reason)
  }

  override fun onPlaybackSuppressionReasonChanged(eventTime: AnalyticsListener.EventTime, playbackSuppressionReason: Int) {
    listener?.onPlaybackSuppressionReasonChanged(eventTime, playbackSuppressionReason)
  }

  override fun onIsPlayingChanged(eventTime: AnalyticsListener.EventTime, isPlaying: Boolean) {
    listener?.onIsPlayingChanged(eventTime, isPlaying)
  }

  override fun onMediaItemTransition(eventTime: AnalyticsListener.EventTime, mediaItem: MediaItem?, reason: Int) {
    listener?.onMediaItemTransition(eventTime, mediaItem, reason)
  }

  override fun onIsLoadingChanged(eventTime: AnalyticsListener.EventTime, isLoading: Boolean) {
    listener?.onIsLoadingChanged(eventTime, isLoading)
  }

  override fun onAudioEnabled(eventTime: AnalyticsListener.EventTime, counters: DecoderCounters) {
    listener?.onAudioEnabled(eventTime, counters)
  }

  @Suppress("DeprecatedCallableAddReplaceWith")
  @Deprecated("Replace with onAudioDecoderInitialized(EventTime, String, Long, Long)")
  override fun onAudioDecoderInitialized(eventTime: AnalyticsListener.EventTime, decoderName: String, initializationDurationMs: Long) {
    @Suppress("DEPRECATION")
    listener?.onAudioDecoderInitialized(eventTime, decoderName, initializationDurationMs)
  }

  override fun onAudioInputFormatChanged(eventTime: AnalyticsListener.EventTime, format: Format, decoderReuseEvaluation: DecoderReuseEvaluation?) {
    listener?.onAudioInputFormatChanged(eventTime, format, decoderReuseEvaluation)
  }

  override fun onAudioPositionAdvancing(eventTime: AnalyticsListener.EventTime, playoutStartSystemTimeMs: Long) {
    listener?.onAudioPositionAdvancing(eventTime, playoutStartSystemTimeMs)
  }

  override fun onAudioDisabled(eventTime: AnalyticsListener.EventTime, counters: DecoderCounters) {
    listener?.onAudioDisabled(eventTime, counters)
  }

  override fun onSkipSilenceEnabledChanged(eventTime: AnalyticsListener.EventTime, skipSilenceEnabled: Boolean) {
    listener?.onSkipSilenceEnabledChanged(eventTime, skipSilenceEnabled)
  }

  override fun onVideoEnabled(eventTime: AnalyticsListener.EventTime, counters: DecoderCounters) {
    listener?.onVideoEnabled(eventTime, counters)
  }

  @Suppress("DeprecatedCallableAddReplaceWith")
  @Deprecated("Replace with onVideoDecoderInitialized(EventTime, String, Long, Long)")
  override fun onVideoDecoderInitialized(eventTime: AnalyticsListener.EventTime, decoderName: String, initializationDurationMs: Long) {
    @Suppress("DEPRECATION")
    listener?.onVideoDecoderInitialized(eventTime, decoderName, initializationDurationMs)
  }

  override fun onVideoInputFormatChanged(eventTime: AnalyticsListener.EventTime, format: Format, decoderReuseEvaluation: DecoderReuseEvaluation?) {
    listener?.onVideoInputFormatChanged(eventTime, format, decoderReuseEvaluation)
  }

  override fun onVideoDisabled(eventTime: AnalyticsListener.EventTime, counters: DecoderCounters) {
    listener?.onVideoDisabled(eventTime, counters)
  }

  override fun onVideoFrameProcessingOffset(eventTime: AnalyticsListener.EventTime, totalProcessingOffsetUs: Long, frameCount: Int) {
    listener?.onVideoFrameProcessingOffset(eventTime, totalProcessingOffsetUs, frameCount)
  }
}