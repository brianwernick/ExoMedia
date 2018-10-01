/*
 * Copyright (C) 2015-2017 ExoMedia Contributors
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

package com.devbrackets.android.exomedia.core;

import android.media.MediaPlayer;
import android.os.Handler;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Surface;

import com.devbrackets.android.exomedia.core.exception.NativeMediaPlaybackException;
import com.devbrackets.android.exomedia.core.exoplayer.ExoMediaPlayer;
import com.devbrackets.android.exomedia.core.listener.ExoPlayerListener;
import com.devbrackets.android.exomedia.core.listener.MetadataListener;
import com.devbrackets.android.exomedia.core.video.ClearableSurface;
import com.devbrackets.android.exomedia.listener.OnBufferUpdateListener;
import com.devbrackets.android.exomedia.listener.OnCompletionListener;
import com.devbrackets.android.exomedia.listener.OnErrorListener;
import com.devbrackets.android.exomedia.listener.OnPreparedListener;
import com.devbrackets.android.exomedia.listener.OnSeekCompletionListener;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;

import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * An internal Listener that implements the listeners for the {@link ExoMediaPlayer},
 * Android VideoView, and the Android MediaPlayer to output to the correct
 * error listeners.
 */
public class ListenerMux implements ExoPlayerListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnSeekCompleteListener, OnBufferUpdateListener, MetadataListener, AnalyticsListener {
    //The amount of time the current position can be off the duration to call the onCompletion listener
    private static final long COMPLETED_DURATION_LEEWAY = 1000;

    @NonNull
    private Handler delayedHandler = new Handler();
    @NonNull
    private Notifier muxNotifier;

    @Nullable
    private OnPreparedListener preparedListener;
    @Nullable
    private OnCompletionListener completionListener;
    @Nullable
    private OnBufferUpdateListener bufferUpdateListener;
    @Nullable
    private OnSeekCompletionListener seekCompletionListener;
    @Nullable
    private OnErrorListener errorListener;
    @Nullable
    private MetadataListener metadataListener;
    @Nullable
    private AnalyticsListener analyticsListener;

    @NonNull
    private WeakReference<ClearableSurface> clearableSurfaceRef = new WeakReference<>(null);

    private boolean notifiedPrepared = false;
    private boolean notifiedCompleted = false;
    private boolean clearRequested = false;

