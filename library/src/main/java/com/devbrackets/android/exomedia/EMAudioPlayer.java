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

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.Log;

import com.devbrackets.android.exomedia.builder.HlsRenderBuilder;
import com.devbrackets.android.exomedia.builder.RenderBuilder;
import com.devbrackets.android.exomedia.event.EMMediaProgressEvent;
import com.devbrackets.android.exomedia.exoplayer.EMExoPlayer;
import com.devbrackets.android.exomedia.listener.EMProgressCallback;
import com.devbrackets.android.exomedia.listener.ExoPlayerListener;
import com.devbrackets.android.exomedia.util.EMDeviceUtil;
import com.devbrackets.android.exomedia.util.EMEventBus;
import com.devbrackets.android.exomedia.util.MediaUtil;
import com.devbrackets.android.exomedia.util.Repeater;
import com.devbrackets.android.exomedia.util.StopWatch;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.audio.AudioCapabilitiesReceiver;

/**
 * An AudioPlayer that uses the ExoPlayer as the backing architecture.  If the current device
 * does <em>NOT</em> pass the Android Compatibility Test Suite (CTS) then the backing architecture
 * will fall back to using the default Android MediaPlayer.
 * <p>
 * To help with quick conversions from the Android MediaPlayer this class follows the APIs
 * the MediaPlayer provides.
 */
@SuppressWarnings("UnusedDeclaration")
public class EMAudioPlayer implements AudioCapabilitiesReceiver.Listener {
    private static final String TAG = EMAudioPlayer.class.getSimpleName();
    private static final String USER_AGENT_FORMAT = "EMAudioPlayer %s / Android %s / %s";

    public enum AudioType {
        HLS,
        DEFAULT;

        public static AudioType get(Uri uri) {
            if (uri.toString().matches(".*\\.m3u8.*")) {
                return AudioType.HLS;
            }

            return AudioType.DEFAULT;
        }
    }

    private Context context;
    private MediaPlayer mediaPlayer;
    private EMExoPlayer emExoPlayer;
    private EMListenerMux listenerMux;

    private boolean useExo;
    private int currentBufferPercent = 0;
    private int overriddenDuration = -1;
    private int positionOffset = 0;

    private boolean overridePosition = false;

    @Nullable
    private EMEventBus bus;
    private EMProgressCallback progressCallback;

    private Repeater pollRepeater = new Repeater();
    private StopWatch overriddenPositionStopWatch = new StopWatch();

    private AudioCapabilities audioCapabilities;
    private AudioCapabilitiesReceiver audioCapabilitiesReceiver;

    private EMMediaProgressEvent currentMediaProgressEvent = new EMMediaProgressEvent(0, 0, 0);

    public EMAudioPlayer(Context context) {
        this.context = context;
        useExo = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN  && EMDeviceUtil.isDeviceCTSCompliant();

        if (!useExo && mediaPlayer == null) {
            setupMediaPlayer();
        } else if (useExo && emExoPlayer == null) {
            setupEMExoPlayer();
        }

        pollRepeater.setRepeatListener(new Repeater.RepeatListener() {
            @Override
            public void onRepeat() {
                currentMediaProgressEvent.update(getCurrentPosition(), getBufferPercentage(), getDuration());

                if (progressCallback != null && progressCallback.onProgressUpdated(currentMediaProgressEvent)) {
                    return;
                }

                if (bus != null) {
                    bus.post(currentMediaProgressEvent);
                }
            }
        });
    }

    /**
     * Creates the ExoPlayer and sets the listeners
     */
    private void setupEMExoPlayer() {
        if (audioCapabilitiesReceiver == null) {
            audioCapabilitiesReceiver = new AudioCapabilitiesReceiver(context.getApplicationContext(), this);
            audioCapabilitiesReceiver.register();
        }

        if (emExoPlayer == null) {
            emExoPlayer = new EMExoPlayer();

            //Sets the internal listener
            listenerMux = new EMListenerMux(new MuxNotifier());
            emExoPlayer.addListener(listenerMux);

            emExoPlayer.setMetadataListener(null);
        }
    }

