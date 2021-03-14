/*
 * Copyright (C) 2017 - 2021 ExoMedia Contributors
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

import android.content.Context
import android.os.Handler
import com.devbrackets.android.exomedia.core.renderer.RenderProvider
import com.devbrackets.android.exomedia.core.renderer.RendererType
import com.google.android.exoplayer2.Renderer
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector
import com.google.android.exoplayer2.video.MediaCodecVideoRenderer
import com.google.android.exoplayer2.video.VideoRendererEventListener

class VideoRenderProvider(
    private val context: Context,
    private val handler: Handler,
    private val videoRendererEventListener: VideoRendererEventListener,
    private val droppedFrameNotificationAmount: Int = 50,
    private val videoJoiningTimeMs: Long = 5_000L
): RenderProvider {
  override fun type() = RendererType.VIDEO

  override fun rendererClasses(): List<String> {
    return listOf(
        "com.google.android.exoplayer2.ext.vp9.LibvpxVideoRenderer"
    )
  }

  override fun buildRenderers(): List<Renderer> {
    val renderers = mutableListOf<Renderer>()
    renderers.add(MediaCodecVideoRenderer(context, MediaCodecSelector.DEFAULT, videoJoiningTimeMs, handler, videoRendererEventListener, droppedFrameNotificationAmount))

    // Adds any registered classes
    rendererClasses().forEach { className ->
      try {
        val clazz = Class.forName(className)
        val constructor = clazz.getConstructor(Boolean::class.javaPrimitiveType, Long::class.javaPrimitiveType, Handler::class.java, VideoRendererEventListener::class.java, Int::class.javaPrimitiveType)
        val renderer = constructor.newInstance(true, videoJoiningTimeMs, handler, videoRendererEventListener, droppedFrameNotificationAmount) as Renderer
        renderers.add(renderer)
      } catch (e: Exception) {
        // Purposefully left blank
      }
    }

    return renderers
  }
}