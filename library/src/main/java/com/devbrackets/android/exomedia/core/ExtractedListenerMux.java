package com.devbrackets.android.exomedia.core;

import android.net.NetworkInfo;
import android.support.annotation.Nullable;
import android.view.Surface;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;

import java.io.IOException;

class ExtractedListenerMux {
    @Nullable
    private
    AnalyticsListener analyticsListener;

    ExtractedListenerMux() {
    }

    // Analytics
    @Override
    void onPlayerStateChanged(EventTime eventTime, boolean playWhenReady, int playbackState) {
        if (analyticsListener != null) {
            analyticsListener.onPlayerStateChanged(eventTime, playWhenReady, playbackState);
        }
    }

    @Override
    void onTimelineChanged(EventTime eventTime, int reason) {
        if (analyticsListener != null) {
            analyticsListener.onTimelineChanged(eventTime, reason);
        }
    }

    @Override
    void onPositionDiscontinuity(EventTime eventTime, int reason) {
        if (analyticsListener != null) {
            analyticsListener.onPositionDiscontinuity(eventTime, reason);
        }
    }

    @Override
    void onSeekStarted(EventTime eventTime) {
        if (analyticsListener != null) {
            analyticsListener.onSeekStarted(eventTime);
        }
    }

    @Override
    void onSeekProcessed(EventTime eventTime) {
        if (analyticsListener != null) {
            analyticsListener.onSeekProcessed(eventTime);
        }
    }

    @Override
    void onPlaybackParametersChanged(EventTime eventTime, PlaybackParameters playbackParameters) {
        if (analyticsListener != null) {
            analyticsListener.onPlaybackParametersChanged(eventTime, playbackParameters);
        }
    }

    @Override
    void onRepeatModeChanged(EventTime eventTime, int repeatMode) {
        if (analyticsListener != null) {
            analyticsListener.onRepeatModeChanged(eventTime, repeatMode);
        }
    }

    @Override
    void onShuffleModeChanged(EventTime eventTime, boolean shuffleModeEnabled) {
        if (analyticsListener != null) {
            analyticsListener.onShuffleModeChanged(eventTime, shuffleModeEnabled);
        }
    }

    @Override
    void onLoadingChanged(EventTime eventTime, boolean isLoading) {
        if (analyticsListener != null) {
            analyticsListener.onLoadingChanged(eventTime, isLoading);
        }
    }

    @Override
    void onPlayerError(EventTime eventTime, ExoPlaybackException error) {
        if (analyticsListener != null) {
            analyticsListener.onPlayerError(eventTime, error);
        }
    }

    @Override
    void onTracksChanged(EventTime eventTime, TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        if (analyticsListener != null) {
            analyticsListener.onTracksChanged(eventTime, trackGroups, trackSelections);
        }
    }

    @Override
    void onLoadStarted(EventTime eventTime, MediaSourceEventListener.LoadEventInfo loadEventInfo, MediaSourceEventListener.MediaLoadData mediaLoadData) {
        if (analyticsListener != null) {
            analyticsListener.onLoadStarted(eventTime, loadEventInfo, mediaLoadData);
        }
    }

    @Override
    void onLoadCompleted(EventTime eventTime, MediaSourceEventListener.LoadEventInfo loadEventInfo, MediaSourceEventListener.MediaLoadData mediaLoadData) {
        if (analyticsListener != null) {
            analyticsListener.onLoadCompleted(eventTime, loadEventInfo, mediaLoadData);
        }
    }

    @Override
    void onLoadCanceled(EventTime eventTime, MediaSourceEventListener.LoadEventInfo loadEventInfo, MediaSourceEventListener.MediaLoadData mediaLoadData) {
        if (analyticsListener != null) {
            analyticsListener.onLoadCanceled(eventTime, loadEventInfo, mediaLoadData);
        }
    }

    @Override
    void onLoadError(EventTime eventTime, MediaSourceEventListener.LoadEventInfo loadEventInfo, MediaSourceEventListener.MediaLoadData mediaLoadData, IOException error, boolean wasCanceled) {
        if (analyticsListener != null) {
            analyticsListener.onLoadError(eventTime, loadEventInfo, mediaLoadData, error, wasCanceled);
        }
    }

    @Override
    void onDownstreamFormatChanged(EventTime eventTime, MediaSourceEventListener.MediaLoadData mediaLoadData) {
        if (analyticsListener != null) {
            analyticsListener.onDownstreamFormatChanged(eventTime, mediaLoadData);
        }
    }

    @Override
    void onUpstreamDiscarded(EventTime eventTime, MediaSourceEventListener.MediaLoadData mediaLoadData) {
        if (analyticsListener != null) {
            analyticsListener.onUpstreamDiscarded(eventTime, mediaLoadData);
        }
    }

