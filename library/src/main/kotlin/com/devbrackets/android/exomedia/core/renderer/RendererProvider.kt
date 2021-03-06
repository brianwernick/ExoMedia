/*
 * Copyright (C) 2017 - 2019 ExoMedia Contributors
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

package com.devbrackets.android.exomedia.core.renderer

import android.content.Context
import android.os.Handler
import com.devbrackets.android.exomedia.ExoMedia
import com.google.android.exoplayer2.Renderer
import com.google.android.exoplayer2.audio.AudioCapabilities
import com.google.android.exoplayer2.audio.AudioRendererEventListener
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector
import com.google.android.exoplayer2.metadata.MetadataDecoderFactory
import com.google.android.exoplayer2.metadata.MetadataOutput
import com.google.android.exoplayer2.metadata.MetadataRenderer
import com.google.android.exoplayer2.text.TextOutput
import com.google.android.exoplayer2.text.TextRenderer
import com.google.android.exoplayer2.video.MediaCodecVideoRenderer
import com.google.android.exoplayer2.video.VideoRendererEventListener
import java.util.*

/**
 * Provides all the necessary [com.google.android.exoplayer2.Renderer]s
 */
open class RendererProvider(
    protected var context: Context,
    protected var handler: Handler,
    protected var captionListener: TextOutput,
    protected var metadataListener: MetadataOutput,
    protected var audioRendererEventListener: AudioRendererEventListener,
    protected var videoRendererEventListener: VideoRendererEventListener
) {

  var droppedFrameNotificationAmount: Int = 50
  var videoJoiningTimeMs: Int = 5_000

  fun generate(): List<Renderer> {
    return mutableListOf<Renderer>().apply {
      addAll(buildAudioRenderers())
      addAll(buildVideoRenderers())
      addAll(buildCaptionRenderers())
      addAll(buildMetadataRenderers())
    }
  }

  protected fun buildAudioRenderers(): List<Renderer> {
    val renderers = mutableListOf<Renderer>()
    renderers.add(MediaCodecAudioRenderer(context, MediaCodecSelector.DEFAULT, handler, audioRendererEventListener, AudioCapabilities.getCapabilities(context)))

    // Adds any registered classes
    val classNames = ExoMedia.Data.registeredRendererClasses[ExoMedia.RendererType.AUDIO]
    classNames?.forEach { className ->
      try {
        val clazz = Class.forName(className)
        val constructor = clazz.getConstructor(Handler::class.java, AudioRendererEventListener::class.java)
        val renderer = constructor.newInstance(handler, audioRendererEventListener) as Renderer
        renderers.add(renderer)
      } catch (e: Exception) {
        // Purposefully left blank
      }
    }

    return renderers
  }

  protected fun buildVideoRenderers(): List<Renderer> {
    val renderers = mutableListOf<Renderer>()
    renderers.add(MediaCodecVideoRenderer(context, MediaCodecSelector.DEFAULT, videoJoiningTimeMs.toLong(), handler, videoRendererEventListener, droppedFrameNotificationAmount))

    // Adds any registered classes
    val classNames = ExoMedia.Data.registeredRendererClasses[ExoMedia.RendererType.VIDEO]
    classNames?.forEach { className ->
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

  protected fun buildCaptionRenderers(): List<Renderer> {
    return listOf(TextRenderer(captionListener, handler.looper))
  }

  protected fun buildMetadataRenderers(): List<Renderer> {
    return listOf(MetadataRenderer(metadataListener, handler.looper, MetadataDecoderFactory.DEFAULT))
  }
}
