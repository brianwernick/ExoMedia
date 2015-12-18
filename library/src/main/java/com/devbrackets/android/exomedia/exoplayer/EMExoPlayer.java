/*
 * Copyright (C) 2015 Brian Wernick,
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

package com.devbrackets.android.exomedia.exoplayer;

import android.content.Context;
import android.media.MediaCodec;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.view.Surface;

import com.devbrackets.android.exomedia.builder.RenderBuilder;
import com.devbrackets.android.exomedia.listener.CaptionListener;
import com.devbrackets.android.exomedia.listener.ExoPlayerListener;
import com.devbrackets.android.exomedia.listener.Id3MetadataListener;
import com.devbrackets.android.exomedia.listener.InfoListener;
import com.devbrackets.android.exomedia.listener.InternalErrorListener;
import com.devbrackets.android.exomedia.renderer.EMMediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.DummyTrackRenderer;
import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecTrackRenderer;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.TimeRange;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.audio.AudioTrack;
import com.google.android.exoplayer.chunk.ChunkSampleSource;
import com.google.android.exoplayer.chunk.Format;
import com.google.android.exoplayer.dash.DashChunkSource;
import com.google.android.exoplayer.drm.StreamingDrmSessionManager;
import com.google.android.exoplayer.hls.HlsSampleSource;
import com.google.android.exoplayer.metadata.MetadataTrackRenderer;
import com.google.android.exoplayer.text.Cue;
import com.google.android.exoplayer.text.TextRenderer;
import com.google.android.exoplayer.upstream.BandwidthMeter;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.util.PlayerControl;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class EMExoPlayer implements
        ExoPlayer.Listener,
        ChunkSampleSource.EventListener,
        HlsSampleSource.EventListener,
        DefaultBandwidthMeter.EventListener,
        MediaCodecVideoTrackRenderer.EventListener,
        MediaCodecAudioTrackRenderer.EventListener,
        StreamingDrmSessionManager.EventListener,
        DashChunkSource.EventListener,
        MetadataTrackRenderer.MetadataRenderer<Map<String, Object>>,
        TextRenderer {

    public static final int DISABLED_TRACK = -1;

    public static final int RENDER_COUNT = 4;
    public static final int RENDER_VIDEO_INDEX = 0;
    public static final int RENDER_AUDIO_INDEX = 1;
    public static final int RENDER_CLOSED_CAPTION_INDEX = 2;
    public static final int RENDER_TIMED_METADATA_INDEX = 3;

    public static final int BUFFER_LENGTH_MIN = 1000;
    public static final int REBUFFER_LENGTH_MIN = 5000;

    public enum RenderBuildingState {
        IDLE,
        BUILDING,
        BUILT
    }

    private RenderBuilder rendererBuilder;
    private final ExoPlayer player;
    private final PlayerControl playerControl;
    private final Handler mainHandler;
    private final CopyOnWriteArrayList<ExoPlayerListener> listeners;

    private RenderBuildingState rendererBuildingState;
    private int lastReportedPlaybackState;
    private boolean lastReportedPlayWhenReady;

    private boolean prepared = false;

    private Surface surface;
    private TrackRenderer videoRenderer;
    private TrackRenderer audioRenderer;

    private CaptionListener captionListener;
    private Id3MetadataListener id3MetadataListener;
    private InternalErrorListener internalErrorListener;
    private InfoListener infoListener;

    private PowerManager.WakeLock wakeLock = null;

    public EMExoPlayer() {
        this(null);
    }

    public EMExoPlayer(RenderBuilder rendererBuilder) {
        this.rendererBuilder = rendererBuilder;
        player = ExoPlayer.Factory.newInstance(RENDER_COUNT, BUFFER_LENGTH_MIN, REBUFFER_LENGTH_MIN);
        player.addListener(this);
        playerControl = new PlayerControl(player);

        mainHandler = new Handler();
        listeners = new CopyOnWriteArrayList<>();
        lastReportedPlaybackState = ExoPlayer.STATE_IDLE;
        rendererBuildingState = RenderBuildingState.IDLE;
        player.setSelectedTrack(RENDER_CLOSED_CAPTION_INDEX, DISABLED_TRACK);
    }

    public void replaceRenderBuilder(RenderBuilder renderBuilder) {
        this.rendererBuilder = renderBuilder;

        prepared = false;
        prepare();
    }

    public PlayerControl getPlayerControl() {
        return playerControl;
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

    public void setInternalErrorListener(InternalErrorListener listener) {
        internalErrorListener = listener;
    }

    public void setInfoListener(InfoListener listener) {
        infoListener = listener;
    }

    public void setCaptionListener(CaptionListener listener) {
        captionListener = listener;
    }

    public void setMetadataListener(Id3MetadataListener listener) {
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

    public int getTrackCount(int type) {
        return player.getTrackCount(type);
    }

    public int getSelectedTrack(int type) {
        return player.getSelectedTrack(type);
    }

    public void setSelectedTrack(int type, int index) {
        player.setSelectedTrack(type, index);
        if (type == RENDER_CLOSED_CAPTION_INDEX && index == DISABLED_TRACK && captionListener != null) {
            captionListener.onCues(Collections.<Cue>emptyList());
        }
    }

    public void setVolume(float volume) {
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
    }

    public void onRenderers(TrackRenderer[] renderers, @Nullable BandwidthMeter bandwidthMeter) {
        for (int i = 0; i < RENDER_COUNT; i++) {
            if (renderers[i] == null) {
                // Convert a null renderer to a dummy renderer.
                renderers[i] = new DummyTrackRenderer();
            }
        }

        // Complete preparation.
        this.videoRenderer = renderers[RENDER_VIDEO_INDEX];
        this.audioRenderer = renderers[RENDER_AUDIO_INDEX];

        pushSurface(false);
        player.prepare(renderers);
        rendererBuildingState = RenderBuildingState.BUILT;
    }

    public void onRenderersError(Exception e) {
        if (internalErrorListener != null) {
            internalErrorListener.onRendererInitializationError(e);
        }

        for (ExoPlayerListener listener : listeners) {
            listener.onError(e);
        }

        rendererBuildingState = RenderBuildingState.IDLE;
        reportPlayerState();
    }

    public void setPlayWhenReady(boolean playWhenReady) {
        player.setPlayWhenReady(playWhenReady);
        stayAwake(playWhenReady);
    }

    public void seekTo(long positionMs) {
        player.seekTo(positionMs);
    }

    public void release() {
        if (rendererBuilder != null) {
            rendererBuilder.cancel();
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

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(mode | PowerManager.ON_AFTER_RELEASE, EMExoPlayer.class.getName());
        wakeLock.setReferenceCounted(false);
        if (wasHeld) {
            wakeLock.acquire();
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
        if (wakeLock != null) {
            if (awake && !wakeLock.isHeld()) {
                wakeLock.acquire();
            } else if (!awake && wakeLock.isHeld()) {
                wakeLock.release();
            }
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
            listener.onError(exception);
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

        if (sourceId == RENDER_VIDEO_INDEX) {
            infoListener.onVideoFormatEnabled(format, trigger, mediaTimeMs);
        } else if (sourceId == RENDER_AUDIO_INDEX) {
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
        if (captionListener != null && getSelectedTrack(RENDER_CLOSED_CAPTION_INDEX) != DISABLED_TRACK) {
            captionListener.onCues(cues);
        }
    }

    @Override
    public void onMetadata(Map<String, Object> metadata) {
        if (id3MetadataListener != null && getSelectedTrack(RENDER_TIMED_METADATA_INDEX) != DISABLED_TRACK) {
            id3MetadataListener.onId3Metadata(metadata);
        }
    }

    @Override
    public void onAvailableRangeChanged(TimeRange availableRange) {
        if (infoListener != null) {
            infoListener.onAvailableRangeChanged(availableRange);
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

    private void reportPlayerState() {
        boolean playWhenReady = player.getPlayWhenReady();
        int playbackState = getPlaybackState();

        if (lastReportedPlayWhenReady != playWhenReady || lastReportedPlaybackState != playbackState) {
            for (ExoPlayerListener listener : listeners) {
                listener.onStateChanged(playWhenReady, playbackState);
            }

            lastReportedPlayWhenReady = playWhenReady;
            lastReportedPlaybackState = playbackState;
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
}