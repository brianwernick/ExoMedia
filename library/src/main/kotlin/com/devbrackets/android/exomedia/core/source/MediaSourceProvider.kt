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

package com.devbrackets.android.exomedia.core.source

import android.net.Uri
import com.devbrackets.android.exomedia.core.source.builder.*
import com.devbrackets.android.exomedia.util.getExtension
import com.google.android.exoplayer2.source.MediaSource

/**
 * Provides the functionality to determine which [MediaSource] should be used
 * to play a particular URL.
 */
open class MediaSourceProvider {
  data class SourceTypeBuilder(
      val builder: MediaSourceBuilder,
      val uriScheme: String?,
      val extension: String?,
      val looseComparisonRegex: String?
  )

  private val builders = listOf(
      SourceTypeBuilder(HlsMediaSourceBuilder(), null, ".m3u8", ".*\\.m3u8.*"),
      SourceTypeBuilder(DashMediaSourceBuilder(), null, ".mpd", ".*\\.mpd.*"),
      SourceTypeBuilder(SsMediaSourceBuilder(), null, ".ism", ".*\\.ism.*")
  )

  open fun builders(): List<SourceTypeBuilder> {
    return builders
  }

  /**
   * Generates a [MediaSource] for the provided attributes
   */
  fun generate(attributes: MediaSourceBuilder.MediaSourceAttributes): MediaSource {
    val sourceTypeBuilder = findByProviders(attributes.uri)

    // If a registered builder wasn't found then use the default
    val builder = sourceTypeBuilder?.builder ?: DefaultMediaSourceBuilder()
    return builder.build(attributes)
  }

  protected fun findByProviders(uri: Uri): SourceTypeBuilder? {
    return findByScheme(uri)
        ?: findByExtension(uri)
        ?: findByLooseComparison(uri)
  }

  /* Uri Scheme (e.g. rtsp) */
  protected fun findByScheme(uri: Uri): SourceTypeBuilder? {
    val scheme = uri.scheme?.takeIf { it.isNotEmpty() } ?: return null
    return builders().firstOrNull { it.uriScheme.equals(scheme, true) }
  }

  protected fun findByExtension(uri: Uri): SourceTypeBuilder? {
    val extension = uri.getExtension().takeIf { it.isNotEmpty() } ?: return null
    return builders().firstOrNull { it.extension.equals(extension, true) }
  }

  protected fun findByLooseComparison(uri: Uri): SourceTypeBuilder? {
    val uriString = uri.toString()
    return builders().firstOrNull { it.looseComparisonRegex?.toRegex()?.matches(uriString) == true }
  }
}
