package com.devbrackets.android.exomedia.core.renderer.provider

import android.content.Context
import android.os.Handler
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.audio.AudioCapabilities
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import com.devbrackets.android.exomedia.core.renderer.RendererType

@OptIn(UnstableApi::class)
open class AudioRenderProvider : RenderProvider {
  private var latestRenderers: List<Renderer> = emptyList()

  override fun type() = RendererType.AUDIO

  override fun rendererClasses(): List<String> {
    return listOf(
      "androidx.media3.exoplayer.ext.opus.LibopusAudioRenderer",
      "androidx.media3.exoplayer.ext.flac.LibflacAudioRenderer",
      "androidx.media3.exoplayer.ext.ffmpeg.FfmpegAudioRenderer",
    )
  }

  override fun getLatestRenderers(): List<Renderer> {
    return latestRenderers
  }

  fun buildRenderers(context: Context, handler: Handler, listener: AudioRendererEventListener): List<Renderer> {
    val renderers = mutableListOf<Renderer>()

    val audioCapabilities = AudioCapabilities.getCapabilities(context)
    renderers.add(MediaCodecAudioRenderer(context, MediaCodecSelector.DEFAULT, handler, listener, audioCapabilities))

    // Adds any registered classes
    rendererClasses().forEach { className ->
      try {
        val renderer = buildRenderer(className, handler, listener)
        renderers.add(renderer)
      } catch (e: Exception) {
        // Purposefully left blank
      }
    }

    return renderers.also {
      latestRenderers = it
    }
  }

  private fun buildRenderer(
    className: String,
    handler: Handler,
    listener: AudioRendererEventListener
  ): Renderer {
    val rendererClass = Class.forName(className)

    val constructor = rendererClass.getConstructor(
      Long::class.javaPrimitiveType,
      AudioRendererEventListener::class.java
    )

    return constructor.newInstance(handler, listener) as Renderer
  }
}