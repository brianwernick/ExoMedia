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

package com.devbrackets.android.exomedia.core.renderer.audio

import android.content.Context
import android.os.Handler
import com.devbrackets.android.exomedia.core.renderer.RenderProvider
import com.devbrackets.android.exomedia.core.renderer.RendererType
import com.google.android.exoplayer2.Renderer
import com.google.android.exoplayer2.audio.AudioCapabilities
import com.google.android.exoplayer2.audio.AudioRendererEventListener
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector

open class AudioRenderProvider(
    private val context: Context,
    private val handler: Handler,
    private val audioRendererEventListener: AudioRendererEventListener
): RenderProvider {
  override fun type() = RendererType.AUDIO

  override fun rendererClasses(): List<String> {
    return listOf(
        "com.google.android.exoplayer2.ext.opus.LibopusAudioRenderer",
        "com.google.android.exoplayer2.ext.flac.LibflacAudioRenderer",
        "com.google.android.exoplayer2.ext.ffmpeg.FfmpegAudioRenderer",
    )
  }

  override fun buildRenderers(): List<Renderer> {
    val renderers = mutableListOf<Renderer>()

    val audioCapabilities = AudioCapabilities.getCapabilities(context)
    renderers.add(MediaCodecAudioRenderer(context, MediaCodecSelector.DEFAULT, handler, audioRendererEventListener, audioCapabilities))

    // Adds any registered classes
    rendererClasses().forEach { className ->
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
}