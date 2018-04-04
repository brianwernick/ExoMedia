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
import android.media.AudioManager;
import android.net.Uri;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.devbrackets.android.exomedia.core.ListenerMux;
import com.devbrackets.android.exomedia.core.api.AudioPlayerApi;
import com.devbrackets.android.exomedia.core.audio.ExoAudioPlayer;
import com.devbrackets.android.exomedia.core.audio.NativeAudioPlayer;
import com.devbrackets.android.exomedia.core.exoplayer.ExoMediaPlayer;
import com.devbrackets.android.exomedia.core.listener.MetadataListener;
import com.devbrackets.android.exomedia.listener.OnBufferUpdateListener;
import com.devbrackets.android.exomedia.listener.OnCompletionListener;
import com.devbrackets.android.exomedia.listener.OnErrorListener;
import com.devbrackets.android.exomedia.listener.OnPreparedListener;
import com.devbrackets.android.exomedia.listener.OnSeekCompletionListener;
import com.devbrackets.android.exomedia.util.DeviceUtil;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.drm.MediaDrmCallback;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;

import java.util.Map;

/**
 * An AudioPlayer that uses the ExoPlayer as the backing architecture.  If the current device
 * does <em>NOT</em> pass the Android Compatibility Test Suite (CTS) then the backing architecture
 * will fall back to using the default Android MediaPlayer.
 * <p>
 * To help with quick conversions from the Android MediaPlayer this class follows the APIs
 * the Android MediaPlayer provides.
 */
@SuppressWarnings("UnusedDeclaration")
public class AudioPlayer {
    protected ListenerMux listenerMux;

    protected AudioPlayerApi audioPlayerImpl;
    protected long overriddenDuration = -1;

    public AudioPlayer(@NonNull Context context) {
        this(context, new DeviceUtil());
    }

    public AudioPlayer(@NonNull Context context, @NonNull DeviceUtil deviceUtil) {
        init(deviceUtil.supportsExoPlayer(context) ? new ExoAudioPlayer(context) : new NativeAudioPlayer(context));
    }

    public AudioPlayer(AudioPlayerApi audioPlayerImpl) {
        init(audioPlayerImpl);
    }

    protected void init(AudioPlayerApi audioPlayerImpl) {
        this.audioPlayerImpl = audioPlayerImpl;

        listenerMux = new ListenerMux(new MuxNotifier());
        audioPlayerImpl.setListenerMux(listenerMux);
    }

    /**
     * Returns the audio session ID.
     *
     * @return the audio session ID.
     * Note that the audio session ID is 0 only if a problem occurred when the AudioPlayer was constructed or the audio stream hasn't been
     * instantiated.
     */
    public int getAudioSessionId() {
        return audioPlayerImpl.getAudioSessionId();
    }

    /**
     * Sets the repeat mode for this MediaPlayer.
     * <b>Note:</b> This will only change the ExoPlayer implementation
     *
     * @param repeatMode The repeat mode to use
     */
    public void setRepeatMode(@Player.RepeatMode int repeatMode) {
        audioPlayerImpl.setRepeatMode(repeatMode);
    }

    /**
     * Sets the playback speed for this MediaPlayer.
     *
     * @param speed The speed to play the media back at
     * @return True if the speed was set
     */
    public boolean setPlaybackSpeed(float speed) {
        return audioPlayerImpl.setPlaybackSpeed(speed);
    }

    /**
     * Sets the audio stream type for this MediaPlayer. See {@link AudioManager}
     * for a list of stream types. Must call this method before prepare() or
     * prepareAsync() in order for the target stream type to become effective
     * thereafter.
     *
     * @param streamType The audio stream type
     * @see android.media.AudioManager
     */
    public void setAudioStreamType(int streamType) {
        audioPlayerImpl.setAudioStreamType(streamType);
    }

    /**
     * Sets the source path for the audio item.  This path can be a web address (e.g. http://) or
     * an absolute local path (e.g. file://)
     *
     * @param uri The Uri representing the path to the audio item
     */
    public void setDataSource(@Nullable Uri uri) {
        audioPlayerImpl.setDataSource(uri);
        overrideDuration(-1);
    }

    /**
     * Sets the source path for the audio item.  This path can be a web address (e.g. http://) or
     * an absolute local path (e.g. file://)
     *
     * @param uri The Uri representing the path to the audio item
     * @param mediaSource The MediaSource to use for audio playback
     */
    public void setDataSource(@Nullable Uri uri, @Nullable MediaSource mediaSource) {
        audioPlayerImpl.setDataSource(uri, mediaSource);
        overrideDuration(-1);
    }

