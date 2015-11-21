/*
 * Copyright (C) 2015 Brian Wernick
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

package com.devbrackets.android.exomedia;

import android.media.MediaPlayer;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.devbrackets.android.exomedia.event.EMMediaCompletionEvent;
import com.devbrackets.android.exomedia.event.EMMediaErrorEvent;
import com.devbrackets.android.exomedia.event.EMMediaPreparedEvent;
import com.devbrackets.android.exomedia.listener.ExoPlayerListener;
import com.devbrackets.android.exomedia.util.EMEventBus;
import com.google.android.exoplayer.ExoPlayer;

import java.util.LinkedList;
import java.util.List;

/**
 * An internal Listener that implements the listeners for the EMExoPlayer,
 * Android VideoView, and the Android MediaPlayer to output to the correct
 * error listeners.
 */
class EMListenerMux implements ExoPlayerListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener {

    //The amount of time the current position can be off the duration to call the onCompletion listener
    private static final long COMPLETED_DURATION_LEEWAY = 1000;
    private boolean notifiedPrepared = false;
    private boolean notifiedCompleted = false;

    private Handler delayedHandler = new Handler();
    private EMListenerMuxNotifier muxNotifier;

    @Nullable
    private EMEventBus bus;

    private List<ExoPlayerListener> exoPlayerListeners = new LinkedList<>();

    private MediaPlayer.OnBufferingUpdateListener bufferingUpdateListener;
    private MediaPlayer.OnCompletionListener completionListener;
    private MediaPlayer.OnPreparedListener preparedListener;
    private MediaPlayer.OnErrorListener errorListener;
    private MediaPlayer.OnInfoListener infoListener;


    EMListenerMux(@NonNull EMListenerMuxNotifier notifier) {
        muxNotifier = notifier;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        muxNotifier.onBufferUpdated(percent);

        if (bufferingUpdateListener != null) {
            bufferingUpdateListener.onBufferingUpdate(mp, percent);
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (completionListener != null) {
            completionListener.onCompletion(null);
        }

        if (bus != null) {
            bus.post(new EMMediaCompletionEvent());
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        if (errorListener != null && errorListener.onError(mp, what, extra)) {
            return true;
        }

        if (bus != null) {
            bus.post(new EMMediaErrorEvent(mp, what, extra));
            return true;
        }

        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        return infoListener != null && infoListener.onInfo(mp, what, extra);
    }

    @Override
    public void onPrepared(final MediaPlayer mp) {
        notifiedPrepared = true;

        delayedHandler.post(new Runnable() {
            @Override
            public void run() {
                performPreparedHandlerNotification(mp);
            }
        });
    }

    @Override
    public void onError(Exception e) {
        muxNotifier.onExoPlayerError(e);
        muxNotifier.onMediaPlaybackEnded();

        if (errorListener != null && errorListener.onError(null, 0, 0)) {
            return;
        }

        for (ExoPlayerListener listener : exoPlayerListeners) {
            listener.onError(e);
        }

        if (bus != null) {
            bus.post(new EMMediaErrorEvent(null, 0, 0));
        }
    }

    @Override
    public void onStateChanged(boolean playWhenReady, int playbackState) {
        if (playbackState == ExoPlayer.STATE_ENDED) {
            muxNotifier.onMediaPlaybackEnded();

            if (!notifiedCompleted) {
                notifyCompletionListener();
            }
        } else if (playbackState == ExoPlayer.STATE_READY && !notifiedPrepared) {
            notifyPreparedListener(null);
        }

        for (ExoPlayerListener listener : exoPlayerListeners) {
            listener.onStateChanged(playWhenReady, playbackState);
        }

        //Updates the previewImage
        if (playbackState == ExoPlayer.STATE_READY && playWhenReady) {
            muxNotifier.onPreviewImageStateChanged(false);
        }
    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        muxNotifier.onVideoSizeChanged(width, height, pixelWidthHeightRatio);

        for (ExoPlayerListener listener : exoPlayerListeners) {
            listener.onVideoSizeChanged(width, height, unappliedRotationDegrees, pixelWidthHeightRatio);
        }
    }

    public void setBus(@Nullable EMEventBus bus) {
        this.bus = bus;
    }

    /**
     * Sets the listener to inform of any exoPlayer events
     *
     * @param listener The listener
     */
    public void addExoPlayerListener(ExoPlayerListener listener) {
        if (listener != null) {
            exoPlayerListeners.add(listener);
        }
    }

    /**
     * Removes the specified listener for the ExoPlayer.
     *
     * @param listener The listener to remove
     */
    public void removeExoPlayerListener(ExoPlayerListener listener) {
        if (listener != null && exoPlayerListeners.contains(listener)) {
            exoPlayerListeners.remove(listener);
        }
    }

    /**
     * Sets the listener to inform of VideoPlayer prepared events.  This can also be
     * accessed through the bus event {@link com.devbrackets.android.exomedia.event.EMMediaPreparedEvent}
     *
     * @param listener The listener
     */
    public void setOnPreparedListener(MediaPlayer.OnPreparedListener listener) {
        preparedListener = listener;
    }

    /**
     * Sets the listener to inform of VideoPlayer completion events.  This can also be
     * accessed through the bus event {@link com.devbrackets.android.exomedia.event.EMMediaCompletionEvent}
     *
     * @param listener The listener
     */
    public void setOnCompletionListener(MediaPlayer.OnCompletionListener listener) {
        completionListener = listener;
    }

    /**
     * Sets the listener to inform of playback errors.  This can also be
     * accessed through the bus event {@link com.devbrackets.android.exomedia.event.EMMediaErrorEvent}
     *
     * @param listener The listener
     */
    public void setOnErrorListener(MediaPlayer.OnErrorListener listener) {
        errorListener = listener;
    }

    /**
     * Sets the listener to inform of media information events.
     *
     * @param listener The listener
     */
    public void setOnInfoListener(MediaPlayer.OnInfoListener listener) {
        infoListener = listener;
    }

    /**
     * Sets the listener to inform of buffering updates
     *
     * @param listener The listener
     */
    public void setOnBufferingUpdateListener(android.media.MediaPlayer.OnBufferingUpdateListener listener) {
        bufferingUpdateListener = listener;
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

    private void notifyPreparedListener(final MediaPlayer mediaPlayer) {
        notifiedPrepared = true;

        delayedHandler.post(new Runnable() {
            @Override
            public void run() {
                performPreparedHandlerNotification(mediaPlayer);
            }
        });
    }

    private void performPreparedHandlerNotification(MediaPlayer mediaPlayer) {
        muxNotifier.onPrepared();

        if (preparedListener != null) {
            preparedListener.onPrepared(mediaPlayer);
        }

        if (bus != null) {
            bus.post(new EMMediaPreparedEvent());
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
                    completionListener.onCompletion(null);
                }

                if (bus != null) {
                    bus.post(new EMMediaCompletionEvent());
                }
            }
        });
    }


    @SuppressWarnings("UnusedParameters")
    public static abstract class EMListenerMuxNotifier {
        public void onBufferUpdated(int percent) {
            //Purposefully left blank
        }

        public void onVideoSizeChanged(int width, int height, float pixelWidthHeightRatio) {
            //Purposefully left blank
        }

        public void onPrepared() {
            //Purposefully left blank
        }

        public void onPreviewImageStateChanged(boolean toVisible) {
            //Purposefully left blank
        }

        public abstract boolean shouldNotifyCompletion(long endLeeway);

        public abstract void onExoPlayerError(Exception e);

        public abstract void onMediaPlaybackEnded();
    }
}