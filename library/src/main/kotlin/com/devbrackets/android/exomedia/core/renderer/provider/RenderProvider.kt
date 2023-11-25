package com.devbrackets.android.exomedia.core.renderer.provider

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.Renderer
import com.devbrackets.android.exomedia.core.renderer.RendererType

/**
 * Provides the capabilities for building renderers
 */
@OptIn(UnstableApi::class)
interface RenderProvider {

  /**
   * The [RendererType] that this provider builds
   */
  fun type(): RendererType

  /**
   * the list of pre-defined classes to that the provider supports
   */
  fun rendererClasses(): List<String>

  /**
   * Retrieves the latest built renderers
   */
  fun getLatestRenderers(): List<Renderer>
}