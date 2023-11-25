package com.devbrackets.android.exomedia.core.renderer.provider

import android.content.Context
import android.os.Handler
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.audio.AudioCapabilities
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.DecoderAudioRenderer
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import com.devbrackets.android.exomedia.core.renderer.RendererType

@OptIn(UnstableApi::class)
open class AudioRenderProvider : AbstractRenderProvider(RendererType.AUDIO) {
  override fun rendererClasses(): List<String> {
    return listOf(
      "androidx.media3.decoder.ffmpeg.FfmpegAudioRenderer",
      "androidx.media3.decoder.flac.LibflacAudioRenderer",
      "androidx.media3.decoder.opus.LibopusAudioRenderer"
    )
  }

  fun buildRenderers(
    context: Context,
    handler: Handler,
    listener: AudioRendererEventListener
  ): List<Renderer> {
    val initialRenderer = MediaCodecAudioRenderer(
      context,
      MediaCodecSelector.DEFAULT,
      handler,
      listener,
      AudioCapabilities.getCapabilities(context)
    )

    return buildRenderers(initialRenderer) { className ->
      buildRenderer(className, handler, listener)
    }
  }

  private fun buildRenderer(
    className: String,
    handler: Handler,
    listener: AudioRendererEventListener
  ): Renderer? {
    return when (val rendererClass = Class.forName(className)) {
      DecoderAudioRenderer::class.java -> rendererClass.newInstance(
        Handler::class to handler,
        AudioRendererEventListener::class to listener,
        arrayOf<AudioProcessor>()::class to arrayOf<AudioProcessor>()
      )
      else -> {
        Log.w("AudioRenderProvider", "Unsupported audio Renderer class $rendererClass")
        null
      }
    }
  }
}
