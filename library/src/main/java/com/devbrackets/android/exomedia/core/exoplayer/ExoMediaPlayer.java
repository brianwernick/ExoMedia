/*
 * Copyright (C) 2015-2017 Brian Wernick,
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

import com.devbrackets.android.exomedia.ExoMedia;
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
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.ExoMediaDrm;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.drm.FrameworkMediaDrm;
import com.google.android.exoplayer2.drm.MediaDrmCallback;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.MetadataRenderer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.TextRenderer;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.FixedTrackSelection;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings({"unused", "WeakerAccess"})
public class ExoMediaPlayer implements Player.EventListener {
    private static final String TAG = "ExoMediaPlayer";
    private static final int BUFFER_REPEAT_DELAY = 1_000;
    private static final int WAKE_LOCK_TIMEOUT = 1_000;

    @NonNull
    private final Context context;
    @NonNull
    private final ExoPlayer player;
    @NonNull
    private final DefaultTrackSelector trackSelector;
    @NonNull
    private final AdaptiveTrackSelection.Factory adaptiveTrackSelectionFactory;
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
    private MediaDrmCallback drmCallback;
    @Nullable
    private MediaSource mediaSource;
    @NonNull
    private List<Renderer> renderers = new LinkedList<>();
    @NonNull
    private MediaSourceProvider mediaSourceProvider = new MediaSourceProvider();
    @NonNull
    private DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();

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
    private int audioSessionId = C.AUDIO_SESSION_ID_UNSET;

    public ExoMediaPlayer(@NonNull Context context) {
        this.context = context;

        bufferRepeater.setRepeaterDelay(BUFFER_REPEAT_DELAY);
        bufferRepeater.setRepeatListener(new BufferRepeatListener());

        mainHandler = new Handler();

        ComponentListener componentListener = new ComponentListener();
        RendererProvider rendererProvider = new RendererProvider(context, mainHandler, componentListener, componentListener, componentListener, componentListener);
        rendererProvider.setDrmSessionManager(generateDrmSessionManager());

        renderers = rendererProvider.generate();

        adaptiveTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        trackSelector = new DefaultTrackSelector(adaptiveTrackSelectionFactory);

        LoadControl loadControl = ExoMedia.Data.loadControl != null ? ExoMedia.Data.loadControl : new DefaultLoadControl();
        player = ExoPlayerFactory.newInstance(renderers.toArray(new Renderer[renderers.size()]), trackSelector, loadControl);
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
    public void onRepeatModeChanged(int repeatMode) {
        // Purposefully left blank
    }

    @Override
    public void onPositionDiscontinuity() {
        // Purposefully left blank
    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
        // Purposefully left blank
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

    /**
     * Sets the {@link MediaDrmCallback} to use when handling DRM for media.
     * This should be called before specifying the videos uri or path
     * <br>
     * <b>NOTE:</b> DRM is only supported on API 18 +
     *
     * @param drmCallback The callback to use when handling DRM media
     */
    public void setDrmCallback(@Nullable MediaDrmCallback drmCallback) {
        this.drmCallback = drmCallback;
    }

    public void setUri(@Nullable Uri uri) {
        setMediaSource(uri != null ? mediaSourceProvider.generate(context, mainHandler, uri, bandwidthMeter) : null);
    }

    public void setMediaSource(@Nullable MediaSource source) {
        this.mediaSource = source;

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

        return selectionOverride.tracks[0];
    }

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

    public void setAudioStreamType(int streamType) {
        @C.AudioUsage
        int usage = Util.getAudioUsageForStreamType(streamType);
        @C.AudioContentType
        int contentType = Util.getAudioContentTypeForStreamType(streamType);

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(usage)
                .setContentType(contentType)
                .build();

        sendMessage(C.TRACK_TYPE_AUDIO, C.MSG_SET_AUDIO_ATTRIBUTES, audioAttributes);
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

        stateStore.reset();
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
        PlaybackParameters params = new PlaybackParameters(speed, 1.0f);
        player.setPlaybackParameters(params);

        return true;
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
            wakeLock.acquire(WAKE_LOCK_TIMEOUT);
        } else if (!awake && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    /**
     * Generates the {@link DrmSessionManager} to use with the {@link RendererProvider}. This will
     * return null on API's &lt; {@value Build.VERSION_CODES#JELLY_BEAN_MR2}
     *
     * @return The {@link DrmSessionManager} to use or <code>null</code>
     */
    @Nullable
    protected DrmSessionManager<FrameworkMediaCrypto> generateDrmSessionManager() {
        // DRM is only supported on API 18 + in the ExoPlayer
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return null;
        }

        // Widevine will capture the majority of use cases however playready is supported on all AndroidTV devices
        UUID uuid = C.WIDEVINE_UUID;

        try {
            return new DefaultDrmSessionManager<>(uuid, FrameworkMediaDrm.newInstance(uuid), new DelegatedMediaDrmCallback(), null, mainHandler, capabilitiesListener);
        } catch (Exception e) {
            Log.d(TAG, "Unable to create a DrmSessionManager due to an exception", e);
            return null;
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
            boolean informSeekCompletion = stateStore.matchesHistory(new int[]{StateStore.STATE_SEEKING, ExoPlayer.STATE_BUFFERING, ExoPlayer.STATE_READY}, true);
            informSeekCompletion |= stateStore.matchesHistory(new int[] {ExoPlayer.STATE_BUFFERING, StateStore.STATE_SEEKING, ExoPlayer.STATE_READY}, true);
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

    private static class StateStore {
        public static final int FLAG_PLAY_WHEN_READY = 0xF0000000;
        public static final int STATE_SEEKING = 100;

        //We keep the last few states because that is all we need currently
        private int[] prevStates = new int[]{ExoPlayer.STATE_IDLE, ExoPlayer.STATE_IDLE, ExoPlayer.STATE_IDLE, ExoPlayer.STATE_IDLE};

        public void reset() {
            for (int i = 0; i < prevStates.length; i++) {
                prevStates[i] = ExoPlayer.STATE_IDLE;
            }
        }

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

    private class BufferRepeatListener implements Repeater.RepeatListener {
        @Override
        public void onRepeat() {
            if (bufferUpdateListener != null) {
                bufferUpdateListener.onBufferingUpdate(getBufferedPercentage());
            }
        }
    }

    /**
     * Delegates the {@link #drmCallback} so that we don't need to re-initialize the renderers
     * when the callback is set.
     */
    private class DelegatedMediaDrmCallback implements MediaDrmCallback {
        @Override
        public byte[] executeProvisionRequest(UUID uuid, ExoMediaDrm.ProvisionRequest request) throws Exception {
            return drmCallback != null ? drmCallback.executeProvisionRequest(uuid, request) : new byte[0];
        }

        @Override
        public byte[] executeKeyRequest(UUID uuid, ExoMediaDrm.KeyRequest request) throws Exception {
            return drmCallback != null ? drmCallback.executeKeyRequest(uuid, request) : new byte[0];
        }
    }

    private class CapabilitiesListener implements DefaultDrmSessionManager.EventListener {
        @Override
        public void onDrmKeysLoaded() {
            // Purposefully left blank
        }

        @Override
        public void onDrmKeysRestored() {
            // Purposefully left blank
        }

        @Override
        public void onDrmKeysRemoved() {
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
            // Purposefully left blank
        }

        @Override
        public void onAudioDisabled(DecoderCounters counters) {
            audioSessionId = C.AUDIO_SESSION_ID_UNSET;
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
            // Purposefully left blank
        }

        @Override
        public void onVideoDisabled(DecoderCounters counters) {
            // Purposefully left blank
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
            // Purposefully left blank
        }

        @Override
        public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
            for (ExoPlayerListener listener : listeners) {
                listener.onVideoSizeChanged(width, height, unappliedRotationDegrees, pixelWidthHeightRatio);
            }
        }

        @Override
        public void onRenderedFirstFrame(Surface surface) {
            // Purposefully left blank
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