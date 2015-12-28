/*
 * Copyright (C) 2015 Brian Wernick
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
import android.net.Uri;
import android.os.Build;

import com.devbrackets.android.exomedia.exoplayer.EMExoPlayer;
import com.devbrackets.android.exomedia.renderer.EMMediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.extractor.ExtractorSampleSource;
import com.google.android.exoplayer.text.TextTrackRenderer;
import com.google.android.exoplayer.upstream.Allocator;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;


/**
 * A default RenderBuilder that can process Http:// URIs and file:// URI's
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class RenderBuilder {

    private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
    private static final int BUFFER_SEGMENT_COUNT = 256;

    private final Context context;
    private final String userAgent;
    private final String uri;

    public RenderBuilder(Context context, String userAgent, String uri) {
        this.uri = uri;
        this.userAgent = userAgent;
        this.context = context;
    }

    public void buildRenderers(EMExoPlayer player) {
        //Create the Sample Source to be used by the renderers
        Allocator allocator = new DefaultAllocator(BUFFER_SEGMENT_SIZE);

        // Build the video and audio renderers.
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter(player.getMainHandler(),
                null);
        DataSource dataSource = new DefaultUriDataSource(context, bandwidthMeter, userAgent);
        ExtractorSampleSource sampleSource = new ExtractorSampleSource(Uri.parse(uri), dataSource, allocator,
                BUFFER_SEGMENT_COUNT * BUFFER_SEGMENT_SIZE);
        TrackRenderer videoRenderer = new MediaCodecVideoTrackRenderer(context,
                sampleSource, MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT, 5000, player.getMainHandler(),
                player, 50);
        TrackRenderer audioRenderer = new EMMediaCodecAudioTrackRenderer(sampleSource,
                null, true, player.getMainHandler(), player, AudioCapabilities.getCapabilities(context));
        TrackRenderer textRenderer = new TextTrackRenderer(sampleSource, player,
                player.getMainHandler().getLooper());


        //Create the Render list to send to the callback
        TrackRenderer[] renderers = new TrackRenderer[EMExoPlayer.RENDER_COUNT];
        renderers[EMExoPlayer.RENDER_VIDEO_INDEX] = videoRenderer;
        renderers[EMExoPlayer.RENDER_AUDIO_INDEX] = audioRenderer;
        renderers[EMExoPlayer.RENDER_CLOSED_CAPTION_INDEX] = textRenderer;
        player.onRenderers(renderers, bandwidthMeter);
    }

    public void cancel() {
        //Purposefully left blank
    }

}