/*
 * Copyright (C) 2015-2018 ExoMedia Contributors,
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
import com.google.android.exoplayer2.PlayerMessage;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.analytics.AnalyticsCollector;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.drm.DefaultDrmSessionEventListener;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.ExoMediaDrm;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.drm.FrameworkMediaDrm;
import com.google.android.exoplayer2.drm.MediaDrmCallback;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.MetadataOutput;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.TextOutput;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.util.Clock;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings({"unused", "WeakerAccess"})
public class ExoMediaPlayer extends Player.DefaultEventListener {
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
    private List<Renderer> renderers;
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

    @FloatRange(from = 0.0, to = 1.0)
    protected float requestedVolume = 1.0f;

    @NonNull
    private AnalyticsCollector analyticsCollector;

    public ExoMediaPlayer(@NonNull Context context) {
        this.context = context;

        bufferRepeater.setRepeaterDelay(BUFFER_REPEAT_DELAY);
        bufferRepeater.setRepeatListener(new BufferRepeatListener());

        mainHandler = new Handler();

        ComponentListener componentListener = new ComponentListener();
        RendererProvider rendererProvider = new RendererProvider(context, mainHandler, componentListener, componentListener, componentListener, componentListener);
        DrmSessionManager<FrameworkMediaCrypto> drmSessionManager = generateDrmSessionManager();
        rendererProvider.setDrmSessionManager(drmSessionManager);

        renderers = rendererProvider.generate();

        adaptiveTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        trackSelector = new DefaultTrackSelector(adaptiveTrackSelectionFactory);

        LoadControl loadControl = ExoMedia.Data.loadControl != null ? ExoMedia.Data.loadControl : new DefaultLoadControl();
        player = ExoPlayerFactory.newInstance(renderers.toArray(new Renderer[renderers.size()]), trackSelector, loadControl);
        player.addListener(this);
        analyticsCollector = new AnalyticsCollector.Factory().createAnalyticsCollector(player, Clock.DEFAULT);
        player.addListener(analyticsCollector);
        setupDamSessionManagerAnalytics(drmSessionManager);
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
        setMediaSource(uri != null ? ExoMedia.Data.mediaSourceProvider.generate(context, mainHandler, uri, bandwidthMeter) : null);
    }

    public void setMediaSource(@Nullable MediaSource source) {
        if (this.mediaSource != null) {
            this.mediaSource.removeEventListener(analyticsCollector);
            analyticsCollector.resetForNewMediaSource();
        }
        if (source != null) {
            source.addEventListener(mainHandler, analyticsCollector);
        }
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

    @NonNull
    public ExoPlayer getExoPlayer() {
        return player;
    }

    @NonNull
    public BandwidthMeter getBandwidthMeter() {
        return bandwidthMeter;
    }

    /**
     * Returns the {@link AnalyticsCollector} used for collecting analytics events.
     */
    @NonNull
    public AnalyticsCollector getAnalyticsCollector() {
        return analyticsCollector;
    }

    /**
     * Adds an {@link AnalyticsListener} to receive analytics events.
     *
     * @param listener The listener to be added.
     */
    public void addAnalyticsListener(AnalyticsListener listener) {
        analyticsCollector.addListener(listener);
    }

    /**
     * Removes an {@link AnalyticsListener}.
     *
     * @param listener The listener to be removed.
     */
    public void removeAnalyticsListener(AnalyticsListener listener) {
        analyticsCollector.removeListener(listener);
    }

    public void clearSurface() {
        if (surface != null) {
            surface.release();
        }

        surface = null;
        sendMessage(C.TRACK_TYPE_VIDEO, C.MSG_SET_SURFACE, null, false);
    }

    /**
     * @deprecated use {@link #clearSurface()} as this is no longer blocking
     */
    @Deprecated
    public void blockingClearSurface() {
        clearSurface();
    }

    /**
     * Retrieves a list of available tracks
     *
     * @return A list of available tracks associated with each type
     */
    @Nullable
    public Map<RendererType, TrackGroupArray> getAvailableTracks() {
        if (getPlaybackState() == Player.STATE_IDLE) {
            return null;
        }

        // Retrieves the available tracks
        Map<RendererType, TrackGroupArray> trackMap = new ArrayMap<>();
        MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
        if (mappedTrackInfo == null) {
            return trackMap;
        }

        // Maps the available tracks
        RendererType[] types = new RendererType[]{RendererType.AUDIO, RendererType.VIDEO, RendererType.CLOSED_CAPTION, RendererType.METADATA};
        for (RendererType type : types) {
            List<TrackGroup> trackGroups = new ArrayList<>();
            // collect track groups from all the track renderers of the same type
            for (Integer exoPlayerTrackIndex : getExoPlayerTracksInfo(type, 0, mappedTrackInfo).rendererTrackIndexes) {
                TrackGroupArray trackGroupArray = mappedTrackInfo.getTrackGroups(exoPlayerTrackIndex);
                for (int i = 0; i < trackGroupArray.length; i++) {
                    trackGroups.add(trackGroupArray.get(i));
                }
            }
            if (!trackGroups.isEmpty()) {
                // construct fake track group array for track groups from all the renderers of the same type
                trackMap.put(type, new TrackGroupArray(trackGroups.toArray(new TrackGroup[trackGroups.size()])));
            }
        }

        return trackMap;
    }

    /**
     * @deprecated Use {@link #getSelectedTrackIndex(RendererType, int)}
     */
    public int getSelectedTrackIndex(@NonNull RendererType type) {
        return getSelectedTrackIndex(type, 0);
    }

    public int getSelectedTrackIndex(@NonNull RendererType type, int groupIndex) {
        // Retrieves the available tracks
        MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
        ExoPlayerRendererTracksInfo tracksInfo = getExoPlayerTracksInfo(type, groupIndex, mappedTrackInfo);
        TrackGroupArray trackGroupArray = tracksInfo.rendererTrackIndex == C.INDEX_UNSET ?
                null : mappedTrackInfo.getTrackGroups(tracksInfo.rendererTrackIndex);
        if (trackGroupArray == null || trackGroupArray.length == 0) {
            return -1;
        }

        // Verifies the track selection has been overridden
        DefaultTrackSelector.SelectionOverride selectionOverride = trackSelector.getParameters().getSelectionOverride(tracksInfo.rendererTrackIndex, trackGroupArray);
        if (selectionOverride == null || selectionOverride.groupIndex != tracksInfo.rendererTrackGroupIndex || selectionOverride.length <= 0) {
            return -1;
        }

        return selectionOverride.tracks[0]; // In the current implementation only one track can be selected at a time so get the first one.
    }

    /**
     * @deprecated Use {@link #setSelectedTrack(RendererType, int, int)}
     */
    @Deprecated
    public void setSelectedTrack(@NonNull RendererType type, int index) {
        setSelectedTrack(type, 0, index);
    }

    public void setSelectedTrack(@NonNull RendererType type, int groupIndex, int trackIndex) {
        // Retrieves the available tracks
        MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
        ExoPlayerRendererTracksInfo tracksInfo = getExoPlayerTracksInfo(type, groupIndex, mappedTrackInfo);
        TrackGroupArray trackGroupArray = tracksInfo.rendererTrackIndex == C.INDEX_UNSET || mappedTrackInfo == null ?
                null : mappedTrackInfo.getTrackGroups(tracksInfo.rendererTrackIndex);
        if (trackGroupArray == null || trackGroupArray.length == 0 || trackGroupArray.length <= tracksInfo.rendererTrackGroupIndex) {
            return;
        }

        // Finds the requested group
        TrackGroup group = trackGroupArray.get(tracksInfo.rendererTrackGroupIndex);
        if (group == null || group.length <= trackIndex) {
            return;
        }

        DefaultTrackSelector.ParametersBuilder parametersBuilder = trackSelector.buildUponParameters();
        for (int rendererTrackIndex : tracksInfo.rendererTrackIndexes) {
            parametersBuilder.clearSelectionOverrides(rendererTrackIndex);
            if (tracksInfo.rendererTrackIndex == rendererTrackIndex) {
                // Specifies the correct track to use
                parametersBuilder.setSelectionOverride(rendererTrackIndex, trackGroupArray,
                        new DefaultTrackSelector.SelectionOverride(tracksInfo.rendererTrackGroupIndex, trackIndex));
                // make sure renderer is enabled
                parametersBuilder.setRendererDisabled(rendererTrackIndex, false);
            } else {
                // disable other renderers of the same type to avoid playback errors
                parametersBuilder.setRendererDisabled(rendererTrackIndex, true);
            }
        }
        trackSelector.setParameters(parametersBuilder);
    }

    /**
     * Clear all selected tracks for the specified renderer and re-enable all renderers so the player can select the default track.
     *
     * @param type The renderer type
     */
    public void clearSelectedTracks(@NonNull RendererType type) {
        // Retrieves the available tracks
        MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
        ExoPlayerRendererTracksInfo tracksInfo = getExoPlayerTracksInfo(type, 0, mappedTrackInfo);
        DefaultTrackSelector.ParametersBuilder parametersBuilder = trackSelector.buildUponParameters();

        for (int rendererTrackIndex : tracksInfo.rendererTrackIndexes) {
            // Reset all renderers re-enabling so the player can select the streams default track.
            parametersBuilder.setRendererDisabled(rendererTrackIndex, false)
                    .clearSelectionOverrides(rendererTrackIndex);
        }
        trackSelector.setParameters(parametersBuilder);
    }

    public void setRendererEnabled(@NonNull RendererType type, boolean enabled) {
        MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
        ExoPlayerRendererTracksInfo tracksInfo = getExoPlayerTracksInfo(type, 0, mappedTrackInfo);
        if (!tracksInfo.rendererTrackIndexes.isEmpty()) {
            boolean enabledSomething = false;
            DefaultTrackSelector.ParametersBuilder parametersBuilder = trackSelector.buildUponParameters();
            for (int rendererTrackIndex : tracksInfo.rendererTrackIndexes) {
                if (enabled) {
                    DefaultTrackSelector.SelectionOverride selectionOverride = trackSelector.getParameters().getSelectionOverride(rendererTrackIndex, mappedTrackInfo.getTrackGroups(rendererTrackIndex));
                    // check whether the renderer has been selected before
                    // other renderers should be kept disabled to avoid playback errors
                    if (selectionOverride != null) {
                        parametersBuilder.setRendererDisabled(rendererTrackIndex, false);
                        enabledSomething = true;
                    }
                } else {
                    parametersBuilder.setRendererDisabled(rendererTrackIndex, true);
                }
            }
            if (enabled && !enabledSomething) {
                // if nothing has been enabled enable the first sequential renderer
                parametersBuilder.setRendererDisabled(tracksInfo.rendererTrackIndexes.get(0), false);
            }
            trackSelector.setParameters(parametersBuilder);
        }
    }

    /**
     * Return true if at least one renderer for the given type is enabled
     * @param type The renderer type
     * @return true if at least one renderer for the given type is enabled
     */
    public boolean isRendererEnabled(@NonNull RendererType type) {
        MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
        ExoPlayerRendererTracksInfo tracksInfo = getExoPlayerTracksInfo(type, 0, mappedTrackInfo);
        DefaultTrackSelector.Parameters parameters = trackSelector.getParameters();
        for (Integer rendererTrackIndex : tracksInfo.rendererTrackIndexes) {
            if (!parameters.getRendererDisabled(rendererTrackIndex)) {
                return true;
            }
        }
        return false;
    }

    public void setVolume(@FloatRange(from = 0.0, to = 1.0) float volume) {
        requestedVolume = volume;
        sendMessage(C.TRACK_TYPE_AUDIO, C.MSG_SET_VOLUME, requestedVolume);
    }

    @FloatRange(from = 0.0, to = 1.0)
    public float getVolume() {
        return requestedVolume;
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
        seekTo(positionMs, false);
    }

    /**
     * TODO: Expose this
     * Seeks to the specified position in the media currently loaded specified by <code>positionMs</code>.
     * If <code>limitToCurrentWindow</code> is true then a seek won't be allowed to span across windows.
     * This should only be different if the media in playback has multiple windows (e.g. in the case of using a
     * <code>ConcatenatingMediaSource</code> with more than 1 source)
     *
     * @param positionMs           The position to seek to in the media
     * @param limitToCurrentWindow <code>true</code> to only seek in the current window
     */
    public void seekTo(long positionMs, boolean limitToCurrentWindow) {
        analyticsCollector.notifySeekStarted();
        if (limitToCurrentWindow) {
            player.seekTo(positionMs);
            stateStore.setMostRecentState(stateStore.isLastReportedPlayWhenReady(), StateStore.STATE_SEEKING);
            return;
        }

        // We seek to the position in the timeline (may be across windows)
        Timeline timeline = player.getCurrentTimeline();
        int windowCount = timeline.getWindowCount();

        long cumulativePositionMs = 0;
        Timeline.Window window = new Timeline.Window();

        for (int index = 0; index < windowCount; index++) {
            timeline.getWindow(index, window);

            long windowDurationMs = window.getDurationMs();
            if (cumulativePositionMs < positionMs && positionMs <= (cumulativePositionMs + windowDurationMs)) {
                player.seekTo(index, positionMs - cumulativePositionMs);
                stateStore.setMostRecentState(stateStore.isLastReportedPlayWhenReady(), StateStore.STATE_SEEKING);
                return;
            }

            cumulativePositionMs += windowDurationMs;
        }

        Log.e(TAG, "Unable to seek across windows, falling back to in-window seeking");
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
        if (playbackState != Player.STATE_IDLE && playbackState != Player.STATE_ENDED) {
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

        if (mediaSource != null) {
            mediaSource.removeEventListener(analyticsCollector);
        }

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

    public float getPlaybackSpeed() {
        PlaybackParameters params = player.getPlaybackParameters();
        return params.speed;
    }

    public long getCurrentPosition() {
        return getCurrentPosition(false);
    }

    /**
     * TODO: Expose this
     * Returns the position in the media. If <code>limitToCurrentWindow</code> is <code>true</code> then the position
     * in the current window will be returned, otherwise the total position across all windows will be returned.
     * These should only be different if the media in playback has multiple windows (e.g. in the case of using a
     * <code>ConcatenatingMediaSource</code> with more than 1 source)
     *
     * @param limitToCurrentWindow If <code>true</code> the position within the current window will be returned
     * @return The current position in the media
     */
    public long getCurrentPosition(boolean limitToCurrentWindow) {
        long positionInCurrentWindow = player.getCurrentPosition();
        if (limitToCurrentWindow) {
            return positionInCurrentWindow;
        }

        // TODO cache the total time at the start of each window (e.g. Map<WindowIndex, cumulativeStartTimeMs>)
        // Adds the preceding window durations
        Timeline timeline = player.getCurrentTimeline();
        int maxWindowIndex = Math.min(timeline.getWindowCount() - 1, player.getCurrentWindowIndex());

        long cumulativePositionMs = 0;
        Timeline.Window window = new Timeline.Window();

        for (int index = 0; index < maxWindowIndex; index++) {
            timeline.getWindow(index, window);
            cumulativePositionMs += window.getDurationMs();
        }

        return cumulativePositionMs + positionInCurrentWindow;
    }

    public long getDuration() {
        return player.getDuration();
    }

    public int getBufferedPercentage() {
        return player.getBufferedPercentage();
    }

    @Nullable
    public WindowInfo getWindowInfo() {
        Timeline timeline = player.getCurrentTimeline();
        if (timeline.isEmpty()) {
            return null;
        }

        int currentWindowIndex = player.getCurrentWindowIndex();
        Timeline.Window currentWindow = timeline.getWindow(currentWindowIndex, new Timeline.Window(), true);

        return new WindowInfo(
                player.getPreviousWindowIndex(),
                currentWindowIndex,
                player.getNextWindowIndex(),
                currentWindow
        );
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
     * @param mode    the power/wake mode to set
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
            if (pm != null) {
                wakeLock = pm.newWakeLock(mode | PowerManager.ON_AFTER_RELEASE, ExoMediaPlayer.class.getName());
                wakeLock.setReferenceCounted(false);
            } else {
                Log.e(TAG, "Unable to acquire WAKE_LOCK due to a null power manager");
            }
        } else {
            Log.w(TAG, "Unable to acquire WAKE_LOCK due to missing manifest permission");
        }

        stayAwake(wasHeld);
    }

    public void setRepeatMode(@Player.RepeatMode int repeatMode) {
        player.setRepeatMode(repeatMode);
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

    protected RendererType getExoMediaRendererType(int exoPlayerTrackType) {
        switch (exoPlayerTrackType) {
            case C.TRACK_TYPE_AUDIO:
                return RendererType.AUDIO;
            case C.TRACK_TYPE_VIDEO:
                return RendererType.VIDEO;
            case C.TRACK_TYPE_TEXT:
                return RendererType.CLOSED_CAPTION;
            case C.TRACK_TYPE_METADATA:
                return RendererType.METADATA;
            default:
                return null;
        }
    }

    protected ExoPlayerRendererTracksInfo getExoPlayerTracksInfo(@NonNull RendererType type, int groupIndex, MappingTrackSelector.MappedTrackInfo mappedTrackInfo) {
        // holder for the all exo player renderer track indexes of the specified renderer type
        List<Integer> exoPlayerRendererTrackIndexes = new ArrayList<>();
        // the exoplayer renderer track index related to the specified group index
        int exoPlayerRendererTrackIndex = C.INDEX_UNSET;
        // the corrected exoplayer group index
        int exoPlayerRendererTrackGroupIndex = C.INDEX_UNSET;
        int skippedRenderersGroupsCount = 0;
        if (mappedTrackInfo != null) {
            for (int rendererIndex = 0; rendererIndex < mappedTrackInfo.getRendererCount(); rendererIndex++) {
                int exoPlayerRendererType = mappedTrackInfo.getRendererType(rendererIndex);
                if (type == getExoMediaRendererType(exoPlayerRendererType)) {
                    exoPlayerRendererTrackIndexes.add(rendererIndex);
                    TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(rendererIndex);
                    if (skippedRenderersGroupsCount + trackGroups.length > groupIndex) {
                        if (exoPlayerRendererTrackIndex == C.INDEX_UNSET) {
                            // if the groupIndex belongs to the current exo player renderer
                            exoPlayerRendererTrackIndex = rendererIndex;
                            exoPlayerRendererTrackGroupIndex = groupIndex - skippedRenderersGroupsCount;
                        }
                    } else {
                        skippedRenderersGroupsCount += trackGroups.length;
                    }
                }
            }
        }
        return new ExoPlayerRendererTracksInfo(exoPlayerRendererTrackIndexes, exoPlayerRendererTrackIndex, exoPlayerRendererTrackGroupIndex);
    }

    class ExoPlayerRendererTracksInfo {
        /**
         * The exo player renderer track indexes
         */
        final List<Integer> rendererTrackIndexes;
        /**
         * The renderer track index related to the requested <code>groupIndex</code>
         */
        final int rendererTrackIndex;
        /**
         * The corrected exoplayer group index which may be used to obtain proper track group from the renderer
         */
        final int rendererTrackGroupIndex;

        public ExoPlayerRendererTracksInfo(List<Integer> rendererTrackIndexes, int rendererTrackIndex, int rendererTrackGroupIndex) {
            this.rendererTrackIndexes = Collections.unmodifiableList(rendererTrackIndexes);
            this.rendererTrackIndex = rendererTrackIndex;
            this.rendererTrackGroupIndex = rendererTrackGroupIndex;
        }
    }

    protected void sendMessage(int renderType, int messageType, Object message) {
        sendMessage(renderType, messageType, message, false);
    }

    protected void sendMessage(int renderType, int messageType, Object message, boolean blocking) {
        if (renderers.isEmpty()) {
            return;
        }

        List<PlayerMessage> messages = new ArrayList<>();
        for (Renderer renderer : renderers) {
            if (renderer.getTrackType() == renderType) {
                messages.add(player.createMessage(renderer).setType(messageType).setPayload(message));
            }
        }

        if (blocking) {
            blockingSendMessages(messages);
        } else {
            for (PlayerMessage playerMessage : messages) {
                playerMessage.send();
            }
        }
    }

    /**
     * This was pulled from the <i>Deprecated</i> ExoPlayerImpl#blockingSendMessages method
     *
     * @param messages The messages
     */
    protected void blockingSendMessages(List<PlayerMessage> messages) {
        boolean wasInterrupted = false;
        for (PlayerMessage message : messages) {
            boolean blockMessage = true;
            while (blockMessage) {
                try {
                    message.blockUntilDelivered();
                    blockMessage = false;
                } catch (InterruptedException e) {
                    wasInterrupted = true;
                }
            }
        }

        if (wasInterrupted) {
            // Restore the interrupted status.
            Thread.currentThread().interrupt();
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
            DefaultDrmSessionManager<FrameworkMediaCrypto> sessionManager = new DefaultDrmSessionManager<>(uuid, FrameworkMediaDrm.newInstance(uuid), new DelegatedMediaDrmCallback(), null);
            sessionManager.addListener(mainHandler, capabilitiesListener);

            return sessionManager;
        } catch (Exception e) {
            Log.d(TAG, "Unable to create a DrmSessionManager due to an exception", e);
            return null;
        }
    }

    protected void setupDamSessionManagerAnalytics(DrmSessionManager<FrameworkMediaCrypto> drmSessionManager) {
        if (drmSessionManager instanceof DefaultDrmSessionManager) {
            ((DefaultDrmSessionManager) drmSessionManager).addListener(mainHandler, analyticsCollector);
        }
    }

    private void reportPlayerState() {
        boolean playWhenReady = player.getPlayWhenReady();
        int playbackState = getPlaybackState();

        int newState = stateStore.getState(playWhenReady, playbackState);
        if (newState != stateStore.getMostRecentState()) {
            stateStore.setMostRecentState(playWhenReady, playbackState);

            //Makes sure the buffering notifications are sent
            if (newState == Player.STATE_READY) {
                setBufferRepeaterStarted(true);
            } else if (newState == Player.STATE_IDLE || newState == Player.STATE_ENDED) {
                setBufferRepeaterStarted(false);
            }

            //Because the playWhenReady isn't a state in itself, rather a flag to a state we will ignore informing of
            // see events when that is the only change.  Additionally, on some devices we get states ordered as
            // [seeking, ready, buffering, ready] while on others we get [seeking, buffering, ready]
            boolean informSeekCompletion = stateStore.matchesHistory(new int[]{StateStore.STATE_SEEKING, Player.STATE_BUFFERING, Player.STATE_READY}, true);
            informSeekCompletion |= stateStore.matchesHistory(new int[]{Player.STATE_BUFFERING, StateStore.STATE_SEEKING, Player.STATE_READY}, true);
            informSeekCompletion |= stateStore.matchesHistory(new int[]{StateStore.STATE_SEEKING, Player.STATE_READY, Player.STATE_BUFFERING, Player.STATE_READY}, true);

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
        private int[] prevStates = new int[]{Player.STATE_IDLE, Player.STATE_IDLE, Player.STATE_IDLE, Player.STATE_IDLE};

        public void reset() {
            for (int i = 0; i < prevStates.length; i++) {
                prevStates[i] = Player.STATE_IDLE;
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

    private class CapabilitiesListener implements DefaultDrmSessionEventListener {
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
            TextOutput,
            MetadataOutput {

        @Override
        public void onAudioEnabled(DecoderCounters counters) {
            analyticsCollector.onAudioEnabled(counters);
        }

        @Override
        public void onAudioDisabled(DecoderCounters counters) {
            audioSessionId = C.AUDIO_SESSION_ID_UNSET;
            analyticsCollector.onAudioDisabled(counters);
        }

        @Override
        public void onAudioSessionId(int sessionId) {
            audioSessionId = sessionId;
            analyticsCollector.onAudioSessionId(sessionId);
        }

        @Override
        public void onAudioDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs) {
            analyticsCollector.onAudioDecoderInitialized(decoderName, initializedTimestampMs, initializationDurationMs);
        }

        @Override
        public void onAudioInputFormatChanged(Format format) {
            analyticsCollector.onAudioInputFormatChanged(format);
        }

        @Override
        public void onAudioSinkUnderrun(int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {
            if (internalErrorListener != null) {
                internalErrorListener.onAudioSinkUnderrun(bufferSize, bufferSizeMs, elapsedSinceLastFeedMs);
            }
            analyticsCollector.onAudioSinkUnderrun(bufferSize, bufferSizeMs, elapsedSinceLastFeedMs);
        }

        @Override
        public void onVideoEnabled(DecoderCounters counters) {
            analyticsCollector.onVideoEnabled(counters);
        }

        @Override
        public void onVideoDisabled(DecoderCounters counters) {
            analyticsCollector.onVideoDisabled(counters);
        }

        @Override
        public void onVideoDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs) {
            analyticsCollector.onVideoDecoderInitialized(decoderName, initializedTimestampMs, initializationDurationMs);
        }

        @Override
        public void onVideoInputFormatChanged(Format format) {
            analyticsCollector.onVideoInputFormatChanged(format);
        }

        @Override
        public void onDroppedFrames(int count, long elapsedMs) {
            analyticsCollector.onDroppedFrames(count, elapsedMs);
        }

        @Override
        public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
            for (ExoPlayerListener listener : listeners) {
                listener.onVideoSizeChanged(width, height, unappliedRotationDegrees, pixelWidthHeightRatio);
            }
            analyticsCollector.onVideoSizeChanged(width, height, unappliedRotationDegrees, pixelWidthHeightRatio);
        }

        @Override
        public void onRenderedFirstFrame(Surface surface) {
            analyticsCollector.onRenderedFirstFrame(surface);
        }

        @Override
        public void onMetadata(Metadata metadata) {
            if (metadataListener != null) {
                metadataListener.onMetadata(metadata);
            }
            analyticsCollector.onMetadata(metadata);
        }

        @Override
        public void onCues(List<Cue> cues) {
            if (captionListener != null) {
                captionListener.onCues(cues);
            }
        }
    }
}