    /**
     * Sets the {@link MediaDrmCallback} to use when handling DRM for media.
     * This should be called before specifying the videos uri or path
     * <br>
     * <b>NOTE:</b> DRM is only supported on API 18 +
     *
     * @param drmCallback The callback to use when handling DRM media
     */
    public void setDrmCallback(@Nullable MediaDrmCallback drmCallback) {
        audioPlayerImpl.setDrmCallback(drmCallback);
    }

    /**
     * Prepares the media specified with {@link #setDataSource(Uri)} or
     * {@link #setDataSource(Uri, MediaSource)} in an asynchronous manner
     */
    public void prepareAsync() {
        audioPlayerImpl.prepareAsync();
    }

    /**
     * Retrieves the current left volume
     *
     * @return the volume for the left channel
     */
    @FloatRange(from = 0.0, to = 1.0)
    public float getVolumeLeft() {
        return audioPlayerImpl.getVolumeLeft();
    }

    /**
     * Retrieves the current right volume
     *
     * @return the volume for the right channel
     */
    @FloatRange(from = 0.0, to = 1.0)
    float getVolumeRight() {
        return audioPlayerImpl.getVolumeRight();
    }

    /**
     * Sets the volume level for the audio playback.
     *
     * @param leftVolume The volume range [0.0 - 1.0]
     * @param rightVolume The volume range [0.0 - 1.0]
     */
    public void setVolume(@FloatRange(from = 0.0, to = 1.0) float leftVolume, @FloatRange(from = 0.0, to = 1.0) float rightVolume) {
        audioPlayerImpl.setVolume(leftVolume, rightVolume);
    }

    /**
     * Set the low-level power management behavior for this AudioPlayer.
     * <p>
     * <p>This function has the AudioPlayer access the low-level power manager
     * service to control the device's power usage while playing is occurring.
     * The parameter is a combination of {@link android.os.PowerManager} wake flags.
     * Use of this method requires {@link android.Manifest.permission#WAKE_LOCK}
     * permission.
     * By default, no attempt is made to keep the device awake during playback.
     *
     * @param context the Context to use
     * @param mode the power/wake mode to set
     * @see android.os.PowerManager
     */
    public void setWakeMode(Context context, int mode) {
        audioPlayerImpl.setWakeMode(context, mode);
    }

    /**
     * Stops the current audio playback and resets the listener states
     * so that we receive the callbacks for events like onPrepared
     */
    public void reset() {
        stopPlayback();
        setDataSource(null, null);

        audioPlayerImpl.reset();
    }

    /**
     * Moves the current audio progress to the specified location.
     * This method should only be called after the AudioPlayer is
     * prepared. (see {@link #setOnPreparedListener(OnPreparedListener)}
     *
     * @param milliSeconds The time to move the playback to
     */
    public void seekTo(long milliSeconds) {
        audioPlayerImpl.seekTo(milliSeconds);
    }

    /**
     * Returns if an audio item is currently in playback
     *
     * @return True if an audio item is playing
     */
    public boolean isPlaying() {
        return audioPlayerImpl.isPlaying();
    }

    /**
     * Starts the playback for the audio item specified in {@link #setDataSource(Uri)}.
     * This should be called after the AudioPlayer is correctly prepared (see {@link #setOnPreparedListener(OnPreparedListener)})
     */
    public void start() {
        audioPlayerImpl.start();
    }

    /**
     * If an audio item is currently in playback, it will be paused
     */
    public void pause() {
        audioPlayerImpl.pause();
    }

    /**
     * If an audio item is currently in playback then the playback will be stopped
     */
    public void stopPlayback() {
        audioPlayerImpl.stopPlayback();
    }

    /**
     * Releases the resources associated with this media player
     */
    public void release() {
        audioPlayerImpl.release();
    }

    /**
     * Retrieves the duration of the current audio item.  This should only be called after
     * the item is prepared (see {@link #setOnPreparedListener(OnPreparedListener)}).
     * If {@link #overrideDuration(long)} is set then that value will be returned.
     *
     * @return The millisecond duration of the video
     */
    public long getDuration() {
        if (overriddenDuration >= 0) {
            return overriddenDuration;
        }

        return audioPlayerImpl.getDuration();
    }