    /**
     * Initializes the MediaPlayer and sets the listeners
     */
    private void setupMediaPlayer() {
        listenerMux = new EMListenerMux(new MuxNotifier());

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(listenerMux);
        mediaPlayer.setOnPreparedListener(listenerMux);
        mediaPlayer.setOnErrorListener(listenerMux);
        mediaPlayer.setOnBufferingUpdateListener(listenerMux);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            mediaPlayer.setOnInfoListener(listenerMux);
        }
    }

    @Override
    public void onAudioCapabilitiesChanged(AudioCapabilities audioCapabilities) {
        if (!audioCapabilities.equals(this.audioCapabilities)) {
            this.audioCapabilities = audioCapabilities;
        }
    }

    /**
     * Sets the delay to use when notifying of progress.  The
     * default is 33 milliseconds, or 30 frames-per-second
     *
     * @param milliSeconds The millisecond delay to use
     */
    public void setProgressPollDelay(int milliSeconds) {
        pollRepeater.setRepeaterDelay(milliSeconds);
    }

    /**
     * Sets the bus to use for dispatching Events such as the poll progress
     *
     * @param bus The EventBus to dispatch events on
     */
    public void setBus(@Nullable EMEventBus bus) {
        this.bus = bus;
        listenerMux.setBus(bus);
    }

    /**
     * Starts the progress poll.
     *
     * @param bus The EventBus event dispatcher that the listener is connected to
     */
    public void startProgressPoll(@Nullable EMEventBus bus) {
        setBus(bus);

        if (bus != null) {
            pollRepeater.start();
        }
    }

    /**
     * Starts the progress poll with the callback to be informed of the progress
     * events.
     *
     * @param callback The Callback to inform of progress events
     */
    public void startProgressPoll(EMProgressCallback callback) {
        progressCallback = callback;

        if (progressCallback != null) {
            pollRepeater.start();
        }
    }

    /**
     * Stops the progress poll
     * (see {@link #startProgressPoll(EMEventBus)})
     */
    public void stopProgressPoll() {
        pollRepeater.stop();
    }

    /**
     * Creates and returns the correct render builder for the specified AudioType and uri.
     *
     * @param renderType        The RenderType to use for creating the correct RenderBuilder
     * @param uri               The audio item's Uri
     * @param defaultMediaType  The MediaType to use when auto-detection fails
     * @return                  The appropriate RenderBuilder
     */
    private RenderBuilder getRendererBuilder(AudioType renderType, Uri uri, MediaUtil.MediaType defaultMediaType) {
        switch (renderType) {
            case HLS:
                return new HlsRenderBuilder(context, getUserAgent(), uri.toString());
            default:
                return new RenderBuilder(context, getUserAgent(), uri.toString(), defaultMediaType);
        }
    }

    /**
     * Retrieves the user agent that the EMAudioPlayer will use when communicating
     * with media servers
     *
     * @return The String user agent for the EMAudioPlayer
     */
    public String getUserAgent() {
        return String.format(USER_AGENT_FORMAT, BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")", Build.VERSION.RELEASE, Build.MODEL);
    }

    /**
     * ***************************************
     * Start of the standard MediaPlayer APIs *
     * ****************************************
     */

    /**
     * Returns the audio session ID.
     *
     * @return the audio session ID.
     * Note that the audio session ID is 0 only if a problem occurred when the AudioPlayer was constructed or the audio stream hasn't been
     * instantiated.
     */
    public int getAudioSessionId() {
        if (!useExo) {
            return mediaPlayer.getAudioSessionId();
        }

        return emExoPlayer.getAudioSessionId();
    }

    public void setAudioStreamType(int steamType) {
        if (!useExo) {
            mediaPlayer.setAudioStreamType(steamType);
        }

        //The ExoPlayer doesn't need this information
    }

    /**
     * Sets the source path for the audio item.  This path can be a web address (e.g. http://) or
     * an absolute local path (e.g. file://). Uses MP3 as the default for media type.
     *
     * @param context The applications context that owns the media
     * @param uri The Uri representing the path to the audio item
     */
    public void setDataSource(Context context, Uri uri) {
        setDataSource(context, uri, MediaUtil.MediaType.MP3);
    }


    /**
     * Sets the source path for the audio item.  This path can be a web address (e.g. http://) or
     * an absolute local path (e.g. file://)
     *
     * @param context The applications context that owns the media
     * @param uri The Uri representing the path to the audio item
     * @param defaultMediaType The MediaType to use when auto-detection fails
     */
    public void setDataSource(Context context, Uri uri, MediaUtil.MediaType defaultMediaType) {
        if (!useExo) {
            try {
                mediaPlayer.setDataSource(context, uri);
            } catch (Exception e) {
                Log.d(TAG, "MediaPlayer: error setting data source", e);
            }
        } else {
            if (uri != null) {
                emExoPlayer.replaceRenderBuilder(getRendererBuilder(AudioType.get(uri), uri, defaultMediaType));
                listenerMux.setNotifiedCompleted(false);
            } else {
                emExoPlayer.replaceRenderBuilder(null);
            }

            emExoPlayer.seekTo(0);
        }

        listenerMux.setNotifiedPrepared(false);
        overrideDuration(-1);
        setPositionOffset(0);
    }

    public void prepareAsync() {
        if (!useExo) {
            mediaPlayer.prepareAsync();
        } else {
            emExoPlayer.prepare();
        }
    }

    /**
     * Sets the volume level for the audio playback.
     *
     * @param leftVolume The volume range [0.0 - 1.0]
     * @param rightVolume The volume range [0.0 - 1.0]
     */
    public void setVolume(float leftVolume, float rightVolume) {
        if (!useExo) {
            mediaPlayer.setVolume(leftVolume, rightVolume);
        } else {
            emExoPlayer.setVolume(leftVolume);
        }
    }

    /**
     * Set the low-level power management behavior for this EMAudioPlayer.
     *
     * <p>This function has the EMAudioPlayer access the low-level power manager
     * service to control the device's power usage while playing is occurring.
     * The parameter is a combination of {@link android.os.PowerManager} wake flags.
     * Use of this method requires {@link android.Manifest.permission#WAKE_LOCK}
     * permission.
     * By default, no attempt is made to keep the device awake during playback.
     *
     * @param context the Context to use
     * @param mode    the power/wake mode to set
     * @see android.os.PowerManager
     */
    public void setWakeMode(Context context, int mode) {
        if (!useExo) {
            mediaPlayer.setWakeMode(context, mode);
        } else {
            emExoPlayer.setWakeMode(context, mode);
        }
    }

    /**
     * Stops the current audio playback and resets the listener states
     * so that we receive the callbacks for events like onPrepared
     */
    public void reset() {
        stopPlayback();
        setDataSource(null, null);

        if (!useExo) {
            mediaPlayer.reset();
        }
    }

    /**
     * Moves the current audio progress to the specified location.
     * This method should only be called after the EMAudioPlayer is
     * prepared. (see {@link #setOnPreparedListener(android.media.MediaPlayer.OnPreparedListener)}
     *
     * @param milliSeconds The time to move the playback to
     */
    public void seekTo(int milliSeconds) {
        if (!listenerMux.isPrepared()) {
            return;
        }

        if (milliSeconds > getDuration()) {
            milliSeconds = (int)getDuration();
        }

        if (!useExo) {
            mediaPlayer.seekTo(milliSeconds);
        } else {
            emExoPlayer.seekTo(milliSeconds);
        }
    }

    /**
     * Returns if an audio item is currently in playback
     *
     * @return True if an audio item is playing
     */
    public boolean isPlaying() {
        if (!useExo) {
            return mediaPlayer.isPlaying();
        }

        return emExoPlayer.getPlayWhenReady();
    }

    /**
     * Starts the playback for the audio item specified in {@link #setDataSource(android.content.Context, android.net.Uri)}.
     * This should be called after the AudioPlayer is correctly prepared (see {@link #setOnPreparedListener(android.media.MediaPlayer.OnPreparedListener)})
     */
    public void start() {
        if (!useExo) {
            mediaPlayer.start();
        } else {
            emExoPlayer.setPlayWhenReady(true);
        }

        startProgressPoll(bus);
        startProgressPoll(progressCallback);
    }

    /**
     * If an audio item is currently in playback, it will be paused and the progressPoll
     * will be stopped (see {@link #startProgressPoll(EMEventBus)})
     */
    public void pause() {
        if (!useExo) {
            mediaPlayer.pause();
        } else {
            emExoPlayer.setPlayWhenReady(false);
        }

        stopProgressPoll();
    }

    /**
     * If an audio item is currently in playback then the playback will be stopped
     * and the progressPoll will be stopped (see {@link #startProgressPoll(EMEventBus)})
     */
    public void stopPlayback() {
        if (!useExo) {
            mediaPlayer.stop();
        } else {
            emExoPlayer.setPlayWhenReady(false);
        }

        stopProgressPoll();
    }

    public void release() {
        if (!useExo) {
            mediaPlayer.release();
        } else {
            emExoPlayer.release();
        }

        stopProgressPoll();
        overriddenPositionStopWatch.stop();

        if (audioCapabilitiesReceiver != null) {
            audioCapabilitiesReceiver.unregister();
            audioCapabilitiesReceiver = null;
        }
    }

    /**
     * Retrieves the duration of the current audio item.  This should only be called after
     * the item is prepared (see {@link #setOnPreparedListener(android.media.MediaPlayer.OnPreparedListener)}).
     * If {@link #overrideDuration(int)} is set then that value will be returned.
     *
     * @return The millisecond duration of the video
     */
    public long getDuration() {
        if (overriddenDuration >= 0) {
            return overriddenDuration;
        }

        if (!listenerMux.isPrepared()) {
            return 0;
        }

        if (!useExo) {
            return mediaPlayer.getDuration();
        }

        return emExoPlayer.getDuration();
    }

    /**
     * Setting this will override the duration that the item may actually be.  This method should
     * only be used when the item doesn't return the correct duration such as with audio streams.
     * This only overrides the current audio item.
     *
     * @param duration The duration for the current media item or &lt; 0 to disable
     */
    public void overrideDuration(int duration) {
        overriddenDuration = duration;
    }

    /**
     * Retrieves the current position of the audio playback.  If an audio item is not currently
     * in playback then the value will be 0.  This should only be called after the item is
     * prepared (see {@link #setOnPreparedListener(android.media.MediaPlayer.OnPreparedListener)})
     *
     * @return The millisecond value for the current position
     */
    public long getCurrentPosition() {
        if (overridePosition) {
            return positionOffset + overriddenPositionStopWatch.getTime();
        }

        if (!listenerMux.isPrepared()) {
            return 0;
        }

        if (!useExo) {
            return positionOffset + mediaPlayer.getCurrentPosition();
        }

        return positionOffset + emExoPlayer.getCurrentPosition();
    }

    /**
     * Sets the amount of time to change the return value from {@link #getCurrentPosition()}.
     * This value will be reset when a new audio item is selected.
     *
     * @param offset The millisecond value to offset the position
     */
    public void setPositionOffset(int offset) {
        positionOffset = offset;
    }

    /**
     * Restarts the audio position to the start if the position is being overridden (see {@link #overridePosition(boolean)}).
     * This will be the value specified with {@link #setPositionOffset(int)} or 0 if it hasn't been set.
     */
    public void restartOverridePosition() {
        overriddenPositionStopWatch.reset();
    }

    /**
     * Sets if the audio position should be overridden, allowing the time to be restarted at will.  This
     * is useful for streaming audio where the audio doesn't have breaks between songs.
     *
     * @param override True if the position should be overridden
     */
    public void overridePosition(boolean override) {
        if (override) {
            overriddenPositionStopWatch.start();
        } else {
            overriddenPositionStopWatch.stop();
        }

        overridePosition = override;
    }

    /**
     * Retrieves the current buffer percent of the audio item.  If an audio item is not currently
     * prepared or buffering the value will be 0.  This should only be called after the audio item is
     * prepared (see {@link #setOnPreparedListener(android.media.MediaPlayer.OnPreparedListener)})
     *
     * @return The integer percent that is buffered [0, 100] inclusive
     */
    public int getBufferPercentage() {
        if (!listenerMux.isPrepared()) {
            return 0;
        }

        if (!useExo) {
            return currentBufferPercent;
        }

        return emExoPlayer.getBufferedPercentage();
    }

    /**
     * Sets the listener to inform of any exoPlayer events
     *
     * @param listener The listener
     */
    public void addExoPlayerListener(ExoPlayerListener listener) {
        listenerMux.addExoPlayerListener(listener);
    }

    /**
     * Removes the specified listener for the ExoPlayer.
     *
     * @param listener The listener to remove
     */
    public void removeExoPlayerListener(ExoPlayerListener listener) {
        listenerMux.removeExoPlayerListener(listener);
    }

    /**
     * Sets the listener to inform of VideoPlayer prepared events.  This can also be
     * accessed through the bus event {@link com.devbrackets.android.exomedia.event.EMMediaPreparedEvent}
     *
     * @param listener The listener
     */
    public void setOnPreparedListener(MediaPlayer.OnPreparedListener listener) {
        listenerMux.setOnPreparedListener(listener);
    }

    /**
     * Sets the listener to inform of VideoPlayer completion events.  This can also be
     * accessed through the bus event {@link com.devbrackets.android.exomedia.event.EMMediaCompletionEvent}
     *
     * @param listener The listener
     */
    public void setOnCompletionListener(MediaPlayer.OnCompletionListener listener) {
        listenerMux.setOnCompletionListener(listener);
    }

    /**
     * Sets the listener to inform of playback errors.  This can also be
     * accessed through the bus event {@link com.devbrackets.android.exomedia.event.EMMediaErrorEvent}
     *
     * @param listener The listener
     */
    public void setOnErrorListener(MediaPlayer.OnErrorListener listener) {
        listenerMux.setOnErrorListener(listener);
    }

    /**
     * Sets the listener to inform of media information events.
     *
     * @param listener The listener
     */
    public void setOnInfoListener(MediaPlayer.OnInfoListener listener) {
        listenerMux.setOnInfoListener(listener);
    }

    /**
     * Sets the listener to inform of buffering updates
     *
     * @param listener The listener
     */
    public void setOnBufferingUpdateListener(android.media.MediaPlayer.OnBufferingUpdateListener listener) {
        listenerMux.setOnBufferingUpdateListener(listener);
    }

    /**
     * Performs the functionality to stop the progress polling, and stop any other
     * procedures from running that we no longer need.
     */
    private void onPlaybackEnded() {
        stopPlayback();
        pollRepeater.stop();
    }

    private class MuxNotifier extends EMListenerMux.EMListenerMuxNotifier {
        @Override
        public boolean shouldNotifyCompletion(long endLeeway) {
            return getCurrentPosition() + endLeeway >= getDuration();
        }

        @Override
        public void onExoPlayerError(Exception e) {
            if (emExoPlayer != null) {
                emExoPlayer.forcePrepare();
            }
        }

        @Override
        public void onMediaPlaybackEnded() {
           onPlaybackEnded();
        }

        @Override
        public void onBufferUpdated(int percent) {
            currentBufferPercent = percent;
        }
    }
}
