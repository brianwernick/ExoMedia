package com.devbrackets.android.exomedia.core.renderer.provider

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.Renderer
import com.devbrackets.android.exomedia.core.renderer.RendererType
import kotlin.reflect.KClass

@OptIn(UnstableApi::class)
abstract class AbstractRenderProvider(
  private val type: RendererType
): RenderProvider {
  private var latestRenderers: List<Renderer> = emptyList()

  override fun type() = type
  override fun getLatestRenderers() = latestRenderers

  override fun rendererClasses(): List<String> {
    return emptyList()
  }

  protected fun buildRenderers(
    vararg initialRenderers: Renderer,
    action: (className: String) -> Renderer?
  ): List<Renderer> {
    val renderers = mutableListOf(*initialRenderers)

    // Adds any registered classes
    rendererClasses().forEach { className ->
      try {
        action(className)?.let { renderer ->
          renderers.add(renderer)
        }
      } catch (e: Exception) {
        // Purposefully left blank
      }
    }

    return renderers.also {
      latestRenderers = it
    }
  }

  protected fun <T> Class<T>.newInstance(vararg params: Pair<KClass<*>, Any>): T {
    val types = params.map { it.first.java }.toTypedArray()
    val values = params.map { it.second }.toTypedArray()

    return getConstructor(*types).newInstance(*values)
  }
}