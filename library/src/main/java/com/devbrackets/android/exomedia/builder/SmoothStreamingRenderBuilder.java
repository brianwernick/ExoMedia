/*
 * Copyright (C) 2015 Sébastiaan Versteeg,
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
package com.devbrackets.android.exomedia.builder;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.MediaCodec;
import android.os.Build;
import android.os.Handler;

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
import com.google.android.exoplayer.drm.DrmSessionManager;
import com.google.android.exoplayer.drm.StreamingDrmSessionManager;
import com.google.android.exoplayer.drm.UnsupportedDrmException;
import com.google.android.exoplayer.smoothstreaming.DefaultSmoothStreamingTrackSelector;
import com.google.android.exoplayer.smoothstreaming.SmoothStreamingChunkSource;
import com.google.android.exoplayer.smoothstreaming.SmoothStreamingManifest;
import com.google.android.exoplayer.smoothstreaming.SmoothStreamingManifest.StreamElement;
import com.google.android.exoplayer.smoothstreaming.SmoothStreamingManifestParser;
import com.google.android.exoplayer.text.TextTrackRenderer;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;
import com.google.android.exoplayer.util.ManifestFetcher;
import com.google.android.exoplayer.util.Util;

import java.io.IOException;

/**
 * A RenderBuilder for parsing and creating the renderers for
 * Smooth Streaming streams.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class SmoothStreamingRenderBuilder extends RenderBuilder {

    private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
    private static final int VIDEO_BUFFER_SEGMENTS = 200;
    private static final int AUDIO_BUFFER_SEGMENTS = 54;
    private static final int TEXT_BUFFER_SEGMENTS = 2;
    private static final int LIVE_EDGE_LATENCY_MS = 30000;

    private final Context context;
    private final String userAgent;
    private final String url;

    private AsyncRendererBuilder currentAsyncBuilder;

    public SmoothStreamingRenderBuilder(Context context, String userAgent, String url) {
        super(context, userAgent, url);
        this.context = context;
        this.userAgent = userAgent;
        this.url = Util.toLowerInvariant(url).endsWith("/manifest") ? url : url + "/Manifest";
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

    private static final class AsyncRendererBuilder
            implements ManifestFetcher.ManifestCallback<SmoothStreamingManifest> {

        private final Context context;
        private final String userAgent;
        private final EMExoPlayer player;
        private final ManifestFetcher<SmoothStreamingManifest> manifestFetcher;

        private boolean canceled;

        public AsyncRendererBuilder(Context context, String userAgent, String url, EMExoPlayer player) {
            this.context = context;
            this.userAgent = userAgent;
            this.player = player;
            SmoothStreamingManifestParser parser = new SmoothStreamingManifestParser();
            manifestFetcher = new ManifestFetcher<>(url, new DefaultHttpDataSource(userAgent, null),
                    parser);
        }

        public void init() {
            manifestFetcher.singleLoad(player.getMainHandler().getLooper(), this);
        }

        public void cancel() {
            canceled = true;
        }

        @Override
        public void onSingleManifestError(IOException exception) {
            if (canceled) {
                return;
            }

            player.onRenderersError(exception);
        }

        @Override
        public void onSingleManifest(SmoothStreamingManifest manifest) {
            if (canceled) {
                return;
            }

            Handler mainHandler = player.getMainHandler();
            LoadControl loadControl = new DefaultLoadControl(new DefaultAllocator(BUFFER_SEGMENT_SIZE));
            DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter(mainHandler, player);

            // Check drm support if necessary.
            DrmSessionManager drmSessionManager = null;
            if (manifest.protectionElement != null) {
                if (Util.SDK_INT < 18) {
                    player.onRenderersError(
                            new UnsupportedDrmException(UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME));
                    return;
                }
                try {
                    drmSessionManager = new StreamingDrmSessionManager(manifest.protectionElement.uuid,
                            player.getPlaybackLooper(), null, null, player.getMainHandler(), player);
                } catch (UnsupportedDrmException e) {
                    player.onRenderersError(e);
                    return;
                }
            }

            // Build the video renderer.
            DataSource videoDataSource = new DefaultUriDataSource(context, bandwidthMeter, userAgent);
            ChunkSource videoChunkSource = new SmoothStreamingChunkSource(manifestFetcher,
                    new DefaultSmoothStreamingTrackSelector(context, StreamElement.TYPE_VIDEO),
                    videoDataSource, new AdaptiveEvaluator(bandwidthMeter), LIVE_EDGE_LATENCY_MS);
            ChunkSampleSource videoSampleSource = new ChunkSampleSource(videoChunkSource, loadControl,
                    VIDEO_BUFFER_SEGMENTS * BUFFER_SEGMENT_SIZE, mainHandler, player,
                    EMExoPlayer.RENDER_VIDEO_INDEX);
            TrackRenderer videoRenderer = new MediaCodecVideoTrackRenderer(context, videoSampleSource,
                    MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT, 5000, drmSessionManager, true, mainHandler,
                    player, 50);

            // Build the audio renderer.
            DataSource audioDataSource = new DefaultUriDataSource(context, bandwidthMeter, userAgent);
            ChunkSource audioChunkSource = new SmoothStreamingChunkSource(manifestFetcher,
                    new DefaultSmoothStreamingTrackSelector(context, StreamElement.TYPE_AUDIO),
                    audioDataSource, null, LIVE_EDGE_LATENCY_MS);
            ChunkSampleSource audioSampleSource = new ChunkSampleSource(audioChunkSource, loadControl,
                    AUDIO_BUFFER_SEGMENTS * BUFFER_SEGMENT_SIZE, mainHandler, player,
                    EMExoPlayer.RENDER_AUDIO_INDEX);
            TrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(audioSampleSource,
                    drmSessionManager, true, mainHandler, player, AudioCapabilities.getCapabilities(context));

            // Build the text renderer.
            DataSource textDataSource = new DefaultUriDataSource(context, bandwidthMeter, userAgent);
            ChunkSource textChunkSource = new SmoothStreamingChunkSource(manifestFetcher,
                    new DefaultSmoothStreamingTrackSelector(context, StreamElement.TYPE_TEXT),
                    textDataSource, null, LIVE_EDGE_LATENCY_MS);
            ChunkSampleSource textSampleSource = new ChunkSampleSource(textChunkSource, loadControl,
                    TEXT_BUFFER_SEGMENTS * BUFFER_SEGMENT_SIZE, mainHandler, player,
                    EMExoPlayer.RENDER_CLOSED_CAPTION_INDEX);
            TrackRenderer textRenderer = new TextTrackRenderer(textSampleSource, player,
                    mainHandler.getLooper());

            // Invoke the callback.
            TrackRenderer[] renderers = new TrackRenderer[EMExoPlayer.RENDER_COUNT];
            renderers[EMExoPlayer.RENDER_VIDEO_INDEX] = videoRenderer;
            renderers[EMExoPlayer.RENDER_AUDIO_INDEX] = audioRenderer;
            renderers[EMExoPlayer.RENDER_CLOSED_CAPTION_INDEX] = textRenderer;
            player.onRenderers(renderers, bandwidthMeter);
        }

    }

}