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
package com.devbrackets.android.exomedia.builder;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.MediaCodec;
import android.os.Build;

import com.devbrackets.android.exomedia.exoplayer.EMExoPlayer;
import com.google.android.exoplayer.DefaultLoadControl;
import com.google.android.exoplayer.LoadControl;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecUtil;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.chunk.VideoFormatSelectorUtil;
import com.google.android.exoplayer.hls.HlsChunkSource;
import com.google.android.exoplayer.hls.HlsMasterPlaylist;
import com.google.android.exoplayer.hls.HlsPlaylist;
import com.google.android.exoplayer.hls.HlsPlaylistParser;
import com.google.android.exoplayer.hls.HlsSampleSource;
import com.google.android.exoplayer.metadata.Id3Parser;
import com.google.android.exoplayer.metadata.MetadataTrackRenderer;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;
import com.google.android.exoplayer.util.ManifestFetcher;
import com.google.android.exoplayer.util.ManifestFetcher.ManifestCallback;

import java.io.IOException;
import java.util.Map;

/**
 * A RenderBuilder for parsing and creating the renderers for
 * Http Live Streams (HLS).
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class HlsRenderBuilder extends RenderBuilder implements ManifestCallback<HlsPlaylist> {

    private static final int BUFFER_SEGMENT_SIZE = 256 * 1024;
    private static final int BUFFER_SEGMENTS = 64;

    private EMExoPlayer player;

    public HlsRenderBuilder(Context context, String userAgent, String url) {
        super(context, userAgent, url);
    }

    @Override
    public void buildRenderers(EMExoPlayer player) {
        this.player = player;
        HlsPlaylistParser parser = new HlsPlaylistParser();
        ManifestFetcher<HlsPlaylist> playlistFetcher = new ManifestFetcher<>(uri, new DefaultUriDataSource(context, null, userAgent, true), parser);
        playlistFetcher.singleLoad(player.getMainHandler().getLooper(), this);
    }

    @Override
    public void onSingleManifestError(IOException e) {
        player.onRenderersError(e);
    }

    @Override
    public void onSingleManifest(HlsPlaylist playlist) {
        LoadControl loadControl = new DefaultLoadControl(new DefaultAllocator(BUFFER_SEGMENT_SIZE));
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter(player.getMainHandler(), player);

        //Calculates the Chunk variant indices
        int[] variantIndices = null;
        if (playlist instanceof HlsMasterPlaylist) {
            HlsMasterPlaylist masterPlaylist = (HlsMasterPlaylist) playlist;

            try {
                variantIndices = VideoFormatSelectorUtil.selectVideoFormatsForDefaultDisplay(context, masterPlaylist.variants, null, false);
            } catch (MediaCodecUtil.DecoderQueryException e) {
                player.onRenderersError(e);
                return;
            }
        }

        //Create the Sample Source to be used by the renders
        DataSource dataSource = new DefaultUriDataSource(context, bandwidthMeter, userAgent, true);
        HlsChunkSource chunkSource = new HlsChunkSource(dataSource, uri, playlist, bandwidthMeter,
                variantIndices, HlsChunkSource.ADAPTIVE_MODE_SPLICE);

        HlsSampleSource sampleSource = new HlsSampleSource(chunkSource, loadControl,
                BUFFER_SEGMENTS * BUFFER_SEGMENT_SIZE, player.getMainHandler(), player, EMExoPlayer.RENDER_VIDEO_INDEX);

        //Create the renderers
        MediaCodecVideoTrackRenderer videoRenderer = new MediaCodecVideoTrackRenderer(context, sampleSource,
                MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT, MAX_JOIN_TIME, player.getMainHandler(), player, DROPPED_FRAME_NOTIFICATION_AMOUNT);

        MediaCodecAudioTrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(sampleSource);

        MetadataTrackRenderer<Map<String, Object>> id3Renderer = new MetadataTrackRenderer<>(sampleSource, new Id3Parser(),
                        player, player.getMainHandler().getLooper());


        //Populate the Render list to pass back to the callback
        TrackRenderer[] renderers = new TrackRenderer[EMExoPlayer.RENDER_COUNT];
        renderers[EMExoPlayer.RENDER_VIDEO_INDEX] = videoRenderer;
        renderers[EMExoPlayer.RENDER_AUDIO_INDEX] = audioRenderer;
        renderers[EMExoPlayer.RENDER_TIMED_METADATA_INDEX] = id3Renderer;
        player.onRenderers(renderers, bandwidthMeter);
    }

}