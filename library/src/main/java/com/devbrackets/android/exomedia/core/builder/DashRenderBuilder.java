/*
 * Copyright (C) 2016 Brian Wernick,
 * Copyright (C) 2015 Sébastiaan Versteeg,
 * Copyright (C) 2014 The Android Open Source Project
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
package com.devbrackets.android.exomedia.core.builder;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaCodec;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import com.devbrackets.android.exomedia.core.exoplayer.EMExoPlayer;
import com.devbrackets.android.exomedia.core.renderer.EMMediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.DefaultLoadControl;
import com.google.android.exoplayer.LoadControl;
import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.chunk.ChunkSampleSource;
import com.google.android.exoplayer.chunk.ChunkSource;
import com.google.android.exoplayer.chunk.FormatEvaluator.AdaptiveEvaluator;
import com.google.android.exoplayer.dash.DashChunkSource;
import com.google.android.exoplayer.dash.DefaultDashTrackSelector;
import com.google.android.exoplayer.dash.mpd.AdaptationSet;
import com.google.android.exoplayer.dash.mpd.MediaPresentationDescription;
import com.google.android.exoplayer.dash.mpd.MediaPresentationDescriptionParser;
import com.google.android.exoplayer.dash.mpd.Period;
import com.google.android.exoplayer.dash.mpd.UtcTimingElement;
import com.google.android.exoplayer.dash.mpd.UtcTimingElementResolver;
import com.google.android.exoplayer.dash.mpd.UtcTimingElementResolver.UtcTimingCallback;
import com.google.android.exoplayer.drm.DrmSessionManager;
import com.google.android.exoplayer.drm.StreamingDrmSessionManager;
import com.google.android.exoplayer.drm.UnsupportedDrmException;
import com.google.android.exoplayer.text.TextTrackRenderer;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;
import com.google.android.exoplayer.upstream.UriDataSource;
import com.google.android.exoplayer.util.ManifestFetcher;
import com.google.android.exoplayer.util.Util;

import java.io.IOException;

/**
 * A RenderBuilder for parsing and creating the renderers for
 * DASH streams.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class DashRenderBuilder extends RenderBuilder {
    private static final String TAG = "DashRendererBuilder";
    protected static final int LIVE_EDGE_LATENCY_MS = 30000;

    protected static final int SECURITY_LEVEL_UNKNOWN = -1;
    protected static final int SECURITY_LEVEL_1 = 1;
    protected static final int SECURITY_LEVEL_3 = 3;

    protected AsyncRendererBuilder currentAsyncBuilder;

    public DashRenderBuilder(Context context, String userAgent, String url) {
        this(context, userAgent, url, AudioManager.STREAM_MUSIC);
    }

    public DashRenderBuilder(Context context, String userAgent, String url, int streamType) {
        super(context, userAgent, url, streamType);
    }
    @Override
    public void buildRenderers(EMExoPlayer player) {
        currentAsyncBuilder = new AsyncRendererBuilder(context, userAgent, uri, player, streamType);
        currentAsyncBuilder.init();
    }

    @Override
    public void cancel() {
        if (currentAsyncBuilder != null) {
            currentAsyncBuilder.cancel();
            currentAsyncBuilder = null;
        }
    }

    protected UriDataSource createManifestDataSource(Context context, String userAgent) {
        return new DefaultUriDataSource(context, userAgent);
    }

    protected final class AsyncRendererBuilder implements ManifestFetcher.ManifestCallback<MediaPresentationDescription>, UtcTimingCallback {
       protected final Context context;
       protected final String userAgent;
       protected final int streamType;
       protected final EMExoPlayer player;
       protected final ManifestFetcher<MediaPresentationDescription> manifestFetcher;
       protected MediaPresentationDescription currentManifest;
       protected final UriDataSource manifestDataSource;

        protected boolean canceled;
        protected long elapsedRealtimeOffset;

        public AsyncRendererBuilder(Context context, String userAgent, String url, EMExoPlayer player, int streamType) {
            this.context = context;
            this.userAgent = userAgent;
            this.streamType = streamType;
            this.player = player;

            MediaPresentationDescriptionParser parser = new MediaPresentationDescriptionParser();
            manifestDataSource = createManifestDataSource(context, userAgent);
            manifestFetcher = new ManifestFetcher<>(url, manifestDataSource, parser);
        }

        public void init() {
            manifestFetcher.singleLoad(player.getMainHandler().getLooper(), this);
        }

        public void cancel() {
            canceled = true;
        }

        @Override
        public void onSingleManifest(MediaPresentationDescription manifest) {
            if (canceled) {
                return;
            }

            this.currentManifest = manifest;
            if (manifest.dynamic && manifest.utcTiming != null) {
                UtcTimingElementResolver.resolveTimingElement(manifestDataSource, manifest.utcTiming, manifestFetcher.getManifestLoadCompleteTimestamp(), this);
            } else {
                buildRenderers();
            }
        }

        @Override
        public void onSingleManifestError(IOException e) {
            if (canceled) {
                return;
            }

            player.onRenderersError(e);
        }

        @Override
        public void onTimestampResolved(UtcTimingElement utcTiming, long elapsedRealtimeOffset) {
            if (canceled) {
                return;
            }

            this.elapsedRealtimeOffset = elapsedRealtimeOffset;
            buildRenderers();
        }

        @Override
        public void onTimestampError(UtcTimingElement utcTiming, IOException e) {
            if (canceled) {
                return;
            }

            Log.e(TAG, "Failed to resolve UtcTiming element [" + utcTiming + "]", e);
            // Be optimistic and continue in the hope that the device clock is correct.
            buildRenderers();
        }

        protected void buildRenderers() {
            boolean filterHdContent = false;
            boolean hasContentProtection = false;
            Period period = currentManifest.getPeriod(0);
            StreamingDrmSessionManager drmSessionManager = null;

            //Determines if the media has content protection
            for (int i = 0; i < period.adaptationSets.size(); i++) {
                AdaptationSet adaptationSet = period.adaptationSets.get(i);
                if (adaptationSet.type != AdaptationSet.TYPE_UNKNOWN) {
                    hasContentProtection |= adaptationSet.hasContentProtection();
                }
            }

            // Check DRM support if the content is protected
            if (hasContentProtection) {
                if (Util.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    player.onRenderersError(new UnsupportedDrmException(UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME));
                    return;
                }
                try {
                    drmSessionManager = StreamingDrmSessionManager.newWidevineInstance(player.getPlaybackLooper(), null, null, player.getMainHandler(), player);
                    filterHdContent = getWidevineSecurityLevel(drmSessionManager) != SECURITY_LEVEL_1;
                } catch (UnsupportedDrmException e) {
                    player.onRenderersError(e);
                    return;
                }
            }

            buildRenderers(drmSessionManager, filterHdContent);
        }

        protected void buildRenderers(DrmSessionManager drmSessionManager, boolean filterHdContent) {
            Handler mainHandler = player.getMainHandler();
            LoadControl loadControl = new DefaultLoadControl(new DefaultAllocator(BUFFER_SEGMENT_SIZE));
            DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter(mainHandler, player);


            //Create the Sample Source to be used by the Video Renderer
            DataSource dataSourceVideo = createDataSource(context, bandwidthMeter, userAgent);
            ChunkSource chunkSourceVideo = new DashChunkSource(manifestFetcher, DefaultDashTrackSelector.newVideoInstance(context, true, filterHdContent), dataSourceVideo,
                    new AdaptiveEvaluator(bandwidthMeter), LIVE_EDGE_LATENCY_MS, elapsedRealtimeOffset, mainHandler, player, EMExoPlayer.RENDER_VIDEO);
            ChunkSampleSource sampleSourceVideo = new ChunkSampleSource(chunkSourceVideo, loadControl, BUFFER_SEGMENTS_VIDEO * BUFFER_SEGMENT_SIZE,
                    mainHandler, player, EMExoPlayer.RENDER_VIDEO);


            //Create the Sample Source to be used by the Audio Renderer
            DataSource dataSourceAudio = createDataSource(context, bandwidthMeter, userAgent);
            ChunkSource chunkSourceAudio = new DashChunkSource(manifestFetcher, DefaultDashTrackSelector.newAudioInstance(), dataSourceAudio,
                    null, LIVE_EDGE_LATENCY_MS, elapsedRealtimeOffset, mainHandler, player, EMExoPlayer.RENDER_AUDIO);
            ChunkSampleSource sampleSourceAudio = new ChunkSampleSource(chunkSourceAudio, loadControl, BUFFER_SEGMENTS_AUDIO * BUFFER_SEGMENT_SIZE,
                    mainHandler, player, EMExoPlayer.RENDER_AUDIO);


            //Create the Sample Source to be used by the Closed Captions Renderer
            DataSource dataSourceCC = createDataSource(context, bandwidthMeter, userAgent);
            ChunkSource chunkSourceCC = new DashChunkSource(manifestFetcher, DefaultDashTrackSelector.newAudioInstance(), dataSourceCC,
                    null, LIVE_EDGE_LATENCY_MS, elapsedRealtimeOffset, mainHandler, player, EMExoPlayer.RENDER_CLOSED_CAPTION);
            ChunkSampleSource sampleSourceCC = new ChunkSampleSource(chunkSourceCC, loadControl, BUFFER_SEGMENTS_TEXT * BUFFER_SEGMENT_SIZE,
                    mainHandler, player, EMExoPlayer.RENDER_CLOSED_CAPTION);


            //Build the renderers
            MediaCodecVideoTrackRenderer videoRenderer = new MediaCodecVideoTrackRenderer(context, sampleSourceVideo, MediaCodecSelector.DEFAULT,
                    MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT, MAX_JOIN_TIME, mainHandler, player, DROPPED_FRAME_NOTIFICATION_AMOUNT);
            EMMediaCodecAudioTrackRenderer audioRenderer = new EMMediaCodecAudioTrackRenderer(sampleSourceAudio, MediaCodecSelector.DEFAULT,
                    drmSessionManager, true, mainHandler, player, AudioCapabilities.getCapabilities(context), streamType);
            TextTrackRenderer captionsRenderer = new TextTrackRenderer(sampleSourceCC, player, mainHandler.getLooper());


            // Invoke the callback.
            TrackRenderer[] renderers = new TrackRenderer[EMExoPlayer.RENDER_COUNT];
            renderers[EMExoPlayer.RENDER_VIDEO] = videoRenderer;
            renderers[EMExoPlayer.RENDER_AUDIO] = audioRenderer;
            renderers[EMExoPlayer.RENDER_CLOSED_CAPTION] = captionsRenderer;
            player.onRenderers(renderers, bandwidthMeter);
        }

        protected int getWidevineSecurityLevel(StreamingDrmSessionManager sessionManager) {
            String securityLevelProperty = sessionManager.getPropertyString("securityLevel");
            return securityLevelProperty.equals("L1") ? SECURITY_LEVEL_1 : securityLevelProperty.equals("L3") ? SECURITY_LEVEL_3 : SECURITY_LEVEL_UNKNOWN;
        }
    }
}