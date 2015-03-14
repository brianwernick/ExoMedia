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

package com.devbrackets.android.exomedia.listener;

import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.chunk.MultiTrackChunkSource;

/**
 *
 */
public interface RendererBuilderCallback {
    /**
     * Invoked with the results from a {@link com.devbrackets.android.exomedia.builder.RenderBuilder}.
     *
     * @param trackNames        The names of the available tracks, indexed by {@link com.devbrackets.android.exomedia.exoplayer.EMExoPlayer} TYPE_*
     *                          constants. May be null if the track names are unknown. An individual element may be null
     *                          if the track names are unknown for the corresponding type.
     * @param multiTrackSources Sources capable of switching between multiple available tracks,
     *                          indexed by {@link com.devbrackets.android.exomedia.exoplayer.EMExoPlayer} TYPE_* constants. May be null if there are no types with
     *                          multiple tracks. An individual element may be null if it does not have multiple tracks.
     * @param renderers         Renderers indexed by {@link com.devbrackets.android.exomedia.exoplayer.EMExoPlayer} TYPE_* constants. An individual
     *                          element may be null if there do not exist tracks of the corresponding type.
     */
    void onRenderers(String[][] trackNames, MultiTrackChunkSource[] multiTrackSources, TrackRenderer[] renderers);

    /**
     * Invoked if a {@link com.devbrackets.android.exomedia.builder.RenderBuilder} encounters an error.
     *
     * @param e Describes the error.
     */
    void onRenderersError(Exception e);
}
