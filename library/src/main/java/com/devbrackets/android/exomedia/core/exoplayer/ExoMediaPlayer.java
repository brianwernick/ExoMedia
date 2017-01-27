/*
 * Copyright (C) 2016 Brian Wernick,
 * Copyright (C) 2015 The Android Open Source Project
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

package com.devbrackets.android.exomedia.core.exoplayer;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.PlaybackParams;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import android.support.v4.util.ArrayMap;
import android.util.Log;
import android.view.Surface;

import com.devbrackets.android.exomedia.ExoMedia.RendererType;
import com.devbrackets.android.exomedia.core.listener.CaptionListener;
import com.devbrackets.android.exomedia.core.listener.ExoPlayerListener;
import com.devbrackets.android.exomedia.core.listener.InternalErrorListener;
import com.devbrackets.android.exomedia.core.listener.MetadataListener;
import com.devbrackets.android.exomedia.core.renderer.RendererProvider;
import com.devbrackets.android.exomedia.core.source.MediaSourceProvider;
import com.devbrackets.android.exomedia.listener.OnBufferUpdateListener;
import com.devbrackets.android.exomedia.util.Repeater;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.audio.AudioCapabilities;
import com.google.android.exoplayer2.audio.AudioCapabilitiesReceiver;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.audio.AudioTrack;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.drm.StreamingDrmSessionManager;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.MetadataRenderer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.TextRenderer;
import com.google.android.exoplayer2.trackselection.AdaptiveVideoTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.FixedTrackSelection;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unused")
public class ExoMediaPlayer implements ExoPlayer.EventListener {
    private static final String TAG = "EMExoPlayer";
    private static final int BUFFER_REPEAT_DELAY = 1_000;

    @NonNull
    private final Context context;
    @NonNull
    private final ExoPlayer player;
    @NonNull
    private final DefaultTrackSelector trackSelector;
    @NonNull
    private final AdaptiveVideoTrackSelection.Factory adaptiveTrackSelectionFactory;
    @NonNull
    private final Handler mainHandler;
    @NonNull
    private final CopyOnWriteArrayList<ExoPlayerListener> listeners = new CopyOnWriteArrayList<>();

    @NonNull
    private final AtomicBoolean stopped = new AtomicBoolean();
    private boolean prepared = false;

    @NonNull
    private StateStore stateStore = new StateStore();
    @NonNull
    private Repeater bufferRepeater = new Repeater();

    @Nullable
    private Surface surface;
    @Nullable
    private MediaSource mediaSource;
    @NonNull
    private List<Renderer> renderers = new LinkedList<>();

    @Nullable
    private AudioCapabilities audioCapabilities;
    @Nullable
    private AudioCapabilitiesReceiver audioCapabilitiesReceiver;

    @Nullable
    private CaptionListener captionListener;
    @Nullable
    private MetadataListener metadataListener;
    @Nullable
    private InternalErrorListener internalErrorListener;
    @Nullable
    private OnBufferUpdateListener bufferUpdateListener;

    @Nullable
    private PowerManager.WakeLock wakeLock = null;


    @NonNull
    private CapabilitiesListener capabilitiesListener = new CapabilitiesListener();
    private int audioSessionId = AudioTrack.SESSION_ID_NOT_SET;

    public ExoMediaPlayer(@NonNull Context context) {
        this.context = context;

        bufferRepeater.setRepeaterDelay(BUFFER_REPEAT_DELAY);
        bufferRepeater.setRepeatListener(new BufferRepeatListener());

        mainHandler = new Handler();

        ComponentListener componentListener = new ComponentListener();
        RendererProvider rendererProvider = new RendererProvider(context, mainHandler, componentListener, componentListener, componentListener, componentListener);

        renderers = rendererProvider.generate();

        adaptiveTrackSelectionFactory = new AdaptiveVideoTrackSelection.Factory(new DefaultBandwidthMeter());
        trackSelector = new DefaultTrackSelector(adaptiveTrackSelectionFactory);

        player = ExoPlayerFactory.newInstance(renderers.toArray(new Renderer[renderers.size()]), trackSelector, new DefaultLoadControl());
        player.addListener(this);
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {
        // Purposefully left blank
    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        // Purposefully left blank
    }

    @Override
    public void onLoadingChanged(boolean isLoading) {
        // Purposefully left blank
    }

    @Override
    public void onPositionDiscontinuity() {
        // Purposefully left blank
    }

    public void setUri(@Nullable Uri uri) {
        setMediaSource(uri != null ? new MediaSourceProvider().generate(context, mainHandler, uri) : null);
    }

    public void setMediaSource(@Nullable MediaSource source) {
        this.mediaSource = source;

        // If we have something to play then create a new receiver
        if (mediaSource != null && audioCapabilitiesReceiver == null) {
            audioCapabilitiesReceiver = new AudioCapabilitiesReceiver(context, capabilitiesListener);
            audioCapabilitiesReceiver.register();
        }

        prepared = false;
        prepare();
    }

    public void addListener(ExoPlayerListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(ExoPlayerListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public void setBufferUpdateListener(@Nullable OnBufferUpdateListener listener) {
        this.bufferUpdateListener = listener;
        setBufferRepeaterStarted(listener != null);
    }

    public void setInternalErrorListener(@Nullable InternalErrorListener listener) {
        internalErrorListener = listener;
    }

    public void setCaptionListener(@Nullable CaptionListener listener) {
        captionListener = listener;
    }

    public void setMetadataListener(@Nullable MetadataListener listener) {
        metadataListener = listener;
    }

    public void setSurface(@Nullable Surface surface) {
        this.surface = surface;
        sendMessage(C.TRACK_TYPE_VIDEO, C.MSG_SET_SURFACE, surface, false);
    }

    @Nullable
    public Surface getSurface() {
        return surface;
    }

    public void blockingClearSurface() {
        if (surface != null) {
            surface.release();
        }

        surface = null;
        sendMessage(C.TRACK_TYPE_VIDEO, C.MSG_SET_SURFACE, null, true);
    }

    @Nullable
    public AudioCapabilities getAudioCapabilities() {
        return audioCapabilities;
    }

    /**
     * Retrieves a list of available tracks
     *
     * @return A list of available tracks associated with each type
     */
    @Nullable
    public Map<RendererType, TrackGroupArray> getAvailableTracks() {
        if (getPlaybackState() == ExoPlayer.STATE_IDLE) {
            return null;
        }

        // Retrieves the available tracks
        Map<RendererType, TrackGroupArray> trackMap = new ArrayMap<>();
        MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
        if (mappedTrackInfo == null) {
            return trackMap;
        }

        // Maps the available tracks
        RendererType[] types = new RendererType[] {RendererType.AUDIO, RendererType.VIDEO, RendererType.CLOSED_CAPTION, RendererType.METADATA};
        for (RendererType type : types) {
            int exoPlayerTrackIndex = getExoPlayerTrackType(type);
            if (mappedTrackInfo.length > exoPlayerTrackIndex) {
                trackMap.put(type, mappedTrackInfo.getTrackGroups(exoPlayerTrackIndex));
            }
        }

        return trackMap;
    }

    //todo we should have a special case for auto / disabled / and unknown (e.g. -10, -5, -1)
    public int getSelectedTrackIndex(@NonNull RendererType type) {
        // Retrieves the available tracks
        int exoPlayerTrackIndex = getExoPlayerTrackType(type);
        MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
        TrackGroupArray trackGroupArray = mappedTrackInfo == null ? null : mappedTrackInfo.getTrackGroups(exoPlayerTrackIndex);
        if (trackGroupArray == null || trackGroupArray.length == 0) {
            return -1;
        }

        // Verifies the track selection has been overridden
        MappingTrackSelector.SelectionOverride selectionOverride = trackSelector.getSelectionOverride(exoPlayerTrackIndex, trackGroupArray);
        if (selectionOverride == null || selectionOverride.groupIndex != exoPlayerTrackIndex || selectionOverride.length <= 0) {
            return -1;
        }

        //TODO: Do we only allow 1 selected track? (ExoPlayer supports multiple with the adaptive streams)
        return selectionOverride.tracks[0];
    }

    // TODO: pay attention to auto / disabled / unknown indexes
    public void setSelectedTrack(@NonNull RendererType type, int index) {
        // Retrieves the available tracks
        int exoPlayerTrackIndex = getExoPlayerTrackType(type);
        MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
        TrackGroupArray trackGroupArray = mappedTrackInfo == null ? null : mappedTrackInfo.getTrackGroups(exoPlayerTrackIndex);
        if (trackGroupArray == null || trackGroupArray.length == 0) {
            return;
        }

        // Creates the track selection override
        int[] tracks = new int[] {index};
        TrackSelection.Factory factory = tracks.length == 1 ? new FixedTrackSelection.Factory() : adaptiveTrackSelectionFactory;
        MappingTrackSelector.SelectionOverride selectionOverride = new MappingTrackSelector.SelectionOverride(factory, exoPlayerTrackIndex, tracks);

        // Specifies the correct track to use
        trackSelector.setSelectionOverride(exoPlayerTrackIndex, trackGroupArray, selectionOverride);
    }

    public void setVolume(@FloatRange(from = 0.0, to = 1.0) float volume) {
        sendMessage(C.TRACK_TYPE_AUDIO, C.MSG_SET_VOLUME, volume);
    }

    public void forcePrepare() {
        prepared = false;
    }

    public void prepare() {
        if (prepared || mediaSource == null) {
            return;
        }

        if (!renderers.isEmpty()) {
            player.stop();
        }

        reportPlayerState();

        player.prepare(mediaSource);
        prepared = true;

        stopped.set(false);
    }

    public void stop() {
        if (!stopped.getAndSet(true)) {
            player.setPlayWhenReady(false);
            player.stop();
        }
    }

    public void setPlayWhenReady(boolean playWhenReady) {
        player.setPlayWhenReady(playWhenReady);
        stayAwake(playWhenReady);
    }

    public void seekTo(long positionMs) {
        player.seekTo(positionMs);
        stateStore.setMostRecentState(stateStore.isLastReportedPlayWhenReady(), StateStore.STATE_SEEKING);
    }

    /**
     * Seeks to the beginning of the media, and plays it. This method will not succeed if playback state is not {@code ExoPlayer.STATE_IDLE} or {@code ExoPlayer.STATE_ENDED}.
     *
     * @return {@code true} if the media was successfully restarted, otherwise {@code false}
     */
    public boolean restart() {
        int playbackState = getPlaybackState();
        if (playbackState != ExoPlayer.STATE_IDLE && playbackState != ExoPlayer.STATE_ENDED) {
            return false;
        }

        seekTo(0);
        setPlayWhenReady(true);

        forcePrepare();
        prepare();

        return true;
    }

    public void release() {
        if (audioCapabilitiesReceiver != null) {
            audioCapabilitiesReceiver.unregister();
            audioCapabilitiesReceiver = null;
        }

        setBufferRepeaterStarted(false);
        listeners.clear();

        surface = null;
        player.release();
        stayAwake(false);
    }

    public int getPlaybackState() {
        return player.getPlaybackState();
    }

    public int getAudioSessionId() {
        return audioSessionId;
    }

    public boolean setPlaybackSpeed(float speed) {
        // Marshmallow+ support setting the playback speed natively
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PlaybackParams params = new PlaybackParams();
            params.setSpeed(speed);

            sendMessage(C.TRACK_TYPE_AUDIO, C.MSG_SET_PLAYBACK_PARAMS, params);
            return true;
        }

        //TODO: backwards compatibility for playback speed (EMExoPlayer)
        return false;
    }

    public long getCurrentPosition() {
        return player.getCurrentPosition();
    }

    public long getDuration() {
        return player.getDuration();
    }

    public int getBufferedPercentage() {
        return player.getBufferedPercentage();
    }

    public boolean getPlayWhenReady() {
        return player.getPlayWhenReady();
    }

    /**
     * This function has the MediaPlayer access the low-level power manager
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
        boolean wasHeld = false;
        if (wakeLock != null) {
            if (wakeLock.isHeld()) {
                wasHeld = true;
                wakeLock.release();
            }

            wakeLock = null;
        }

        //Acquires the wakelock if we have permissions to
        if (context.getPackageManager().checkPermission(Manifest.permission.WAKE_LOCK, context.getPackageName()) == PackageManager.PERMISSION_GRANTED) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(mode | PowerManager.ON_AFTER_RELEASE, ExoMediaPlayer.class.getName());
            wakeLock.setReferenceCounted(false);
        } else {
            Log.w(TAG, "Unable to acquire WAKE_LOCK due to missing manifest permission");
        }

        stayAwake(wasHeld);
    }

    protected int getExoPlayerTrackType(@NonNull RendererType type) {
        switch (type) {
            case AUDIO:
                return C.TRACK_TYPE_AUDIO;
            case VIDEO:
                return C.TRACK_TYPE_VIDEO;
            case CLOSED_CAPTION:
                return C.TRACK_TYPE_TEXT;
            case METADATA:
                return C.TRACK_TYPE_METADATA;
        }

        return C.TRACK_TYPE_UNKNOWN;
    }

    protected void sendMessage(int renderType, int messageType, Object message) {
        sendMessage(renderType, messageType, message, false);
    }

    protected void sendMessage(int renderType, int messageType, Object message, boolean blocking) {
        if (renderers.isEmpty()) {
            return;
        }

        List<ExoPlayer.ExoPlayerMessage> messages = new ArrayList<>();
        for (Renderer renderer : renderers) {
            if (renderer.getTrackType() == renderType) {
                messages.add(new ExoPlayer.ExoPlayerMessage(renderer, messageType, message));
            }
        }

        if (blocking) {
            player.blockingSendMessages(messages.toArray(new ExoPlayer.ExoPlayerMessage[messages.size()]));
        } else {
            player.sendMessages(messages.toArray(new ExoPlayer.ExoPlayerMessage[messages.size()]));
        }
    }

    /**
     * Used with playback state changes to correctly acquire and
     * release the wakelock if the user has enabled it with {@link #setWakeMode(Context, int)}.
     * If the {@link #wakeLock} is null then no action will be performed.
     *
     * @param awake True if the wakelock should be acquired
     */
    protected void stayAwake(boolean awake) {
        if (wakeLock == null) {
            return;
        }

        if (awake && !wakeLock.isHeld()) {
            wakeLock.acquire();
        } else if (!awake && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int state) {
        reportPlayerState();
    }

    @Override
    public void onPlayerError(ExoPlaybackException exception) {
        for (ExoPlayerListener listener : listeners) {
            listener.onError(this, exception);
        }
    }

    private void reportPlayerState() {
        boolean playWhenReady = player.getPlayWhenReady();
        int playbackState = getPlaybackState();

        int newState = stateStore.getState(playWhenReady, playbackState);
        if (newState != stateStore.getMostRecentState()) {
            stateStore.setMostRecentState(playWhenReady, playbackState);

            //Makes sure the buffering notifications are sent
            if (newState == ExoPlayer.STATE_READY) {
                setBufferRepeaterStarted(true);
            } else if (newState == ExoPlayer.STATE_IDLE || newState == ExoPlayer.STATE_ENDED) {
                setBufferRepeaterStarted(false);
            }

            //Because the playWhenReady isn't a state in itself, rather a flag to a state we will ignore informing of
            // see events when that is the only change.  Additionally, on some devices we get states ordered as
            // [seeking, ready, buffering, ready] while on others we get [seeking, buffering, ready]
            boolean informSeekCompletion = stateStore.matchesHistory(new int[]{StateStore.STATE_SEEKING, ExoPlayer.STATE_READY}, true);
            informSeekCompletion |= stateStore.matchesHistory(new int[]{StateStore.STATE_SEEKING, ExoPlayer.STATE_BUFFERING, ExoPlayer.STATE_READY}, true);
            informSeekCompletion |= stateStore.matchesHistory(new int[]{StateStore.STATE_SEEKING, ExoPlayer.STATE_READY, ExoPlayer.STATE_BUFFERING, ExoPlayer.STATE_READY}, true);

            for (ExoPlayerListener listener : listeners) {
                listener.onStateChanged(playWhenReady, playbackState);

                if (informSeekCompletion) {
                    listener.onSeekComplete();
                }
            }
        }
    }

    private void setBufferRepeaterStarted(boolean start) {
        if (start && bufferUpdateListener != null) {
            bufferRepeater.start();
        } else {
            bufferRepeater.stop();
        }
    }

    private class BufferRepeatListener implements Repeater.RepeatListener {
        @Override
        public void onRepeat() {
            if (bufferUpdateListener != null) {
                bufferUpdateListener.onBufferingUpdate(getBufferedPercentage());
            }
        }
    }

    private static class StateStore {
        public static final int FLAG_PLAY_WHEN_READY = 0xF0000000;
        public static final int STATE_SEEKING = 100;

        //We keep the last few states because that is all we need currently
        private int[] prevStates = new int[]{ExoPlayer.STATE_IDLE, ExoPlayer.STATE_IDLE, ExoPlayer.STATE_IDLE, ExoPlayer.STATE_IDLE};

        public void setMostRecentState(boolean playWhenReady, int state) {
            int newState = getState(playWhenReady, state);
            if (prevStates[3] == newState) {
                return;
            }

            prevStates[0] = prevStates[1];
            prevStates[1] = prevStates[2];
            prevStates[2] = prevStates[3];
            prevStates[3] = state;
        }

        public int getState(boolean playWhenReady, int state) {
            return state | (playWhenReady ? FLAG_PLAY_WHEN_READY : 0);
        }

        public int getMostRecentState() {
            return prevStates[3];
        }

        public boolean isLastReportedPlayWhenReady() {
            return (prevStates[3] & FLAG_PLAY_WHEN_READY) != 0;
        }

        public boolean matchesHistory(@Size(min = 1, max = 4) int[] states, boolean ignorePlayWhenReady) {
            boolean flag = true;
            int andFlag = ignorePlayWhenReady ? ~FLAG_PLAY_WHEN_READY : ~0x0;
            int startIndex = prevStates.length - states.length;

            for (int i = startIndex; i < prevStates.length; i++) {
                flag &= (prevStates[i] & andFlag) == (states[i - startIndex] & andFlag);
            }

            return flag;
        }
    }

    private class CapabilitiesListener implements
            AudioCapabilitiesReceiver.Listener,
            StreamingDrmSessionManager.EventListener {

        @Override
        public void onAudioCapabilitiesChanged(AudioCapabilities capabilities) {
            if (capabilities.equals(audioCapabilities)) {
                return;
            }

            audioCapabilities = capabilities;
            if (mediaSource == null) {
                return;
            }

            //Restarts the media playback to allow the RenderBuilder to handle the audio channel determination
            boolean playWhenReady = getPlayWhenReady();
            long currentPosition = getCurrentPosition();

            //TODO: is this still necessary? or does the ExoPlayer now already handle this?
            // I think we need to re-build the renderers (as they take the current audio capabilities) and push those in to the ExoPlayer
            setMediaSource(mediaSource);

            player.seekTo(currentPosition);
            player.setPlayWhenReady(playWhenReady);
        }

        @Override
        public void onDrmKeysLoaded() {
            // Purposefully left blank
        }

        @Override
        public void onDrmSessionManagerError(Exception e) {
            if (internalErrorListener != null) {
                internalErrorListener.onDrmSessionManagerError(e);
            }
        }
    }

    private class ComponentListener implements
            VideoRendererEventListener,
            AudioRendererEventListener,
            TextRenderer.Output,
            MetadataRenderer.Output {

        @Override
        public void onAudioEnabled(DecoderCounters counters) {

        }

        @Override
        public void onAudioDisabled(DecoderCounters counters) {
            audioSessionId = AudioTrack.SESSION_ID_NOT_SET;
        }

        @Override
        public void onAudioSessionId(int sessionId) {
            audioSessionId = sessionId;
        }

        @Override
        public void onAudioDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs) {
            // Purposefully left blank
        }

        @Override
        public void onAudioInputFormatChanged(Format format) {
            // Purposefully left blank
        }

        @Override
        public void onAudioTrackUnderrun(int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {
            if (internalErrorListener != null) {
                internalErrorListener.onAudioTrackUnderrun(bufferSize, bufferSizeMs, elapsedSinceLastFeedMs);
            }
        }

        @Override
        public void onVideoEnabled(DecoderCounters counters) {

        }

        @Override
        public void onVideoDisabled(DecoderCounters counters) {

        }

        @Override
        public void onVideoDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs) {
            // Purposefully left blank
        }

        @Override
        public void onVideoInputFormatChanged(Format format) {
            // Purposefully left blank
        }

        @Override
        public void onDroppedFrames(int count, long elapsedMs) {

        }

        @Override
        public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
            for (ExoPlayerListener listener : listeners) {
                listener.onVideoSizeChanged(width, height, unappliedRotationDegrees, pixelWidthHeightRatio);
            }
        }

        @Override
        public void onRenderedFirstFrame(Surface surface) {

        }

        @Override
        public void onMetadata(Metadata metadata) {
            if (metadataListener != null) {
                metadataListener.onMetadata(metadata);
            }
        }

        @Override
        public void onCues(List<Cue> cues) {
            if (captionListener != null) {
                captionListener.onCues(cues);
            }
        }
    }
}