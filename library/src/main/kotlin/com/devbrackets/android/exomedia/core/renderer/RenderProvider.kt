/*
 * Copyright (C) 2021 ExoMedia Contributors
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

package com.devbrackets.android.exomedia.core.renderer

import com.google.android.exoplayer2.Renderer

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
   * Builds the renderers of type [type]
   */
  fun buildRenderers(): List<Renderer>
}