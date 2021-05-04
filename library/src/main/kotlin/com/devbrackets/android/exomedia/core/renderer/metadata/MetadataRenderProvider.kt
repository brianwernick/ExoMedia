/*
 * Copyright (C) 2017 - 2021 ExoMedia Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.devbrackets.android.exomedia.core.renderer.metadata

import android.os.Handler
import com.devbrackets.android.exomedia.core.renderer.RenderProvider
import com.devbrackets.android.exomedia.core.renderer.RendererType
import com.google.android.exoplayer2.Renderer
import com.google.android.exoplayer2.metadata.MetadataDecoderFactory
import com.google.android.exoplayer2.metadata.MetadataOutput
import com.google.android.exoplayer2.metadata.MetadataRenderer

class MetadataRenderProvider(
    private val handler: Handler,
    private val metadataListener: MetadataOutput
): RenderProvider {
  override fun type() = RendererType.METADATA

  override fun rendererClasses(): List<String> {
    return emptyList()
  }

  override fun buildRenderers(): List<Renderer> {
    return listOf(MetadataRenderer(metadataListener, handler.looper, MetadataDecoderFactory.DEFAULT))
  }
}