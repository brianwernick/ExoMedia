/*
 * Copyright (C) 2016 - 2021 ExoMedia Contributors
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

package com.devbrackets.android.exomedia.ui.widget.attr

import com.devbrackets.android.exomedia.core.video.scale.ScaleType
import com.devbrackets.android.exomedia.nmp.config.DefaultPlayerConfigProvider
import com.devbrackets.android.exomedia.nmp.config.PlayerConfigProvider

data class VideoViewAttributes(
    /**
     * Specifies if the [DefaultVideoControls] should be added to the view.  These
     * can be added through source code with [.setControls]
     */
    val useDefaultControls: Boolean = false,

    /**
     * Specifies if the [VideoViewApi] implementations should use the [android.view.TextureView]
     * implementations.  If this is false then the implementations will be based on
     * the [android.view.SurfaceView]
     */
    val useTextureViewBacking: Boolean = false,

    /**
     * Specifies the scale that the [VideoView] should use. If this is `null`
     * then the default value from the [com.devbrackets.android.exomedia.core.video.scale.MatrixManager]
     * will be used.
     */
    val scaleType: ScaleType? = null,

    /**
     * Specifies if the [VideoView] should be measured based on the aspect ratio. Because
     * the default value is different between the [com.devbrackets.android.exomedia.core.video.ResizingSurfaceView]
     * and [com.devbrackets.android.exomedia.core.video.ResizingTextureView] this will be `null`
     * when not specified.
     */
    val measureBasedOnAspectRatio: Boolean? = null,

    /**
     * Specifies the provider to use when fetching the configuration for the
     * media playback.
     */
    val playerConfigProvider: PlayerConfigProvider = DefaultPlayerConfigProvider()
)