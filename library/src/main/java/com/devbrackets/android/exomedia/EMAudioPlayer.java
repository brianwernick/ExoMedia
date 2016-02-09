/*
 * Copyright (C) 2016 Brian Wernick
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

import com.devbrackets.android.exomedia.core.EMListenerMux;
import com.devbrackets.android.exomedia.core.audio.ExoMediaPlayer;
import com.devbrackets.android.exomedia.core.audio.NativeMediaPlayer;
import com.devbrackets.android.exomedia.core.builder.RenderBuilder;
import com.devbrackets.android.exomedia.core.exoplayer.EMExoPlayer;
import com.devbrackets.android.exomedia.core.listener.ExoPlayerListener;
import com.devbrackets.android.exomedia.core.type.MediaPlayerApi;
import com.devbrackets.android.exomedia.util.EMDeviceUtil;

/**
 * An AudioPlayer that uses the ExoPlayer as the backing architecture.  If the current device
 * does <em>NOT</em> pass the Android Compatibility Test Suite (CTS) then the backing architecture
 * will fall back to using the default Android MediaPlayer.
 * <p>
 * To help with quick conversions from the Android MediaPlayer this class follows the APIs
 * the MediaPlayer provides.
 */
@SuppressWarnings("UnusedDeclaration")
public class EMAudioPlayer {
    private EMListenerMux listenerMux;

    protected MediaPlayerApi mediaPlayerImpl;
    private int overriddenDuration = -1;

    public EMAudioPlayer(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN  && EMDeviceUtil.isDeviceCTSCompliant()) {
            mediaPlayerImpl = new ExoMediaPlayer(context);
        } else {
            mediaPlayerImpl = new NativeMediaPlayer(context);
        }

        listenerMux = new EMListenerMux(new MuxNotifier());
        mediaPlayerImpl.setListenerMux(listenerMux);
    }

    /**
     * Returns the audio session ID.
     *
     * @return the audio session ID.
     * Note that the audio session ID is 0 only if a problem occurred when the AudioPlayer was constructed or the audio stream hasn't been
     * instantiated.
     */
    public int getAudioSessionId() {
        return mediaPlayerImpl.getAudioSessionId();
    }

    public void setAudioStreamType(int streamType) {
        mediaPlayerImpl.setAudioStreamType(streamType);
    }

    /**
     * Sets the source path for the audio item.  This path can be a web address (e.g. http://) or
     * an absolute local path (e.g. file://)
     *
     * @param context The applications context that owns the media
     * @param uri The Uri representing the path to the audio item
     */
    public void setDataSource(Context context, Uri uri) {
        mediaPlayerImpl.setDataSource(context, uri);
        overrideDuration(-1);
    }

    /**
     * Sets the source path for the audio item.  This path can be a web address (e.g. http://) or
     * an absolute local path (e.g. file://)
     *
     * @param context The applications context that owns the media
     * @param uri The Uri representing the path to the audio item
     * @param renderBuilder The RenderBuilder to use for audio playback
     */
    public void setDataSource(Context context, Uri uri, RenderBuilder renderBuilder) {
        mediaPlayerImpl.setDataSource(context, uri, renderBuilder);
        overrideDuration(-1);
    }

    public void prepareAsync() {
        mediaPlayerImpl.prepareAsync();
    }

    /**
     * Sets the volume level for the audio playback.
     *
     * @param leftVolume The volume range [0.0 - 1.0]
     * @param rightVolume The volume range [0.0 - 1.0]
     */
    public void setVolume(float leftVolume, float rightVolume) {
        mediaPlayerImpl.setVolume(leftVolume, rightVolume);
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
        mediaPlayerImpl.setWakeMode(context, mode);
    }

    /**
     * Stops the current audio playback and resets the listener states
     * so that we receive the callbacks for events like onPrepared
     */
    public void reset() {
        stopPlayback();
        setDataSource(null, null);

        mediaPlayerImpl.reset();
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

        mediaPlayerImpl.seekTo(milliSeconds);
    }

    /**
     * Returns if an audio item is currently in playback
     *
     * @return True if an audio item is playing
     */
    public boolean isPlaying() {
        return mediaPlayerImpl.isPlaying();
    }

    /**
     * Starts the playback for the audio item specified in {@link #setDataSource(android.content.Context, android.net.Uri)}.
     * This should be called after the AudioPlayer is correctly prepared (see {@link #setOnPreparedListener(android.media.MediaPlayer.OnPreparedListener)})
     */
    public void start() {
        mediaPlayerImpl.start();
    }

    /**
     * If an audio item is currently in playback, it will be paused
     */
    public void pause() {
        mediaPlayerImpl.pause();
    }

    /**
     * If an audio item is currently in playback then the playback will be stopped
     */
    public void stopPlayback() {
        mediaPlayerImpl.stopPlayback();
    }

    public void release() {
        mediaPlayerImpl.release();
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

        return mediaPlayerImpl.getDuration();
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
        return mediaPlayerImpl.getCurrentPosition();
    }

    /**
     * Retrieves the current buffer percent of the audio item.  If an audio item is not currently
     * prepared or buffering the value will be 0.  This should only be called after the audio item is
     * prepared (see {@link #setOnPreparedListener(android.media.MediaPlayer.OnPreparedListener)})
     *
     * @return The integer percent that is buffered [0, 100] inclusive
     */
    public int getBufferPercentage() {
        return mediaPlayerImpl.getBufferedPercent();
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
     * Sets the listener to inform of VideoPlayer prepared events
     *
     * @param listener The listener
     */
    public void setOnPreparedListener(MediaPlayer.OnPreparedListener listener) {
        listenerMux.setOnPreparedListener(listener);
    }

    /**
     * Sets the listener to inform of VideoPlayer completion events
     *
     * @param listener The listener
     */
    public void setOnCompletionListener(MediaPlayer.OnCompletionListener listener) {
        listenerMux.setOnCompletionListener(listener);
    }

    /**
     * Sets the listener to inform of playback errors
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
    }

    private class MuxNotifier extends EMListenerMux.EMListenerMuxNotifier {
        @Override
        public boolean shouldNotifyCompletion(long endLeeway) {
            return getCurrentPosition() + endLeeway >= getDuration();
        }

        @Override
        public void onExoPlayerError(EMExoPlayer emExoPlayer, Exception e) {
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
            //purposefully left blank
        }
    }
}