    /**
     * Setting this will override the duration that the item may actually be.  This method should
     * only be used when the item doesn't return the correct duration such as with audio streams.
     * This only overrides the current audio item.
     *
     * @param duration The duration for the current media item or &lt; 0 to disable
     */
    public void overrideDuration(long duration) {
        overriddenDuration = duration;
    }

    /**
     * Retrieves the current position of the audio playback.  If an audio item is not currently
     * in playback then the value will be 0.  This should only be called after the item is
     * prepared (see {@link #setOnPreparedListener(OnPreparedListener)})
     *
     * @return The millisecond value for the current position
     */
    public long getCurrentPosition() {
        return audioPlayerImpl.getCurrentPosition();
    }

    /**
     * Retrieves the current buffer percent of the audio item.  If an audio item is not currently
     * prepared or buffering the value will be 0.  This should only be called after the audio item is
     * prepared (see {@link #setOnPreparedListener(OnPreparedListener)})
     *
     * @return The integer percent that is buffered [0, 100] inclusive
     */
    public int getBufferPercentage() {
        return audioPlayerImpl.getBufferedPercent();
    }

    /**
     * Determines if the current video player implementation supports
     * track selection for audio or video tracks.
     *
     * @return True if tracks can be manually specified
     */
    public boolean trackSelectionAvailable() {
        return audioPlayerImpl.trackSelectionAvailable();
    }

    /**
     * Changes to the track with <code>trackIndex</code> for the specified
     * <code>trackType</code>
     *
     * @param trackType The type for the track to switch to the selected index
     * @param trackIndex The index for the track to switch to
     */
    public void setTrack(ExoMedia.RendererType trackType, int trackIndex) {
        audioPlayerImpl.setTrack(trackType, trackIndex);
    }

    /**
     * Retrieves a list of available tracks to select from.  Typically {@link #trackSelectionAvailable()}
     * should be called before this.
     *
     * @return A list of available tracks associated with each track type
     */
    @Nullable
    public Map<ExoMedia.RendererType, TrackGroupArray> getAvailableTracks() {
        return audioPlayerImpl.getAvailableTracks();
    }

    /**
     * Sets the listener to inform of VideoPlayer prepared events
     *
     * @param listener The listener
     */
    public void setOnPreparedListener(@Nullable OnPreparedListener listener) {
        listenerMux.setOnPreparedListener(listener);
    }

    /**
     * Sets the listener to inform of VideoPlayer completion events
     *
     * @param listener The listener
     */
    public void setOnCompletionListener(@Nullable OnCompletionListener listener) {
        listenerMux.setOnCompletionListener(listener);
    }

    /**
     * Sets the listener to inform of VideoPlayer buffer update events
     *
     * @param listener The listener
     */
    public void setOnBufferUpdateListener(@Nullable OnBufferUpdateListener listener) {
        listenerMux.setOnBufferUpdateListener(listener);
    }

    /**
     * Sets the listener to inform of VideoPlayer seek completion events
     *
     * @param listener The listener
     */
    public void setOnSeekCompletionListener(@Nullable OnSeekCompletionListener listener) {
        listenerMux.setOnSeekCompletionListener(listener);
    }

    /**
     * Sets the listener to inform of playback errors
     *
     * @param listener The listener
     */
    public void setOnErrorListener(@Nullable OnErrorListener listener) {
        listenerMux.setOnErrorListener(listener);
    }

    /**
     * Sets the listener to inform of ID3 metadata updates
     *
     * @param listener The listener to inform
     */
    public void setMetadataListener(@Nullable MetadataListener listener) {
        listenerMux.setMetadataListener(listener);
    }

    /**
     * Performs the functionality to stop the progress polling, and stop any other
     * procedures from running that we no longer need.
     */
    private void onPlaybackEnded() {
        pause();
    }

    private class MuxNotifier extends ListenerMux.Notifier {
        @Override
        public boolean shouldNotifyCompletion(long endLeeway) {
            long position = getCurrentPosition();
            long duration = getDuration();
            return position > 0 && duration > 0 && position + endLeeway >= duration;
        }

        @Override
        public void onExoPlayerError(ExoMediaPlayer exoMediaPlayer, Exception e) {
            stopPlayback();

            if (exoMediaPlayer != null) {
                exoMediaPlayer.forcePrepare();
            }
        }

        @Override
        public void onMediaPlaybackEnded() {
            onPlaybackEnded();
        }

        @Override
        public void onPrepared() {
            audioPlayerImpl.onMediaPrepared();
        }
    }
}
