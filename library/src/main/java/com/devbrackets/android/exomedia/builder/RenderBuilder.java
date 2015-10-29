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
import android.util.Log;

import com.devbrackets.android.exomedia.exoplayer.EMExoPlayer;
import com.devbrackets.android.exomedia.renderer.EMMediaCodecAudioTrackRenderer;
import com.devbrackets.android.exomedia.util.MediaUtil;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.extractor.Extractor;
import com.google.android.exoplayer.extractor.ExtractorSampleSource;
import com.google.android.exoplayer.extractor.mp3.Mp3Extractor;
import com.google.android.exoplayer.extractor.mp4.FragmentedMp4Extractor;
import com.google.android.exoplayer.extractor.mp4.Mp4Extractor;
import com.google.android.exoplayer.extractor.ts.AdtsExtractor;
import com.google.android.exoplayer.extractor.ts.TsExtractor;
import com.google.android.exoplayer.extractor.webm.WebmExtractor;
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
    private static final String TAG = RenderBuilder.class.getSimpleName();
    protected static final int DROPPED_FRAME_NOTIFICATION_AMOUNT = 50;

    protected static final long MAX_JOIN_TIME = 5000;
    private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
    private static final int BUFFER_SEGMENTS = 160;

    protected final Context context;
    protected final String userAgent;
    protected final String uri;
    protected MediaUtil.MediaType requestedDefaultType;

    public RenderBuilder(Context context, String userAgent, String uri) {
        this.uri = uri;
        this.userAgent = userAgent;
        this.context = context;
    }

    public RenderBuilder(Context context, String userAgent, String uri, MediaUtil.MediaType defaultType) {
        this(context, userAgent, uri);
        this.requestedDefaultType = defaultType;
    }

    public void buildRenderers(EMExoPlayer player) {
        //Create the Sample Source to be used by the renderers
        Allocator allocator = new DefaultAllocator(BUFFER_SEGMENT_SIZE);
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter(player.getMainHandler(), player);
        DataSource dataSource = new DefaultUriDataSource(context, bandwidthMeter, userAgent, true);

        ExtractorSampleSource sampleSource = new ExtractorSampleSource(Uri.parse(MediaUtil.getUriWithProtocol(uri)), dataSource,
               allocator, BUFFER_SEGMENT_SIZE * BUFFER_SEGMENTS, getExtractor(uri, requestedDefaultType));

        //Create the Renderers
        MediaCodecVideoTrackRenderer videoRenderer = new MediaCodecVideoTrackRenderer(context, sampleSource, MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT,
                MAX_JOIN_TIME, null, true, player.getMainHandler(), player, DROPPED_FRAME_NOTIFICATION_AMOUNT);

        EMMediaCodecAudioTrackRenderer audioRenderer = new EMMediaCodecAudioTrackRenderer(sampleSource, null, true, player.getMainHandler(), player);


        //Create the Render list to send to the callback
        TrackRenderer[] renderers = new TrackRenderer[EMExoPlayer.RENDER_COUNT];
        renderers[EMExoPlayer.RENDER_VIDEO_INDEX] = videoRenderer;
        renderers[EMExoPlayer.RENDER_AUDIO_INDEX] = audioRenderer;
        player.onRenderers(renderers, bandwidthMeter);
    }

    public void cancel() {
        //Purposefully left blank
    }

    /**
     * Retrieves the extractor to use with the specified mediaUri
     *
     * @param mediaUri The uri to get the extractor for
     * @return The Extractor for the specified mediaUri
     */
    protected Extractor getExtractor(String mediaUri) {
        return getExtractor(mediaUri, MediaUtil.MediaType.UNKNOWN);
    }

    /**
     * Retrieves the extractor to use with the specified mediaUri
     *
     * @param mediaUri The uri to get the extractor for
     * @param defaultType The media type to use if we can't determine the type
     * @return The Extractor for the specified mediaUri
     */
    protected Extractor getExtractor(String mediaUri, MediaUtil.MediaType defaultType) {
        return getExtractor(MediaUtil.getMediaType(mediaUri), defaultType);
    }

    /**
     * Retrieves the extractor for the requested type.  If the extractor
     * is unavailable then the default extractor will be returned.
     *
     * @param mediaType The media type to get the extractor for
     * @return The extractor for the <code>mediaType</code>
     */
    protected Extractor getExtractor(MediaUtil.MediaType mediaType) {
        return getExtractor(mediaType, MediaUtil.MediaType.UNKNOWN);
    }

    /**
     * Retrieves the extractor for the requested type.  If the extractor
     * is unavailable then the extractor for <code>defaultType</code> will be returned.
     *
     * @param mediaType The media type to get the extractor for
     * @param defaultType The media type to use if the requested type is unavailable
     * @return The extractor for the <code>mediaType</code>
     */
    protected Extractor getExtractor(MediaUtil.MediaType mediaType, MediaUtil.MediaType defaultType) {
        boolean canUseRequestedType = defaultType != null && defaultType != MediaUtil.MediaType.UNKNOWN;

        // NOTE: this is based on the demo project for the ExoPlayer, found at
        // https://github.com/google/ExoPlayer/blob/888d9db3e92bf4605c7be5cf61da52db0c75bdee/demo/src/main/java/com/google/android/exoplayer/demo/PlayerActivity.java#L222
        switch (mediaType) {
            case AAC:
                return new AdtsExtractor();

            case FMP4:
                return new FragmentedMp4Extractor();

            case M4A:
            case MP4:
                return new Mp4Extractor();

            case MP3:
                return new Mp3Extractor();

            case TS:
                return new TsExtractor();

            case WEBM:
            case MKV:
                return new WebmExtractor();

            default:
            case UNKNOWN:
                Log.d(TAG, "Unable to determine extractor for the uri \"" + uri + "\", assuming " + (canUseRequestedType ? requestedDefaultType : "MP4"));
                return canUseRequestedType ? getExtractor(defaultType) : new Mp4Extractor();
        }
    }
}