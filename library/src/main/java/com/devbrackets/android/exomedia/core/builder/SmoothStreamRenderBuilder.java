/*
 * Copyright (C) 2016 Brian Wernick,
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

package com.devbrackets.android.exomedia.core.builder;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaCodec;
import android.os.Build;
import android.os.Handler;
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
import com.google.android.exoplayer.chunk.FormatEvaluator;
import com.google.android.exoplayer.drm.DrmSessionManager;
import com.google.android.exoplayer.drm.StreamingDrmSessionManager;
import com.google.android.exoplayer.drm.UnsupportedDrmException;
import com.google.android.exoplayer.smoothstreaming.DefaultSmoothStreamingTrackSelector;
import com.google.android.exoplayer.smoothstreaming.SmoothStreamingChunkSource;
import com.google.android.exoplayer.smoothstreaming.SmoothStreamingManifest;
import com.google.android.exoplayer.smoothstreaming.SmoothStreamingManifestParser;
import com.google.android.exoplayer.smoothstreaming.SmoothStreamingTrackSelector;
import com.google.android.exoplayer.text.TextTrackRenderer;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer.upstream.UriDataSource;
import com.google.android.exoplayer.util.ManifestFetcher;
import com.google.android.exoplayer.util.Util;

import java.io.IOException;

/**
 * A RenderBuilder for parsing and creating the renderers for
 * Smooth Streaming streams.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class SmoothStreamRenderBuilder extends RenderBuilder {
    protected static final int LIVE_EDGE_LATENCY_MS = 30000;

    protected AsyncRendererBuilder currentAsyncBuilder;

    public SmoothStreamRenderBuilder(Context context, String userAgent, String url) {
        this(context, userAgent, url, AudioManager.STREAM_MUSIC);
    }

    public SmoothStreamRenderBuilder(Context context, String userAgent, String url, int streamType) {
        super(context, userAgent, getManifestUri(url), streamType);
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

    @SuppressWarnings("UnusedParameters") // Context kept for consistency with the HLS and Dash builders
    protected UriDataSource createManifestDataSource(Context context, String userAgent) {
        return new DefaultHttpDataSource(userAgent, null);
    }

    protected static String getManifestUri(String url) {
        return Util.toLowerInvariant(url).endsWith("/manifest") ? url : url + "/Manifest";
    }

    protected final class AsyncRendererBuilder implements ManifestFetcher.ManifestCallback<SmoothStreamingManifest> {
        protected final Context context;
        protected final String userAgent;
        protected final int streamType;
        protected final EMExoPlayer player;
        protected final ManifestFetcher<SmoothStreamingManifest> manifestFetcher;

        protected boolean canceled;

        public AsyncRendererBuilder(Context context, String userAgent, String url, EMExoPlayer player, int streamType) {
            this.context = context;
            this.userAgent = userAgent;
            this.streamType = streamType;
            this.player = player;
            SmoothStreamingManifestParser parser = new SmoothStreamingManifestParser();
            manifestFetcher = new ManifestFetcher<>(url, createManifestDataSource(null, userAgent), parser);
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

            // Check drm support if necessary.
            DrmSessionManager drmSessionManager = null;
            if (manifest.protectionElement != null) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    player.onRenderersError(new UnsupportedDrmException(UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME));
                    return;
                }

                try {
                    drmSessionManager = new StreamingDrmSessionManager(manifest.protectionElement.uuid, player.getPlaybackLooper(), null, null, player.getMainHandler(), player);
                } catch (UnsupportedDrmException e) {
                    player.onRenderersError(e);
                    return;
                }
            }

            buildRenderers(drmSessionManager);
        }

        protected void buildRenderers(DrmSessionManager drmSessionManager) {
            Handler mainHandler = player.getMainHandler();
            LoadControl loadControl = new DefaultLoadControl(new DefaultAllocator(BUFFER_SEGMENT_SIZE));
            DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter(mainHandler, player);


            //Create the Sample Source to be used by the Video Renderer
            DataSource dataSourceVideo = createDataSource(context, bandwidthMeter, userAgent);
            SmoothStreamingTrackSelector trackSelectorVideo = DefaultSmoothStreamingTrackSelector.newVideoInstance(context, true, false);
            ChunkSource chunkSourceVideo = new SmoothStreamingChunkSource(manifestFetcher, trackSelectorVideo, dataSourceVideo,
                    new FormatEvaluator.AdaptiveEvaluator(bandwidthMeter), LIVE_EDGE_LATENCY_MS);
            ChunkSampleSource sampleSourceVideo = new ChunkSampleSource(chunkSourceVideo, loadControl, BUFFER_SEGMENTS_VIDEO * BUFFER_SEGMENT_SIZE,
                    mainHandler, player, EMExoPlayer.RENDER_VIDEO);


            //Create the Sample Source to be used by the Audio Renderer
            DataSource dataSourceAudio = createDataSource(context, bandwidthMeter, userAgent);
            SmoothStreamingTrackSelector trackSelectorAudio = DefaultSmoothStreamingTrackSelector.newAudioInstance();
            ChunkSource chunkSourceAudio = new SmoothStreamingChunkSource(manifestFetcher, trackSelectorAudio, dataSourceAudio, null, LIVE_EDGE_LATENCY_MS);
            ChunkSampleSource sampleSourceAudio = new ChunkSampleSource(chunkSourceAudio, loadControl, BUFFER_SEGMENTS_AUDIO * BUFFER_SEGMENT_SIZE,
                    mainHandler, player, EMExoPlayer.RENDER_AUDIO);


            //Create the Sample Source to be used by the Closed Captions Renderer
            DataSource dataSourceCC = createDataSource(context, bandwidthMeter, userAgent);
            SmoothStreamingTrackSelector trackSelectorCC = DefaultSmoothStreamingTrackSelector.newTextInstance();
            ChunkSource chunkSourceCC = new SmoothStreamingChunkSource(manifestFetcher, trackSelectorCC, dataSourceCC, null, LIVE_EDGE_LATENCY_MS);
            ChunkSampleSource sampleSourceCC = new ChunkSampleSource(chunkSourceCC, loadControl, BUFFER_SEGMENTS_TEXT * BUFFER_SEGMENT_SIZE,
                    mainHandler, player, EMExoPlayer.RENDER_CLOSED_CAPTION);


            // Build the renderers
            MediaCodecVideoTrackRenderer videoRenderer = new MediaCodecVideoTrackRenderer(context, sampleSourceVideo, MediaCodecSelector.DEFAULT,
                    MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT, MAX_JOIN_TIME, drmSessionManager, true, mainHandler, player, DROPPED_FRAME_NOTIFICATION_AMOUNT);
            EMMediaCodecAudioTrackRenderer audioRenderer = new EMMediaCodecAudioTrackRenderer(sampleSourceAudio, MediaCodecSelector.DEFAULT, drmSessionManager,
                    true, mainHandler, player, AudioCapabilities.getCapabilities(context), streamType);
            TextTrackRenderer captionsRenderer = new TextTrackRenderer(sampleSourceCC, player, mainHandler.getLooper());


            // Invoke the callback
            TrackRenderer[] renderers = new TrackRenderer[EMExoPlayer.RENDER_COUNT];
            renderers[EMExoPlayer.RENDER_VIDEO] = videoRenderer;
            renderers[EMExoPlayer.RENDER_AUDIO] = audioRenderer;
            renderers[EMExoPlayer.RENDER_CLOSED_CAPTION] = captionsRenderer;
            player.onRenderers(renderers, bandwidthMeter);
        }
    }
}