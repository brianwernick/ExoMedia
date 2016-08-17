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
import android.media.MediaCodec;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.devbrackets.android.exomedia.core.exoplayer.EMExoPlayer;
import com.devbrackets.android.exomedia.core.renderer.EMMediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.DefaultLoadControl;
import com.google.android.exoplayer.LoadControl;
import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.SampleSource;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.drm.MediaDrmCallback;
import com.google.android.exoplayer.hls.DefaultHlsTrackSelector;
import com.google.android.exoplayer.hls.HlsChunkSource;
import com.google.android.exoplayer.hls.HlsMasterPlaylist;
import com.google.android.exoplayer.hls.HlsPlaylist;
import com.google.android.exoplayer.hls.HlsPlaylistParser;
import com.google.android.exoplayer.hls.HlsSampleSource;
import com.google.android.exoplayer.hls.PtsTimestampAdjusterProvider;
import com.google.android.exoplayer.metadata.MetadataTrackRenderer;
import com.google.android.exoplayer.metadata.id3.Id3Frame;
import com.google.android.exoplayer.metadata.id3.Id3Parser;
import com.google.android.exoplayer.text.TextTrackRenderer;
import com.google.android.exoplayer.text.eia608.Eia608TrackRenderer;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;
import com.google.android.exoplayer.upstream.UriDataSource;
import com.google.android.exoplayer.util.ManifestFetcher;
import com.google.android.exoplayer.util.ManifestFetcher.ManifestCallback;

import java.io.IOException;
import java.util.List;

