package com.devbrackets.android.exomedia.core.renderer.provider

import android.os.Handler
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.text.TextOutput
import androidx.media3.exoplayer.text.TextRenderer
import com.devbrackets.android.exomedia.core.renderer.RendererType

@OptIn(UnstableApi::class)
class CaptionRenderProvider: AbstractRenderProvider(RendererType.CLOSED_CAPTION) {
  fun buildRenderers(handler: Handler, captionListener: TextOutput): List<Renderer> {
    return buildRenderers(TextRenderer(captionListener, handler.looper)) {
      Log.w("CaptionRenderProvider", "Adding additional Caption renderers not supported")
      null
    }
  }
}