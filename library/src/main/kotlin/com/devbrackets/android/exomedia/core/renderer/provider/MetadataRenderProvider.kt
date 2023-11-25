package com.devbrackets.android.exomedia.core.renderer.provider

import android.os.Handler
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.metadata.MetadataDecoderFactory
import androidx.media3.exoplayer.metadata.MetadataOutput
import androidx.media3.exoplayer.metadata.MetadataRenderer
import com.devbrackets.android.exomedia.core.renderer.RendererType

@OptIn(UnstableApi::class)
class MetadataRenderProvider: AbstractRenderProvider(RendererType.METADATA) {
  fun buildRenderers(handler: Handler, metadataListener: MetadataOutput): List<Renderer> {
    return buildRenderers(MetadataRenderer(metadataListener, handler.looper, MetadataDecoderFactory.DEFAULT)) {
      Log.w("MetadataRenderProvider", "Adding additional Metadata renderers not supported")
      null
    }
  }
}