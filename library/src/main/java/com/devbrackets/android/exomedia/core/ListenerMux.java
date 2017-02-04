/*
 * Copyright (C) 2015-2017 Brian Wernick
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
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.metadata.Metadata;

import java.lang.ref.WeakReference;

/**
 * An internal Listener that implements the listeners for the {@link ExoMediaPlayer},
 * Android VideoView, and the Android MediaPlayer to output to the correct
 * error listeners.
 */
public class ListenerMux implements ExoPlayerListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnSeekCompleteListener, OnBufferUpdateListener, MetadataListener {
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
        if (playbackState == ExoPlayer.STATE_ENDED) {
            muxNotifier.onMediaPlaybackEnded();

            if (!notifiedCompleted) {
                notifyCompletionListener();
            }
        } else if (playbackState == ExoPlayer.STATE_READY && !notifiedPrepared) {
            notifyPreparedListener();
        }

        //Updates the previewImage
        if (playbackState == ExoPlayer.STATE_READY && playWhenReady) {
            muxNotifier.onPreviewImageStateChanged(false);
        }

        //Clears the textureView when requested
        if (playbackState == ExoPlayer.STATE_IDLE && clearRequested) {
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