package com.devbrackets.android.exomedia.core.renderer.provider

import android.content.Context
import android.os.Handler
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.video.DecoderVideoRenderer
import androidx.media3.exoplayer.video.MediaCodecVideoRenderer
import androidx.media3.exoplayer.video.VideoRendererEventListener
import com.devbrackets.android.exomedia.core.renderer.RendererType

@OptIn(UnstableApi::class)
class VideoRenderProvider(
  private val droppedFrameNotificationAmount: Int = 50,
  private val videoJoiningTimeMs: Long = 5_000L
) : AbstractRenderProvider(RendererType.VIDEO) {
  override fun rendererClasses(): List<String> {
    return listOf(
      "androidx.media3.decoder.av1.Libgav1VideoRenderer",
      "androidx.media3.decoder.vp9.LibvpxVideoRenderer"
    )
  }

  fun buildRenderers(
    context: Context,
    handler: Handler,
    listener: VideoRendererEventListener
  ): List<Renderer> {
    val initialRenderer = MediaCodecVideoRenderer(
      context,
      MediaCodecSelector.DEFAULT,
      videoJoiningTimeMs,
      handler,
      listener,
      droppedFrameNotificationAmount
    )

    return buildRenderers(initialRenderer) { className ->
      buildRenderer(className, handler, listener)
    }
  }

  private fun buildRenderer(
    className: String,
    handler: Handler,
    listener: VideoRendererEventListener
  ): Renderer? {
    return when (val rendererClass = Class.forName(className)) {
      DecoderVideoRenderer::class.java -> rendererClass.newInstance(
        Long::class to videoJoiningTimeMs,
        Handler::class to handler,
        VideoRendererEventListener::class to listener,
        Int::class to droppedFrameNotificationAmount
      )
      else -> {
        Log.w("VideoRenderProvider", "Unsupported video Renderer class $rendererClass")
        null
      }
    }
  }
}