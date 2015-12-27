/*
 * Copyright (C) 2015 Brian Wernick,
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
package com.devbrackets.android.exomedia.builder;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.MediaCodec;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import com.devbrackets.android.exomedia.exoplayer.EMExoPlayer;
import com.google.android.exoplayer.DefaultLoadControl;
import com.google.android.exoplayer.LoadControl;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.chunk.ChunkSampleSource;
import com.google.android.exoplayer.chunk.ChunkSource;
import com.google.android.exoplayer.chunk.FormatEvaluator.AdaptiveEvaluator;
import com.google.android.exoplayer.dash.DashChunkSource;
import com.google.android.exoplayer.dash.DefaultDashTrackSelector;
import com.google.android.exoplayer.dash.mpd.MediaPresentationDescription;
import com.google.android.exoplayer.dash.mpd.MediaPresentationDescriptionParser;
import com.google.android.exoplayer.dash.mpd.UtcTimingElement;
import com.google.android.exoplayer.dash.mpd.UtcTimingElementResolver;
import com.google.android.exoplayer.dash.mpd.UtcTimingElementResolver.UtcTimingCallback;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;
import com.google.android.exoplayer.upstream.UriDataSource;
import com.google.android.exoplayer.util.ManifestFetcher;

import java.io.IOException;

/**
 * A RenderBuilder for parsing and creating the renderers for
 * DASH streams.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class DashRenderBuilder extends RenderBuilder {
    private static final String TAG = "DashRendererBuilder";

    private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
    private static final int BUFFER_SEGMENTS_VIDEO = 200;
    private static final int BUFFER_SEGMENTS_AUDIO = 54;
    private static final int LIVE_EDGE_LATENCY_MS = 30000;


    private final Context context;
    private final String userAgent;
    private final String url;

    private AsyncRendererBuilder currentAsyncBuilder;

    public DashRenderBuilder(Context context, String userAgent, String url) {
        super(context, userAgent, url);

        this.context = context;
        this.userAgent = userAgent;
        this.url = url;
    }

    @Override
    public void buildRenderers(EMExoPlayer player) {
        currentAsyncBuilder = new AsyncRendererBuilder(context, userAgent, url, player);
        currentAsyncBuilder.init();
    }

    @Override
    public void cancel() {
        if (currentAsyncBuilder != null) {
            currentAsyncBuilder.cancel();
            currentAsyncBuilder = null;
        }
    }

    private static final class AsyncRendererBuilder implements ManifestFetcher.ManifestCallback<MediaPresentationDescription>, UtcTimingCallback {
        private final Context context;
        private final String userAgent;
        private final EMExoPlayer player;
        private final ManifestFetcher<MediaPresentationDescription> manifestFetcher;
        private final UriDataSource manifestDataSource;

        private boolean canceled;
        private long elapsedRealtimeOffset;

        public AsyncRendererBuilder(Context context, String userAgent, String url, EMExoPlayer player) {
            this.context = context;
            this.userAgent = userAgent;
            this.player = player;

            MediaPresentationDescriptionParser parser = new MediaPresentationDescriptionParser();
            manifestDataSource = new DefaultUriDataSource(context, userAgent);
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

        private void buildRenderers() {
            Handler mainHandler = player.getMainHandler();
            LoadControl loadControl = new DefaultLoadControl(new DefaultAllocator(BUFFER_SEGMENT_SIZE));
            DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter(mainHandler, player);

            //Create the Sample Source to be used by the Video Renderer
            DataSource dataSourceVideo = new DefaultUriDataSource(context, bandwidthMeter, userAgent, true);
            ChunkSource chunkSourceVideo = new DashChunkSource(manifestFetcher, DefaultDashTrackSelector.newVideoInstance(context, true, false),
                    dataSourceVideo, new AdaptiveEvaluator(bandwidthMeter), LIVE_EDGE_LATENCY_MS, elapsedRealtimeOffset, mainHandler, player);
            ChunkSampleSource sampleSourceVideo = new ChunkSampleSource(chunkSourceVideo, loadControl, BUFFER_SEGMENTS_VIDEO * BUFFER_SEGMENT_SIZE,
                    mainHandler, player, EMExoPlayer.RENDER_VIDEO_INDEX);

            //Create the Sample Source to be used by the Audio Renderer
            DataSource dataSourceAudio = new DefaultUriDataSource(context, bandwidthMeter, userAgent, true);
            ChunkSource chunkSourceAudio = new DashChunkSource(manifestFetcher, DefaultDashTrackSelector.newAudioInstance(),
                    dataSourceAudio, null, LIVE_EDGE_LATENCY_MS, elapsedRealtimeOffset, mainHandler, player);
            ChunkSampleSource sampleSourceAudio = new ChunkSampleSource(chunkSourceAudio, loadControl, BUFFER_SEGMENTS_AUDIO * BUFFER_SEGMENT_SIZE,
                    mainHandler, player, EMExoPlayer.RENDER_AUDIO_INDEX);

            //Create the renderers
            MediaCodecVideoTrackRenderer videoRenderer = new MediaCodecVideoTrackRenderer(context, sampleSourceVideo,
                    MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT, MAX_JOIN_TIME, mainHandler, player, DROPPED_FRAME_NOTIFICATION_AMOUNT);
            MediaCodecAudioTrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(sampleSourceAudio,
                    null, true, mainHandler, player, AudioCapabilities.getCapabilities(context));


            // Invoke the callback.
            TrackRenderer[] renderers = new TrackRenderer[EMExoPlayer.RENDER_COUNT];
            renderers[EMExoPlayer.RENDER_VIDEO_INDEX] = videoRenderer;
            renderers[EMExoPlayer.RENDER_AUDIO_INDEX] = audioRenderer;
            player.onRenderers(renderers, bandwidthMeter);
        }
    }
}