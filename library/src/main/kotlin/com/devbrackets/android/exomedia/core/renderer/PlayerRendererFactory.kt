package com.devbrackets.android.exomedia.core.renderer

import android.content.Context
import android.os.Handler
import com.devbrackets.android.exomedia.core.renderer.provider.AudioRenderProvider
import com.devbrackets.android.exomedia.core.renderer.provider.CaptionRenderProvider
import com.devbrackets.android.exomedia.core.renderer.provider.MetadataRenderProvider
import com.devbrackets.android.exomedia.core.renderer.provider.VideoRenderProvider
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.metadata.MetadataOutput
import androidx.media3.exoplayer.text.TextOutput
import androidx.media3.exoplayer.video.VideoRendererEventListener

class PlayerRendererFactory(
  private val context: Context
): RenderersFactory {
  override fun createRenderers(
    eventHandler: Handler,
    videoRendererEventListener: VideoRendererEventListener,
    audioRendererEventListener: AudioRendererEventListener,
    textRendererOutput: TextOutput,
    metadataRendererOutput: MetadataOutput
  ): Array<Renderer> {
    return mutableListOf<Renderer>().apply {
      addAll(AudioRenderProvider().buildRenderers(context, eventHandler, audioRendererEventListener))
      addAll(VideoRenderProvider().buildRenderers(context, eventHandler, videoRendererEventListener))
      addAll(CaptionRenderProvider().buildRenderers(eventHandler, textRendererOutput))
      addAll(MetadataRenderProvider().buildRenderers(eventHandler, metadataRendererOutput))
    }.toTypedArray()
  }
}