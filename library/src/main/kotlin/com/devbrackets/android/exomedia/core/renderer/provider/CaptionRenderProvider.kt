package com.devbrackets.android.exomedia.core.renderer.provider

import android.os.Handler
import com.devbrackets.android.exomedia.core.renderer.RendererType
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.text.TextOutput
import androidx.media3.exoplayer.text.TextRenderer

class CaptionRenderProvider: RenderProvider {
  private var latestRenderers: List<Renderer> = emptyList()

  override fun type() = RendererType.CLOSED_CAPTION

  override fun rendererClasses(): List<String> {
    return emptyList()
  }

  override fun getLatestRenderers(): List<Renderer> {
    return latestRenderers
  }

  fun buildRenderers(handler: Handler, captionListener: TextOutput): List<Renderer> {
    return listOf(
      TextRenderer(captionListener, handler.looper)
    ).also {
      latestRenderers = it
    }
  }
}