    public ListenerMux(@NonNull Notifier notifier) {
        muxNotifier = notifier;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        onBufferingUpdate(percent);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (completionListener != null) {
            completionListener.onCompletion();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return notifyErrorListener(new NativeMediaPlaybackException(what, extra));
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        if (seekCompletionListener != null) {
            seekCompletionListener.onSeekComplete();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        notifyPreparedListener();
    }

    @Override
    public void onError(ExoMediaPlayer exoMediaPlayer, Exception e) {
        muxNotifier.onMediaPlaybackEnded();
        muxNotifier.onExoPlayerError(exoMediaPlayer, e);
        notifyErrorListener(e);
    }

    @Override
    public void onStateChanged(boolean playWhenReady, int playbackState) {
        if (playbackState == Player.STATE_ENDED) {
            muxNotifier.onMediaPlaybackEnded();

            if (!notifiedCompleted) {
                notifyCompletionListener();
            }
        } else if (playbackState == Player.STATE_READY && !notifiedPrepared) {
            notifyPreparedListener();
        }

        //Updates the previewImage
        if (playbackState == Player.STATE_READY && playWhenReady) {
            muxNotifier.onPreviewImageStateChanged(false);
        }

        //Clears the textureView when requested
        if (playbackState == Player.STATE_IDLE && clearRequested) {
            clearRequested = false;
            ClearableSurface clearableSurface = clearableSurfaceRef.get();

            if (clearableSurface != null) {
                clearableSurface.clearSurface();
                clearableSurfaceRef = new WeakReference<>(null);
            }
        }
    }

    @Override
    public void onSeekComplete() {
        muxNotifier.onSeekComplete();
        if (seekCompletionListener != null) {
            seekCompletionListener.onSeekComplete();
        }
    }

    @Override
    public void onBufferingUpdate(@IntRange(from = 0, to = 100) int percent) {
        muxNotifier.onBufferUpdated(percent);

        if (bufferUpdateListener != null) {
            bufferUpdateListener.onBufferingUpdate(percent);
        }
    }

    @Override
    public void onMetadata(Metadata metadata) {
        if (metadataListener != null) {
            metadataListener.onMetadata(metadata);
        }
    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unAppliedRotationDegrees, float pixelWidthHeightRatio) {
        muxNotifier.onVideoSizeChanged(width, height, unAppliedRotationDegrees, pixelWidthHeightRatio);
    }

    // Analytics
    @Override
    public void onPlayerStateChanged(EventTime eventTime, boolean playWhenReady, int playbackState) {
        if (analyticsListener != null) {
            analyticsListener.onPlayerStateChanged(eventTime, playWhenReady, playbackState);
        }
    }

    @Override
    public void onTimelineChanged(EventTime eventTime, int reason) {
        if (analyticsListener != null) {
            analyticsListener.onTimelineChanged(eventTime, reason);
        }
    }

    @Override
    public void onPositionDiscontinuity(EventTime eventTime, int reason) {
        if (analyticsListener != null) {
            analyticsListener.onPositionDiscontinuity(eventTime, reason);
        }
    }

    @Override
    public void onSeekStarted(EventTime eventTime) {
        if (analyticsListener != null) {
            analyticsListener.onSeekStarted(eventTime);
        }
    }

    @Override
    public void onSeekProcessed(EventTime eventTime) {
        if (analyticsListener != null) {
            analyticsListener.onSeekProcessed(eventTime);
        }
    }

    @Override
    public void onPlaybackParametersChanged(EventTime eventTime, PlaybackParameters playbackParameters) {
        if (analyticsListener != null) {
            analyticsListener.onPlaybackParametersChanged(eventTime, playbackParameters);
        }
    }

    @Override
    public void onRepeatModeChanged(EventTime eventTime, int repeatMode) {
        if (analyticsListener != null) {
            analyticsListener.onRepeatModeChanged(eventTime, repeatMode);
        }
    }

    @Override
    public void onShuffleModeChanged(EventTime eventTime, boolean shuffleModeEnabled) {
        if (analyticsListener != null) {
            analyticsListener.onShuffleModeChanged(eventTime, shuffleModeEnabled);
        }
    }

    @Override
    public void onLoadingChanged(EventTime eventTime, boolean isLoading) {
        if (analyticsListener != null) {
            analyticsListener.onLoadingChanged(eventTime, isLoading);
        }
    }

    @Override
    public void onPlayerError(EventTime eventTime, ExoPlaybackException error) {
        if (analyticsListener != null) {
            analyticsListener.onPlayerError(eventTime, error);
        }
    }

    @Override
    public void onTracksChanged(EventTime eventTime, TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        if (analyticsListener != null) {
            analyticsListener.onTracksChanged(eventTime, trackGroups, trackSelections);
        }
    }

    @Override
    public void onLoadStarted(EventTime eventTime, MediaSourceEventListener.LoadEventInfo loadEventInfo, MediaSourceEventListener.MediaLoadData mediaLoadData) {
        if (analyticsListener != null) {
            analyticsListener.onLoadStarted(eventTime, loadEventInfo, mediaLoadData);
        }
    }

    @Override
    public void onLoadCompleted(EventTime eventTime, MediaSourceEventListener.LoadEventInfo loadEventInfo, MediaSourceEventListener.MediaLoadData mediaLoadData) {
        if (analyticsListener != null) {
            analyticsListener.onLoadCompleted(eventTime, loadEventInfo, mediaLoadData);
        }
    }

    @Override
    public void onLoadCanceled(EventTime eventTime, MediaSourceEventListener.LoadEventInfo loadEventInfo, MediaSourceEventListener.MediaLoadData mediaLoadData) {
        if (analyticsListener != null) {
            analyticsListener.onLoadCanceled(eventTime, loadEventInfo, mediaLoadData);
        }
    }

    @Override
    public void onLoadError(EventTime eventTime, MediaSourceEventListener.LoadEventInfo loadEventInfo, MediaSourceEventListener.MediaLoadData mediaLoadData, IOException error, boolean wasCanceled) {
        if (analyticsListener != null) {
            analyticsListener.onLoadError(eventTime, loadEventInfo, mediaLoadData, error, wasCanceled);
        }
    }

    @Override
    public void onDownstreamFormatChanged(EventTime eventTime, MediaSourceEventListener.MediaLoadData mediaLoadData) {
        if (analyticsListener != null) {
            analyticsListener.onDownstreamFormatChanged(eventTime, mediaLoadData);
        }
    }

    @Override
    public void onUpstreamDiscarded(EventTime eventTime, MediaSourceEventListener.MediaLoadData mediaLoadData) {
        if (analyticsListener != null) {
            analyticsListener.onUpstreamDiscarded(eventTime, mediaLoadData);
        }
    }

    @Override
    public void onMediaPeriodCreated(EventTime eventTime) {
        if (analyticsListener != null) {
            analyticsListener.onMediaPeriodCreated(eventTime);
        }
    }

    @Override
    public void onMediaPeriodReleased(EventTime eventTime) {
        if (analyticsListener != null) {
            analyticsListener.onMediaPeriodReleased(eventTime);
        }
    }

    @Override
    public void onReadingStarted(EventTime eventTime) {
        if (analyticsListener != null) {
            analyticsListener.onReadingStarted(eventTime);
        }
    }

    @Override
    public void onBandwidthEstimate(EventTime eventTime, int totalLoadTimeMs, long totalBytesLoaded, long bitrateEstimate) {
        if (analyticsListener != null) {
            analyticsListener.onBandwidthEstimate(eventTime, totalLoadTimeMs, totalBytesLoaded, bitrateEstimate);
        }
    }

    @Override
    public void onSurfaceSizeChanged(EventTime eventTime, int width, int height) {
        if (analyticsListener != null) {
            analyticsListener.onSurfaceSizeChanged(eventTime, width, height);
        }
    }

    @Override
    public void onVolumeChanged(EventTime eventTime, float volume) {
        if (analyticsListener != null) {
            analyticsListener.onVolumeChanged(eventTime, volume);
        }
    }

    @Override
    public void onDrmSessionAcquired(EventTime eventTime) {
        if (analyticsListener != null) {
            analyticsListener.onDrmSessionAcquired(eventTime);
        }
    }

    @Override
    public void onDrmSessionReleased(EventTime eventTime) {
        if (analyticsListener != null) {
            analyticsListener.onDrmSessionReleased(eventTime);
        }
    }

    @Override
    public void onAudioAttributesChanged(EventTime eventTime, AudioAttributes audioAttributes) {
        if (analyticsListener != null) {
            analyticsListener.onAudioAttributesChanged(eventTime, audioAttributes);
        }
    }

    @Override
    public void onMetadata(EventTime eventTime, Metadata metadata) {
        if (analyticsListener != null) {
            analyticsListener.onMetadata(eventTime, metadata);
        }
    }

    @Override
    public void onDecoderEnabled(EventTime eventTime, int trackType, DecoderCounters decoderCounters) {
        if (analyticsListener != null) {
            analyticsListener.onDecoderEnabled(eventTime, trackType, decoderCounters);
        }
    }

    @Override
    public void onDecoderInitialized(EventTime eventTime, int trackType, String decoderName, long initializationDurationMs) {
        if (analyticsListener != null) {
            analyticsListener.onDecoderInitialized(eventTime, trackType, decoderName, initializationDurationMs);
        }
    }

    @Override
    public void onDecoderInputFormatChanged(EventTime eventTime, int trackType, Format format) {
        if (analyticsListener != null) {
            analyticsListener.onDecoderInputFormatChanged(eventTime, trackType, format);
        }
    }

    @Override
    public void onDecoderDisabled(EventTime eventTime, int trackType, DecoderCounters decoderCounters) {
        if (analyticsListener != null) {
            analyticsListener.onDecoderDisabled(eventTime, trackType, decoderCounters);
        }
    }

    @Override
    public void onAudioSessionId(EventTime eventTime, int audioSessionId) {
        if (analyticsListener != null) {
            analyticsListener.onAudioSessionId(eventTime, audioSessionId);
        }
    }

    @Override
    public void onAudioUnderrun(EventTime eventTime, int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {
        if (analyticsListener != null) {
            analyticsListener.onAudioUnderrun(eventTime, bufferSize, bufferSizeMs, elapsedSinceLastFeedMs);
        }
    }

    @Override
    public void onDroppedVideoFrames(EventTime eventTime, int droppedFrames, long elapsedMs) {
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
    public void onRenderedFirstFrame(EventTime eventTime, Surface surface) {
        if (analyticsListener != null) {
            analyticsListener.onRenderedFirstFrame(eventTime, surface);
        }
    }

    @Override
    public void onDrmKeysLoaded(EventTime eventTime) {
        if (analyticsListener != null) {
            analyticsListener.onDrmKeysLoaded(eventTime);
        }
    }

    @Override
    public void onDrmSessionManagerError(EventTime eventTime, Exception error) {
        if (analyticsListener != null) {
            analyticsListener.onDrmSessionManagerError(eventTime, error);
        }
    }

    @Override
    public void onDrmKeysRestored(EventTime eventTime) {
        if (analyticsListener != null) {
            analyticsListener.onDrmKeysRestored(eventTime);
        }
    }

    @Override
    public void onDrmKeysRemoved(EventTime eventTime) {
        if (analyticsListener != null) {
            analyticsListener.onDrmKeysRemoved(eventTime);
        }
    }

    /**
     * Specifies the surface to clear when the playback reaches an appropriate state.
     * Once the <code>clearableSurface</code> is cleared, the reference will be removed
     *
     * @param clearableSurface The {@link ClearableSurface} to clear when the playback reaches an appropriate state
     */
    public void clearSurfaceWhenReady(@Nullable ClearableSurface clearableSurface) {
        clearRequested = true;
        clearableSurfaceRef = new WeakReference<>(clearableSurface);
    }

    /**
     * Sets the listener to inform of VideoPlayer prepared events
     *
     * @param listener The listener to inform
     */
    public void setOnPreparedListener(@Nullable OnPreparedListener listener) {
        preparedListener = listener;
    }

    /**
     * Sets the listener to inform of VideoPlayer completion events
     *
     * @param listener The listener to inform
     */
    public void setOnCompletionListener(@Nullable OnCompletionListener listener) {
        completionListener = listener;
    }

    /**
     * Sets the listener to inform of buffering updates
     *
     * @param listener The listener to inform
     */
    public void setOnBufferUpdateListener(@Nullable OnBufferUpdateListener listener) {
        bufferUpdateListener = listener;
    }

    /**
     * Sets the listener to inform of VideoPlayer seek completion events
     *
     * @param listener The listener to inform
     */
    public void setOnSeekCompletionListener(@Nullable OnSeekCompletionListener listener) {
        seekCompletionListener = listener;
    }

    /**
     * Sets the listener to inform of playback errors
     *
     * @param listener The listener to inform
     */
    public void setOnErrorListener(@Nullable OnErrorListener listener) {
        errorListener = listener;
    }

    /**
     * Sets the listener to inform of ID3 metadata updates
     *
     * @param listener The listener to inform
     */
    public void setMetadataListener(@Nullable MetadataListener listener) {
        metadataListener = listener;
    }

    /**
     * Sets the listener to inform of Analytics updates
     *
     * @param listener The listener to inform
     */
    public void setAnalyticsListener(@Nullable AnalyticsListener listener) {
        analyticsListener = listener;
    }

    /**
     * Sets weather the listener was notified when we became prepared.
     *
     * @param wasNotified True if the onPreparedListener was already notified
     */
    public void setNotifiedPrepared(boolean wasNotified) {
        notifiedPrepared = wasNotified;
        muxNotifier.onPreviewImageStateChanged(true);
    }

    /**
     * Retrieves if the player was prepared
     *
     * @return True if the player was prepared
     */
    public boolean isPrepared() {
        return notifiedPrepared;
    }

    /**
     * Sets weather the listener was notified when the playback was completed
     * (played through the end).
     *
     * @param wasNotified True if the onCompletionListener was already notified
     */
    public void setNotifiedCompleted(boolean wasNotified) {
        notifiedCompleted = wasNotified;
    }

    private boolean notifyErrorListener(Exception e) {
        return errorListener != null && errorListener.onError(e);
    }

    private void notifyPreparedListener() {
        notifiedPrepared = true;

        delayedHandler.post(new Runnable() {
            @Override
            public void run() {
                performPreparedHandlerNotification();
            }
        });
    }

    private void performPreparedHandlerNotification() {
        muxNotifier.onPrepared();

        if (preparedListener != null) {
            preparedListener.onPrepared();
        }
    }

    private void notifyCompletionListener() {
        if (!muxNotifier.shouldNotifyCompletion(COMPLETED_DURATION_LEEWAY)) {
            return;
        }

        notifiedCompleted = true;

        delayedHandler.post(new Runnable() {
            @Override
            public void run() {
                if (completionListener != null) {
                    completionListener.onCompletion();
                }
            }
        });
    }

    public static abstract class Notifier {
        public void onSeekComplete() {
            //Purposefully left blank
        }

        public void onBufferUpdated(int percent) {
            //Purposefully left blank
        }

        public void onVideoSizeChanged(int width, int height, int unAppliedRotationDegrees, float pixelWidthHeightRatio) {
            //Purposefully left blank
        }

        public void onPrepared() {
            //Purposefully left blank
        }

        public void onPreviewImageStateChanged(boolean toVisible) {
            //Purposefully left blank
        }

        public abstract boolean shouldNotifyCompletion(long endLeeway);

        public abstract void onExoPlayerError(ExoMediaPlayer exoMediaPlayer, Exception e);

        public abstract void onMediaPlaybackEnded();
    }
}