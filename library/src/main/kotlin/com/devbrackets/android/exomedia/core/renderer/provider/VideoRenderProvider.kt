package com.devbrackets.android.exomedia.core.renderer.provider

import android.content.Context
import android.os.Handler
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.video.MediaCodecVideoRenderer
import androidx.media3.exoplayer.video.VideoRendererEventListener
import com.devbrackets.android.exomedia.core.renderer.RendererType

@OptIn(UnstableApi::class)
class VideoRenderProvider(
  private val droppedFrameNotificationAmount: Int = 50,
  private val videoJoiningTimeMs: Long = 5_000L
) : RenderProvider {
  private var latestRenderers: List<Renderer> = emptyList()

  override fun type() = RendererType.VIDEO

  override fun rendererClasses(): List<String> {
    return listOf(
      "androidx.media3.exoplayer.ext.vp9.LibvpxVideoRenderer"
    )
  }

  override fun getLatestRenderers(): List<Renderer> {
    return latestRenderers
  }

  fun buildRenderers(
    context: Context,
    handler: Handler,
    listener: VideoRendererEventListener
  ): List<Renderer> {
    val renderers = mutableListOf<Renderer>()
    renderers.add(MediaCodecVideoRenderer(context, MediaCodecSelector.DEFAULT, videoJoiningTimeMs, handler, listener, droppedFrameNotificationAmount))

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
    listener: VideoRendererEventListener
  ): Renderer {
    val rendererClass = Class.forName(className)

    val constructor = rendererClass.getConstructor(
      Boolean::class.javaPrimitiveType,
      Long::class.javaPrimitiveType,
      Handler::class.java,
      VideoRendererEventListener::class.java,
      Int::class.javaPrimitiveType
    )

    return constructor.newInstance(
      true,
      videoJoiningTimeMs,
      handler,
      listener,
      droppedFrameNotificationAmount
    ) as Renderer
  }
}