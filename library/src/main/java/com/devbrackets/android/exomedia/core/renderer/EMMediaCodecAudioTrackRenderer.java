/*
 * Copyright (C) 2016 Brian Wernick
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

package com.devbrackets.android.exomedia.core.renderer;

import android.os.Handler;

import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.SampleSource;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.drm.DrmSessionManager;

/**
 * Extends the MediaCodecAudioTrackRenderer so that we can keep track of the audioSessionId
 */
public class EMMediaCodecAudioTrackRenderer extends MediaCodecAudioTrackRenderer {
    private int audioSessionId = 0;

    public EMMediaCodecAudioTrackRenderer(SampleSource source, MediaCodecSelector mediaCodecSelector) {
        super(source, mediaCodecSelector);
    }

    public EMMediaCodecAudioTrackRenderer(SampleSource source, MediaCodecSelector mediaCodecSelector, DrmSessionManager drmSessionManager, boolean playClearSamplesWithoutKeys) {
        super(source, mediaCodecSelector, drmSessionManager, playClearSamplesWithoutKeys);
    }

    public EMMediaCodecAudioTrackRenderer(SampleSource source, MediaCodecSelector mediaCodecSelector, Handler eventHandler, EventListener eventListener) {
        super(source, mediaCodecSelector, eventHandler, eventListener);
    }

    public EMMediaCodecAudioTrackRenderer(SampleSource source, MediaCodecSelector mediaCodecSelector, DrmSessionManager drmSessionManager, boolean playClearSamplesWithoutKeys, Handler eventHandler, EventListener eventListener) {
        super(source, mediaCodecSelector, drmSessionManager, playClearSamplesWithoutKeys, eventHandler, eventListener);
    }

    public EMMediaCodecAudioTrackRenderer(SampleSource source, MediaCodecSelector mediaCodecSelector, DrmSessionManager drmSessionManager, boolean playClearSamplesWithoutKeys, Handler eventHandler, EventListener eventListener, AudioCapabilities audioCapabilities, int streamType) {
        super(source, mediaCodecSelector, drmSessionManager, playClearSamplesWithoutKeys, eventHandler, eventListener, audioCapabilities, streamType);
    }

    @Override
    protected void onAudioSessionId(int audioSessionId) {
        this.audioSessionId = audioSessionId;
        super.onAudioSessionId(audioSessionId);
    }

    public int getAudioSessionId() {
        return audioSessionId;
    }

}