    @Override
    void onMediaPeriodCreated(EventTime eventTime) {
        if (analyticsListener != null) {
            analyticsListener.onMediaPeriodCreated(eventTime);
        }
    }

    @Override
    void onMediaPeriodReleased(EventTime eventTime) {
        if (analyticsListener != null) {
            analyticsListener.onMediaPeriodReleased(eventTime);
        }
    }

    @Override
    void onReadingStarted(EventTime eventTime) {
        if (analyticsListener != null) {
            analyticsListener.onReadingStarted(eventTime);
        }
    }

    @Override
    void onBandwidthEstimate(EventTime eventTime, int totalLoadTimeMs, long totalBytesLoaded, long bitrateEstimate) {
        if (analyticsListener != null) {
            analyticsListener.onBandwidthEstimate(eventTime, totalLoadTimeMs, totalBytesLoaded, bitrateEstimate);
        }
    }

    @Override
    void onViewportSizeChange(EventTime eventTime, int width, int height) {
        if (analyticsListener != null) {
            analyticsListener.onViewportSizeChange(eventTime, width, height);
        }
    }

    @Override
    void onNetworkTypeChanged(EventTime eventTime, @Nullable NetworkInfo networkInfo) {
        if (analyticsListener != null) {
            analyticsListener.onNetworkTypeChanged(eventTime, networkInfo);
        }
    }

    @Override
    void onMetadata(EventTime eventTime, Metadata metadata) {
        if (analyticsListener != null) {
            analyticsListener.onMetadata(eventTime, metadata);
        }
    }

    @Override
    void onDecoderEnabled(EventTime eventTime, int trackType, DecoderCounters decoderCounters) {
        if (analyticsListener != null) {
            analyticsListener.onDecoderEnabled(eventTime, trackType, decoderCounters);
        }
    }

    @Override
    void onDecoderInitialized(EventTime eventTime, int trackType, String decoderName, long initializationDurationMs) {
        if (analyticsListener != null) {
            analyticsListener.onDecoderInitialized(eventTime, trackType, decoderName, initializationDurationMs);
        }
    }

    @Override
    void onDecoderInputFormatChanged(EventTime eventTime, int trackType, Format format) {
        if (analyticsListener != null) {
            analyticsListener.onDecoderInputFormatChanged(eventTime, trackType, format);
        }
    }

    @Override
    void onDecoderDisabled(EventTime eventTime, int trackType, DecoderCounters decoderCounters) {
        if (analyticsListener != null) {
            analyticsListener.onDecoderDisabled(eventTime, trackType, decoderCounters);
        }
    }

    @Override
    void onAudioSessionId(EventTime eventTime, int audioSessionId) {
        if (analyticsListener != null) {
            analyticsListener.onAudioSessionId(eventTime, audioSessionId);
        }
    }

    @Override
    void onAudioUnderrun(EventTime eventTime, int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {
        if (analyticsListener != null) {
            analyticsListener.onAudioUnderrun(eventTime, bufferSize, bufferSizeMs, elapsedSinceLastFeedMs);
        }
    }

    @Override
    void onDroppedVideoFrames(EventTime eventTime, int droppedFrames, long elapsedMs) {
        if (analyticsListener != null) {
            analyticsListener.onDroppedVideoFrames(eventTime, droppedFrames, elapsedMs);
        }
    }

    @Override
    public void onVideoSizeChanged(EventTime eventTime, int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        if (analyticsListener != null) {
            analyticsListener.onVideoSizeChanged(eventTime, width, height, unappliedRotationDegrees, pixelWidthHeightRatio);
        }
    }

    @Override
    void onRenderedFirstFrame(EventTime eventTime, Surface surface) {
        if (analyticsListener != null) {
            analyticsListener.onRenderedFirstFrame(eventTime, surface);
        }
    }

    @Override
    void onDrmKeysLoaded(EventTime eventTime) {
        if (analyticsListener != null) {
            analyticsListener.onDrmKeysLoaded(eventTime);
        }
    }

    @Override
    void onDrmSessionManagerError(EventTime eventTime, Exception error) {
        if (analyticsListener != null) {
            analyticsListener.onDrmSessionManagerError(eventTime, error);
        }
    }

    @Override
    void onDrmKeysRestored(EventTime eventTime) {
        if (analyticsListener != null) {
            analyticsListener.onDrmKeysRestored(eventTime);
        }
    }

    @Override
    void onDrmKeysRemoved(EventTime eventTime) {
        if (analyticsListener != null) {
            analyticsListener.onDrmKeysRemoved(eventTime);
        }
    }

    /**
     * Sets the listener to inform of Analytics updates
     *
     * @param listener The listener to inform
     */
    void setAnalyticsListener(@Nullable AnalyticsListener listener) {
        analyticsListener = listener;
    }
}