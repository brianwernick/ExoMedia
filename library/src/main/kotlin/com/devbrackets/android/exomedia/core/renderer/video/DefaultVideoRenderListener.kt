/*
 * Copyright (C) 2015-2021 ExoMedia Contributors,
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

package com.devbrackets.android.exomedia.core.renderer.video

import com.devbrackets.android.exomedia.nmp.CorePlayerListeners
import com.google.android.exoplayer2.analytics.AnalyticsCollector
import com.google.android.exoplayer2.video.VideoRendererEventListener

class DefaultVideoRenderListener(
    private val coreListeners: CorePlayerListeners,
    private val delegate: AnalyticsCollector
): VideoRendererEventListener by delegate {
  override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
    coreListeners.videoSizeListener?.onVideoSizeChanged(width, height, unappliedRotationDegrees, pixelWidthHeightRatio)
    delegate.onVideoSizeChanged(width, height, unappliedRotationDegrees, pixelWidthHeightRatio)
  }
}