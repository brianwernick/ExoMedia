package com.devbrackets.android.exomedia.core.renderer.provider

import com.devbrackets.android.exomedia.core.renderer.RendererType
import androidx.media3.exoplayer.Renderer

/**
 * Provides the capabilities for building renderers
 */
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