package com.devbrackets.android.exomedia.core.renderer.provider

import android.os.Handler
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.metadata.MetadataDecoderFactory
import androidx.media3.exoplayer.metadata.MetadataOutput
import androidx.media3.exoplayer.metadata.MetadataRenderer
import com.devbrackets.android.exomedia.core.renderer.RendererType

@OptIn(UnstableApi::class)
class MetadataRenderProvider: RenderProvider {
  private var latestRenderers: List<Renderer> = emptyList()

  override fun type() = RendererType.METADATA

  override fun rendererClasses(): List<String> {
    return emptyList()
  }

  override fun getLatestRenderers(): List<Renderer> {
    return latestRenderers
  }

  fun buildRenderers(handler: Handler, metadataListener: MetadataOutput): List<Renderer> {
    return listOf(
      MetadataRenderer(metadataListener, handler.looper, MetadataDecoderFactory.DEFAULT)
    ).also {
      latestRenderers = it
    }
  }
}