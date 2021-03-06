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

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Handler
import com.devbrackets.android.exomedia.BuildConfig
import com.devbrackets.android.exomedia.ExoMedia
import com.devbrackets.android.exomedia.core.source.builder.DefaultMediaSourceBuilder
import com.devbrackets.android.exomedia.core.source.builder.MediaSourceBuilder
import com.devbrackets.android.exomedia.util.getExtension
import com.google.android.exoplayer2.drm.DrmSessionManager
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.upstream.TransferListener

/**
 * Provides the functionality to determine which [MediaSource] should be used
 * to play a particular URL.
 */
open class MediaSourceProvider {

  @Deprecated("Deprecated in 5.0.0", replaceWith = ReplaceWith("generate(MediaSourceBuilder.MediaSourceAttributes(context, uri, handler, transferListener, drmSessionManager))"))
  fun generate(context: Context, handler: Handler, uri: Uri, transferListener: TransferListener?, drmSessionManager: DrmSessionManager?): MediaSource {
    return generate(MediaSourceBuilder.MediaSourceAttributes(context, uri, handler, transferListener, drmSessionManager))
  }

  /**
   * Generates a [MediaSource] for the provided attributes,
   */
  fun generate(attributes: MediaSourceBuilder.MediaSourceAttributes): MediaSource {
    val sourceTypeBuilder = findByProviders(attributes.uri)

    // If a registered builder wasn't found then use the default
    val builder = sourceTypeBuilder?.builder ?: DefaultMediaSourceBuilder()
    return builder.build(attributes)
  }

  class SourceTypeBuilder(val builder: MediaSourceBuilder, val uriScheme: String?, val extension: String?, val looseComparisonRegex: String?)

  companion object {
    protected const val USER_AGENT_FORMAT = "ExoMedia %s (%d) / Android %s / %s"

    @SuppressLint("DefaultLocale")
    val defaultUserAgent = String.format(USER_AGENT_FORMAT, BuildConfig.EXO_MEDIA_VERSION_NAME, BuildConfig.EXO_MEDIA_VERSION_CODE, Build.VERSION.RELEASE, Build.MODEL)

    protected fun findByProviders(uri: Uri): SourceTypeBuilder? {
      return findByScheme(uri) /* Uri Scheme (e.g. rtsp) */
          ?: findByExtension(uri)
          ?: findByLooseComparison(uri)
    }

    protected fun findByScheme(uri: Uri): SourceTypeBuilder? {
      val scheme = uri.scheme?.takeIf { it.isNotEmpty() } ?: return null
      return ExoMedia.Data.sourceTypeBuilders.firstOrNull { it.uriScheme.equals(scheme, true) }
    }

    protected fun findByExtension(uri: Uri): SourceTypeBuilder? {
      val extension = uri.getExtension().takeIf { it.isNotEmpty() } ?: return null
      return ExoMedia.Data.sourceTypeBuilders.firstOrNull { it.extension.equals(extension, true) }
    }

    protected fun findByLooseComparison(uri: Uri): SourceTypeBuilder? {
      val uriString = uri.toString()
      return ExoMedia.Data.sourceTypeBuilders.firstOrNull { it.looseComparisonRegex?.toRegex()?.matches(uriString) == true }
    }
  }
}
