/*
 * Copyright (C) 2016 Brian Wernick,
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

package com.devbrackets.android.exomedia.core.listener;

import com.google.android.exoplayer.TimeRange;
import com.google.android.exoplayer.chunk.Format;

/**
 * A listener for debugging EMExoPlayer information.
 */
public interface InfoListener {
    void onVideoFormatEnabled(Format format, int trigger, long mediaTimeMs);

    void onAudioFormatEnabled(Format format, int trigger, long mediaTimeMs);

    void onDroppedFrames(int count, long elapsed);

    void onBandwidthSample(int elapsedMs, long bytes, long bitrateEstimate);

    void onLoadStarted(int sourceId, long length, int type, int trigger, Format format, long mediaStartTimeMs, long mediaEndTimeMs);

    void onLoadCompleted(int sourceId, long bytesLoaded, int type, int trigger, Format format, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs);

    void onDecoderInitialized(String decoderName, long elapsedRealtimeMs, long initializationDurationMs);

    void onAvailableRangeChanged(int sourceId, TimeRange availableRange);
}
