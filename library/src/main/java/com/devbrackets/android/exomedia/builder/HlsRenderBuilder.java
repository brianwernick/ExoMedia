/*
 * Copyright (C) 2015 Brian Wernick,
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
import android.media.MediaCodec;
import android.os.Build;
import android.util.Log;

import com.devbrackets.android.exomedia.exoplayer.EMExoPlayer;
import com.devbrackets.android.exomedia.listener.RendererBuilderCallback;
import com.devbrackets.android.exomedia.renderer.EMMediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecUtil;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.hls.HlsChunkSource;
import com.google.android.exoplayer.hls.HlsPlaylist;
import com.google.android.exoplayer.hls.HlsPlaylistParser;
import com.google.android.exoplayer.hls.HlsSampleSource;
import com.google.android.exoplayer.metadata.Id3Parser;
import com.google.android.exoplayer.metadata.MetadataTrackRenderer;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.upstream.UriDataSource;
import com.google.android.exoplayer.util.ManifestFetcher;
import com.google.android.exoplayer.util.ManifestFetcher.ManifestCallback;
import com.google.android.exoplayer.util.MimeTypes;

import java.io.IOException;
import java.util.Map;

/**
 * A RenderBuilder for parsing and creating the renderers for
 * Http Live Streams (HLS).
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class HlsRenderBuilder extends RenderBuilder implements ManifestCallback<HlsPlaylist> {
    private static final String TAG = HlsRenderBuilder.class.getSimpleName();
    private static final int DOWNSTREAM_RENDER_COUNT = 3;

    private final String userAgent;
    private final String url;
    private final String contentId;

    private EMExoPlayer player;
    private RendererBuilderCallback callback;

    public HlsRenderBuilder(String userAgent, String url, String contentId) {
        super();
        this.userAgent = userAgent;
        this.url = url;
        this.contentId = contentId;
    }

    @Override
    public void buildRenderers(EMExoPlayer player, RendererBuilderCallback callback) {
        this.player = player;
        this.callback = callback;

        HlsPlaylistParser parser = new HlsPlaylistParser();
        ManifestFetcher<HlsPlaylist> playlistFetcher = new ManifestFetcher<>(parser, contentId, url, userAgent);
        playlistFetcher.singleLoad(player.getMainHandler().getLooper(), this);
    }

    @Override
    public void onManifestError(String contentId, IOException e) {
        callback.onRenderersError(e);
    }

    @Override
    public void onManifest(String contentId, HlsPlaylist manifest) {
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();

        //Create the Sample Source to be used by the renderers
        DataSource dataSource = new UriDataSource(userAgent, bandwidthMeter);
        boolean adaptiveDecoder = false;

        try {
            adaptiveDecoder = MediaCodecUtil.getDecoderInfo(MimeTypes.VIDEO_H264, false).adaptive;
        } catch (MediaCodecUtil.DecoderQueryException dqe) {
            Log.d(TAG, "Unable to determine adaptive availability.  Defaulting to false", dqe);
        }

        HlsChunkSource chunkSource = new HlsChunkSource(dataSource, url, manifest, bandwidthMeter, null,
                adaptiveDecoder ? HlsChunkSource.ADAPTIVE_MODE_SPLICE : HlsChunkSource.ADAPTIVE_MODE_NONE);

        HlsSampleSource sampleSource = new HlsSampleSource(chunkSource, true, DOWNSTREAM_RENDER_COUNT);

        //Create the renderers
        MediaCodecVideoTrackRenderer videoRenderer = new MediaCodecVideoTrackRenderer(sampleSource,
                MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT, 0, player.getMainHandler(), player, DROPPED_FRAME_NOTIFICATION_AMOUNT);

        EMMediaCodecAudioTrackRenderer audioRenderer = new EMMediaCodecAudioTrackRenderer(sampleSource);

        MetadataTrackRenderer<Map<String, Object>> id3Renderer =
                new MetadataTrackRenderer<>(sampleSource, new Id3Parser(), player.getId3MetadataRenderer(), player.getMainHandler().getLooper());


        //Populate the Render list to pass back to the callback
        TrackRenderer[] renderers = new TrackRenderer[EMExoPlayer.RENDER_COUNT];
        renderers[EMExoPlayer.RENDER_VIDEO_INDEX] = videoRenderer;
        renderers[EMExoPlayer.RENDER_AUDIO_INDEX] = audioRenderer;
        renderers[EMExoPlayer.RENDER_TIMED_METADATA_INDEX] = id3Renderer;
        callback.onRenderers(null, null, renderers);
    }
}