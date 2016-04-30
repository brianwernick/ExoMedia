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
import android.media.MediaCodec;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.support.annotation.FloatRange;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import android.support.v4.util.ArrayMap;
import android.util.Log;
import android.view.Surface;

import com.devbrackets.android.exomedia.annotation.TrackRenderType;
import com.devbrackets.android.exomedia.core.builder.RenderBuilder;
import com.devbrackets.android.exomedia.core.listener.CaptionListener;
import com.devbrackets.android.exomedia.core.listener.ExoPlayerListener;
import com.devbrackets.android.exomedia.core.listener.Id3MetadataListener;
import com.devbrackets.android.exomedia.core.listener.InfoListener;
import com.devbrackets.android.exomedia.core.listener.InternalErrorListener;
import com.devbrackets.android.exomedia.core.renderer.EMMediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.DummyTrackRenderer;
import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecTrackRenderer;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.TimeRange;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.audio.AudioCapabilitiesReceiver;
import com.google.android.exoplayer.audio.AudioTrack;
import com.google.android.exoplayer.chunk.ChunkSampleSource;
import com.google.android.exoplayer.chunk.Format;
import com.google.android.exoplayer.dash.DashChunkSource;
import com.google.android.exoplayer.drm.StreamingDrmSessionManager;
import com.google.android.exoplayer.hls.HlsSampleSource;
import com.google.android.exoplayer.metadata.MetadataTrackRenderer;
import com.google.android.exoplayer.metadata.id3.Id3Frame;
import com.google.android.exoplayer.text.Cue;
import com.google.android.exoplayer.text.TextRenderer;
import com.google.android.exoplayer.upstream.BandwidthMeter;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unused")
public class EMExoPlayer implements
        ExoPlayer.Listener,
        AudioCapabilitiesReceiver.Listener,
        ChunkSampleSource.EventListener,
        HlsSampleSource.EventListener,
        DefaultBandwidthMeter.EventListener,
        MediaCodecVideoTrackRenderer.EventListener,
        MediaCodecAudioTrackRenderer.EventListener,
        StreamingDrmSessionManager.EventListener,
        DashChunkSource.EventListener,
        MetadataTrackRenderer.MetadataRenderer<List<Id3Frame>>,
        TextRenderer {
    private static final String TAG = "EMExoPlayer";
    public static final int DISABLED_TRACK = -1;

    public static final int RENDER_COUNT = 4;
    public static final int RENDER_VIDEO = 0;
    public static final int RENDER_AUDIO = 1;
    public static final int RENDER_CLOSED_CAPTION = 2;
    public static final int RENDER_TIMED_METADATA = 3;

    public static final int BUFFER_LENGTH_MIN = 1000;
    public static final int REBUFFER_LENGTH_MIN = 5000;

    public enum RenderBuildingState {
        IDLE,
        BUILDING,
        BUILT
    }

    private RenderBuilder rendererBuilder;
    private final ExoPlayer player;
    private final Handler mainHandler;
    private final CopyOnWriteArrayList<ExoPlayerListener> listeners;

    private final AtomicBoolean stopped = new AtomicBoolean();

    private RenderBuildingState rendererBuildingState;
    private StateStore stateStore = new StateStore();

    private boolean prepared = false;

    private Surface surface;
    private TrackRenderer videoRenderer;
    private TrackRenderer audioRenderer;

    @Nullable
    private AudioCapabilities audioCapabilities;
    @Nullable
    private AudioCapabilitiesReceiver audioCapabilitiesReceiver;

    @Nullable
    private CaptionListener captionListener;
    @Nullable
    private Id3MetadataListener id3MetadataListener;
    @Nullable
    private InternalErrorListener internalErrorListener;
    @Nullable
    private InfoListener infoListener;

    @Nullable
    private PowerManager.WakeLock wakeLock = null;

    public EMExoPlayer() {
        this(null);
    }

    public EMExoPlayer(@Nullable RenderBuilder rendererBuilder) {
        player = ExoPlayer.Factory.newInstance(RENDER_COUNT, BUFFER_LENGTH_MIN, REBUFFER_LENGTH_MIN);
        player.addListener(this);

        mainHandler = new Handler();
        listeners = new CopyOnWriteArrayList<>();
        rendererBuildingState = RenderBuildingState.IDLE;
        player.setSelectedTrack(RENDER_CLOSED_CAPTION, DISABLED_TRACK);

        replaceRenderBuilder(rendererBuilder);
    }

    public void replaceRenderBuilder(@Nullable RenderBuilder renderBuilder) {
        this.rendererBuilder = renderBuilder;
        if (rendererBuilder != null && audioCapabilities == null) {
            audioCapabilitiesReceiver = new AudioCapabilitiesReceiver(rendererBuilder.getContext(), this);
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

    public void setInternalErrorListener(@Nullable InternalErrorListener listener) {
        internalErrorListener = listener;
    }

    public void setInfoListener(@Nullable InfoListener listener) {
        infoListener = listener;
    }

    public void setCaptionListener(@Nullable CaptionListener listener) {
        captionListener = listener;
    }

    public void setMetadataListener(@Nullable Id3MetadataListener listener) {
        id3MetadataListener = listener;
    }

    public void setSurface(Surface surface) {
        this.surface = surface;
        pushSurface(false);
    }

    public Surface getSurface() {
        return surface;
    }

    public void blockingClearSurface() {
        surface = null;
        pushSurface(true);
    }

    @Nullable
    public AudioCapabilities getAudioCapabilities() {
        return audioCapabilities;
    }

    /**
     * Retrieves a list of available tracks
     *
     * @return A list of available tracks associated with each type (see {@link com.devbrackets.android.exomedia.annotation.TrackRenderType})
     */
    @Nullable
    public Map<Integer, List<MediaFormat>> getAvailableTracks() {
        if (getPlaybackState() == ExoPlayer.STATE_IDLE) {
            return null;
        }

        Map<Integer, List<MediaFormat>> trackMap = new ArrayMap<>();
        int[] trackTypes = new int[] {RENDER_AUDIO, RENDER_VIDEO, RENDER_CLOSED_CAPTION, RENDER_TIMED_METADATA};

        //Populates the map with all available tracks
        for (int type : trackTypes) {
            List<MediaFormat> tracks = new ArrayList<>(getTrackCount(type));
            trackMap.put(type, tracks);

            for (int i = 0; i < tracks.size(); i++) {
                tracks.add(getTrackFormat(type, i));
            }
        }

        return trackMap;
    }

    public int getTrackCount(@TrackRenderType int type) {
        return player.getTrackCount(type);
    }

    public MediaFormat getTrackFormat(@TrackRenderType int type, int index) {
        return player.getTrackFormat(type, index);
    }

    public int getSelectedTrack(@TrackRenderType int type) {
        return player.getSelectedTrack(type);
    }

    public void setSelectedTrack(@TrackRenderType int type, int index) {
        player.setSelectedTrack(type, index);
        if (type == RENDER_CLOSED_CAPTION && index == DISABLED_TRACK && captionListener != null) {
            captionListener.onCues(Collections.<Cue>emptyList());
        }
    }

    public void setVolume(@FloatRange(from = 0.0, to = 1.0) float volume) {
        player.sendMessage(audioRenderer, MediaCodecAudioTrackRenderer.MSG_SET_VOLUME, volume);
    }

    public void forcePrepare() {
        prepared = false;
    }

    public void prepare() {
        if (prepared || rendererBuilder == null) {
            return;
        }

        if (rendererBuildingState == RenderBuildingState.BUILT) {
            player.stop();
        }

        videoRenderer = null;
        rendererBuildingState = RenderBuildingState.BUILDING;
        reportPlayerState();

        rendererBuilder.buildRenderers(this);
        prepared = true;

        stopped.set(false);
    }

    public void onRenderers(TrackRenderer[] renderers, @Nullable BandwidthMeter bandwidthMeter) {
        for (int i = 0; i < RENDER_COUNT; i++) {
            if (renderers[i] == null) {
                // Convert a null renderer to a dummy renderer.
                renderers[i] = new DummyTrackRenderer();
            }
        }

        // Complete preparation.
        this.videoRenderer = renderers[RENDER_VIDEO];
        this.audioRenderer = renderers[RENDER_AUDIO];

        pushSurface(false);
        player.prepare(renderers);
        rendererBuildingState = RenderBuildingState.BUILT;
    }

    public void onRenderersError(Exception e) {
        if (internalErrorListener != null) {
            internalErrorListener.onRendererInitializationError(e);
        }

        for (ExoPlayerListener listener : listeners) {
            listener.onError(this, e);
        }

        rendererBuildingState = RenderBuildingState.IDLE;
        reportPlayerState();
    }

    public void stop() {
        if(!stopped.getAndSet(true)) {
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
        if(playbackState != ExoPlayer.STATE_IDLE && playbackState != ExoPlayer.STATE_ENDED) {
            return false;
        }

        seekTo(0);
        setPlayWhenReady(true);

        forcePrepare();
        prepare();

        return true;
    }

    public void release() {
        if (rendererBuilder != null) {
            rendererBuilder.cancel();
        }

        if (audioCapabilitiesReceiver != null) {
            audioCapabilitiesReceiver.unregister();
            audioCapabilitiesReceiver = null;
        }

        rendererBuildingState = RenderBuildingState.IDLE;
        surface = null;
        player.release();
        stayAwake(false);
    }

    public int getPlaybackState() {
        if (rendererBuildingState == RenderBuildingState.BUILDING) {
            return ExoPlayer.STATE_PREPARING;
        }

        return player.getPlaybackState();
    }

    public int getAudioSessionId() {
        if (audioRenderer != null) {
            return ((EMMediaCodecAudioTrackRenderer)audioRenderer).getAudioSessionId();
        }

        return 0;
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

    public Looper getPlaybackLooper() {
        return player.getPlaybackLooper();
    }

    public Handler getMainHandler() {
        return mainHandler;
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
            wakeLock = pm.newWakeLock(mode | PowerManager.ON_AFTER_RELEASE, EMExoPlayer.class.getName());
            wakeLock.setReferenceCounted(false);
        } else {
            Log.w(TAG, "Unable to acquire WAKE_LOCK due to missing manifest permission");
        }

        stayAwake(wasHeld);
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
        rendererBuildingState = RenderBuildingState.IDLE;
        for (ExoPlayerListener listener : listeners) {
            listener.onError(this, exception);
        }
    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        for (ExoPlayerListener listener : listeners) {
            listener.onVideoSizeChanged(width, height, unappliedRotationDegrees, pixelWidthHeightRatio);
        }
    }

    @Override
    public void onDroppedFrames(int count, long elapsed) {
        if (infoListener != null) {
            infoListener.onDroppedFrames(count, elapsed);
        }
    }

    @Override
    public void onBandwidthSample(int elapsedMs, long bytes, long bitrateEstimate) {
        if (infoListener != null) {
            infoListener.onBandwidthSample(elapsedMs, bytes, bitrateEstimate);
        }
    }

    @Override
    public void onDownstreamFormatChanged(int sourceId, Format format, int trigger, long mediaTimeMs) {
        if (infoListener == null) {
            return;
        }

        if (sourceId == RENDER_VIDEO) {
            infoListener.onVideoFormatEnabled(format, trigger, mediaTimeMs);
        } else if (sourceId == RENDER_AUDIO) {
            infoListener.onAudioFormatEnabled(format, trigger, mediaTimeMs);
        }
    }

    @Override
    public void onDrmKeysLoaded() {
        //Purposefully left blank
    }

    @Override
    public void onDrmSessionManagerError(Exception e) {
        if (internalErrorListener != null) {
            internalErrorListener.onDrmSessionManagerError(e);
        }
    }

    @Override
    public void onDecoderInitializationError(MediaCodecTrackRenderer.DecoderInitializationException e) {
        if (internalErrorListener != null) {
            internalErrorListener.onDecoderInitializationError(e);
        }
    }

    @Override
    public void onDecoderInitialized(String decoderName, long elapsedRealtimeMs, long initializationDurationMs) {
        if (infoListener != null) {
            infoListener.onDecoderInitialized(decoderName, elapsedRealtimeMs, initializationDurationMs);
        }
    }

    @Override
    public void onAudioTrackInitializationError(AudioTrack.InitializationException e) {
        if (internalErrorListener != null) {
            internalErrorListener.onAudioTrackInitializationError(e);
        }
    }

    @Override
    public void onAudioTrackWriteError(AudioTrack.WriteException e) {
        if (internalErrorListener != null) {
            internalErrorListener.onAudioTrackWriteError(e);
        }
    }

    @Override
    public void onAudioTrackUnderrun(int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {
        if (internalErrorListener != null) {
            internalErrorListener.onAudioTrackUnderrun(bufferSize, bufferSizeMs, elapsedSinceLastFeedMs);
        }
    }

    @Override
    public void onCryptoError(MediaCodec.CryptoException e) {
        if (internalErrorListener != null) {
            internalErrorListener.onCryptoError(e);
        }
    }

    @Override
    public void onLoadError(int sourceId, IOException e) {
        if (internalErrorListener != null) {
            internalErrorListener.onLoadError(sourceId, e);
        }
    }

    @Override
    public void onCues(List<Cue> cues) {
        if (captionListener != null && getSelectedTrack(RENDER_CLOSED_CAPTION) != DISABLED_TRACK) {
            captionListener.onCues(cues);
        }
    }

    @Override
    public void onMetadata(List<Id3Frame> metadata) {
        if (id3MetadataListener != null && getSelectedTrack(RENDER_TIMED_METADATA) != DISABLED_TRACK) {
            id3MetadataListener.onId3Metadata(metadata);
        }
    }

    @Override
    public void onAvailableRangeChanged(int sourceId, TimeRange availableRange) {
        if (infoListener != null) {
            infoListener.onAvailableRangeChanged(sourceId, availableRange);
        }
    }

    @Override
    public void onPlayWhenReadyCommitted() {
        //Purposefully left blank
    }

    @Override
    public void onDrawnToSurface(Surface surface) {
        //Purposefully left blank
    }

    @Override
    public void onLoadStarted(int sourceId, long length, int type, int trigger, Format format, long mediaStartTimeMs, long mediaEndTimeMs) {
        if (infoListener != null) {
            infoListener.onLoadStarted(sourceId, length, type, trigger, format, mediaStartTimeMs, mediaEndTimeMs);
        }
    }

    @Override
    public void onLoadCompleted(int sourceId, long bytesLoaded, int type, int trigger, Format format, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs) {
        if (infoListener != null) {
            infoListener.onLoadCompleted(sourceId, bytesLoaded, type, trigger, format, mediaStartTimeMs, mediaEndTimeMs, elapsedRealtimeMs, loadDurationMs);
        }
    }

    @Override
    public void onLoadCanceled(int sourceId, long bytesLoaded) {
        //Purposefully left blank
    }

    @Override
    public void onUpstreamDiscarded(int sourceId, long mediaStartTimeMs, long mediaEndTimeMs) {
        //Purposefully left blank
    }

    @Override
    public void onAudioCapabilitiesChanged(AudioCapabilities audioCapabilities) {
        if (audioCapabilities.equals(this.audioCapabilities)) {
            return;
        }

        this.audioCapabilities = audioCapabilities;
        if (rendererBuilder == null) {
            return;
        }

        //Restarts the media playback to allow the RenderBuilder to handle the audio channel determination
        boolean playWhenReady = getPlayWhenReady();
        long currentPosition = getCurrentPosition();

        replaceRenderBuilder(rendererBuilder);

        player.seekTo(currentPosition);
        player.setPlayWhenReady(playWhenReady);
    }

    private void reportPlayerState() {
        boolean playWhenReady = player.getPlayWhenReady();
        int playbackState = getPlaybackState();

        int newState = stateStore.getState(playWhenReady, playbackState);
        if (newState != stateStore.getMostRecentState()) {
            stateStore.setMostRecentState(playWhenReady, playbackState);

            //Because the playWhenReady isn't a state in itself, rather a flag to a state we will ignore informing of
            // see events when that is the only change.  Additionally, on some devices we get states ordered as
            // [seeking, ready, buffering, ready] while on others we get [seeking, buffering, ready]
            boolean informSeekCompletion = stateStore.matchesHistory(new int[]{StateStore.STATE_SEEKING, ExoPlayer.STATE_BUFFERING, ExoPlayer.STATE_READY}, true);
            informSeekCompletion |= stateStore.matchesHistory(new int[]{StateStore.STATE_SEEKING, ExoPlayer.STATE_READY, ExoPlayer.STATE_BUFFERING, ExoPlayer.STATE_READY}, true);

            for (ExoPlayerListener listener : listeners) {
                listener.onStateChanged(playWhenReady, playbackState);

                if (informSeekCompletion) {
                    listener.onSeekComplete();
                }
            }
        }
    }

    private void pushSurface(boolean blockForSurfacePush) {
        if (videoRenderer == null) {
            return;
        }

        if (blockForSurfacePush) {
            player.blockingSendMessage(videoRenderer, MediaCodecVideoTrackRenderer.MSG_SET_SURFACE, surface);
        } else {
            player.sendMessage(videoRenderer, MediaCodecVideoTrackRenderer.MSG_SET_SURFACE, surface);
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
}