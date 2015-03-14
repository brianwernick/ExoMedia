/*
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
import android.net.Uri;
import android.os.Build;

import com.devbrackets.android.exomedia.exoplayer.EMExoPlayer;
import com.devbrackets.android.exomedia.listener.RendererBuilderCallback;
import com.devbrackets.android.exomedia.renderer.EMMediaCodecAudioTrackRenderer;
import com.devbrackets.android.exomedia.util.MediaUtil;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.source.DefaultSampleSource;
import com.google.android.exoplayer.source.FrameworkSampleExtractor;

/**
 * A default RenderBuilder that can process Http:// URIs and file:// URI's
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class RenderBuilder {
    public static final int DROPPED_FRAME_NOTIFICATION_AMOUNT = 50;

    private final Context context;
    private final Uri uri;

    public RenderBuilder() {
        uri = null;
        context = null;
    }

    public RenderBuilder(Context context, String uri) {
        this.uri = Uri.parse(MediaUtil.getUriWithProtocol(uri));
        this.context = context;
    }

    public void buildRenderers(EMExoPlayer player, RendererBuilderCallback callback) {
        DefaultSampleSource sampleSource = new DefaultSampleSource(new FrameworkSampleExtractor(context, uri, null), 2);

        //Create the renderers
        MediaCodecVideoTrackRenderer videoRenderer = new MediaCodecVideoTrackRenderer(sampleSource, null, true, MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT,
                0, null, player.getMainHandler(), player, DROPPED_FRAME_NOTIFICATION_AMOUNT);

        EMMediaCodecAudioTrackRenderer audioRenderer = new EMMediaCodecAudioTrackRenderer(sampleSource, null, true, player.getMainHandler(), player);

        //Create the Render list to send to the callback
        TrackRenderer[] renderers = new TrackRenderer[EMExoPlayer.RENDER_COUNT];
        renderers[EMExoPlayer.RENDER_VIDEO_INDEX] = videoRenderer;
        renderers[EMExoPlayer.RENDER_AUDIO_INDEX] = audioRenderer;
        callback.onRenderers(null, null, renderers);
    }
}