/**
 * A RenderBuilder for parsing and creating the renderers for
 * Http Live Streams (HLS).
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class HlsRenderBuilder extends RenderBuilder {

    public HlsRenderBuilder(@NonNull Context context, @NonNull String userAgent, @NonNull String uri) {
        super(context, userAgent, uri);
    }

    public HlsRenderBuilder(@NonNull Context context, @NonNull String userAgent, @NonNull String uri, int streamType) {
        super(context, userAgent, uri, streamType);
    }

    public HlsRenderBuilder(@NonNull Context context, @NonNull String userAgent, @NonNull String uri, @Nullable MediaDrmCallback drmCallback, int streamType) {
        super(context, userAgent, uri, drmCallback, streamType);
    }

    @Override
    protected AsyncBuilder createAsyncBuilder(EMExoPlayer player) {
        return new AsyncHlsBuilder(context, userAgent, uri, drmCallback, player, streamType);
    }

    protected UriDataSource createManifestDataSource(Context context, String userAgent) {
        return new DefaultUriDataSource(context, userAgent);
    }

    protected class AsyncHlsBuilder extends AsyncBuilder implements ManifestCallback<HlsPlaylist> {
        protected final ManifestFetcher<HlsPlaylist> playlistFetcher;

        public AsyncHlsBuilder(Context context, String userAgent, String url, @Nullable MediaDrmCallback drmCallback, EMExoPlayer player, int streamType) {
            super(context, userAgent, url, drmCallback, player, streamType);

            HlsPlaylistParser parser = new HlsPlaylistParser();
            playlistFetcher = new ManifestFetcher<>(url, createManifestDataSource(context, userAgent), parser);
        }

        @Override
        public void init() {
            playlistFetcher.singleLoad(player.getMainHandler().getLooper(), this);
        }

        @Override
        public void onSingleManifestError(IOException e) {
            if (canceled) {
                return;
            }

            player.onRenderersError(e);
        }

        @Override
        public void onSingleManifest(HlsPlaylist playlist) {
            if (canceled) {
                return;
            }

            buildRenderers(playlist);
        }

        protected void buildRenderers(HlsPlaylist playlist) {
            if (canceled) {
                return;
            }

            boolean hasClosedCaptions = false;
            boolean hasMultipleAudioTracks = false;

            //Calculates the Chunk variant indices
            int[] variantIndices;
            if (playlist instanceof HlsMasterPlaylist) {
                HlsMasterPlaylist masterPlaylist = (HlsMasterPlaylist) playlist;
                hasClosedCaptions = !masterPlaylist.subtitles.isEmpty();
                hasMultipleAudioTracks = !masterPlaylist.audios.isEmpty();
            }

            buildTrackRenderers(playlist, hasClosedCaptions, hasMultipleAudioTracks);
        }

        protected void buildTrackRenderers(HlsPlaylist playlist, boolean hasClosedCaptions, boolean hasMultipleAudioTracks) {
            LoadControl loadControl = new DefaultLoadControl(new DefaultAllocator(BUFFER_SEGMENT_SIZE));
            DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter(player.getMainHandler(), player);
            PtsTimestampAdjusterProvider timestampAdjusterProvider = new PtsTimestampAdjusterProvider();


            //Create the Sample Source to be used by the Video Renderer
            DataSource dataSourceVideo = createDataSource(context, bandwidthMeter, userAgent);
            HlsChunkSource chunkSourceVideo = new HlsChunkSource(true, dataSourceVideo, playlist, DefaultHlsTrackSelector.newDefaultInstance(context),
                    bandwidthMeter, timestampAdjusterProvider);
            HlsSampleSource sampleSourceVideo = new HlsSampleSource(chunkSourceVideo, loadControl,
                    BUFFER_SEGMENTS_TOTAL * BUFFER_SEGMENT_SIZE, player.getMainHandler(), player, EMExoPlayer.RENDER_VIDEO);


            //Create the Sample Source to be used by the Audio Renderer
            DataSource dataSourceAudio = new DefaultUriDataSource(context, bandwidthMeter, userAgent);
            HlsChunkSource chunkSourceAudio = new HlsChunkSource(false, dataSourceAudio, playlist, DefaultHlsTrackSelector.newAudioInstance(),
                    bandwidthMeter, timestampAdjusterProvider);
            HlsSampleSource sampleSourceAudio = new HlsSampleSource(chunkSourceAudio, loadControl, BUFFER_SEGMENTS_AUDIO * BUFFER_SEGMENT_SIZE,
                    player.getMainHandler(), player, EMExoPlayer.RENDER_AUDIO);
            SampleSource[] sampleSourcesAudio = hasMultipleAudioTracks ? new SampleSource[] {sampleSourceVideo, sampleSourceAudio} : new SampleSource[] {sampleSourceVideo};


            //Create the Sample Source to be used by the Closed Captions Renderer
            DataSource dataSourceCC = createDataSource(context, bandwidthMeter, userAgent);
            HlsChunkSource chunkSourceCC = new HlsChunkSource(false, dataSourceCC, playlist, DefaultHlsTrackSelector.newSubtitleInstance(),
                    bandwidthMeter, timestampAdjusterProvider);
            HlsSampleSource sampleSourceCC = new HlsSampleSource(chunkSourceCC, loadControl,
                    BUFFER_SEGMENTS_TEXT * BUFFER_SEGMENT_SIZE, player.getMainHandler(), player, EMExoPlayer.RENDER_CLOSED_CAPTION);


            //Build the renderers
            MediaCodecVideoTrackRenderer videoRenderer = new MediaCodecVideoTrackRenderer(context, sampleSourceVideo, MediaCodecSelector.DEFAULT,
                    MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT, MAX_JOIN_TIME, player.getMainHandler(), player, DROPPED_FRAME_NOTIFICATION_AMOUNT);
            EMMediaCodecAudioTrackRenderer audioRenderer = new EMMediaCodecAudioTrackRenderer(sampleSourcesAudio, MediaCodecSelector.DEFAULT, null, true,
                    player.getMainHandler(), player, AudioCapabilities.getCapabilities(context), streamType);
            TrackRenderer captionsRenderer = hasClosedCaptions ? new TextTrackRenderer(sampleSourceCC, player, player.getMainHandler().getLooper()) :
                    new Eia608TrackRenderer(sampleSourceVideo, player, player.getMainHandler().getLooper());
            MetadataTrackRenderer<List<Id3Frame>> id3Renderer = new MetadataTrackRenderer<>(sampleSourceVideo, new Id3Parser(),
                    player, player.getMainHandler().getLooper());


            //Populate the Render list to pass back to the callback
            TrackRenderer[] renderers = new TrackRenderer[EMExoPlayer.RENDER_COUNT];
            renderers[EMExoPlayer.RENDER_VIDEO] = videoRenderer;
            renderers[EMExoPlayer.RENDER_AUDIO] = audioRenderer;
            renderers[EMExoPlayer.RENDER_CLOSED_CAPTION] = captionsRenderer;
            renderers[EMExoPlayer.RENDER_TIMED_METADATA] = id3Renderer;
            player.onRenderers(renderers, bandwidthMeter);
        }
    }
}