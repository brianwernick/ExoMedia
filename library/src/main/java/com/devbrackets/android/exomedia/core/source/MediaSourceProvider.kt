/*
 * Copyright (C) 2017 - 2019 ExoMedia Contributors
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
import com.devbrackets.android.exomedia.util.MediaSourceUtil
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.upstream.TransferListener

/**
 * Provides the functionality to determine which [MediaSource] should be used
 * to play a particular URL.
 */
open class MediaSourceProvider {

    @SuppressLint("DefaultLocale")
    protected var userAgent = String.format(USER_AGENT_FORMAT, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, Build.VERSION.RELEASE, Build.MODEL)

    fun generate(context: Context, handler: Handler, uri: Uri, transferListener: TransferListener?): MediaSource {
        val sourceTypeBuilder = findByProviders(uri)

        // If a registered builder wasn't found then use the default
        val builder = sourceTypeBuilder?.builder ?: DefaultMediaSourceBuilder()
        return builder.build(context, uri, userAgent, handler, transferListener)
    }

    class SourceTypeBuilder(val builder: MediaSourceBuilder, val uriScheme: String?, val extension: String?, val looseComparisonRegex: String?)

    companion object {
        protected val USER_AGENT_FORMAT = "ExoMedia %s (%d) / Android %s / %s"

        protected fun findByProviders(uri: Uri): SourceTypeBuilder? {
            return findByScheme(uri) /* Uri Scheme (e.g. rtsp) */ ?: findByExtension(uri)
            ?: findByLooseComparison(uri)
        }

        protected fun findByScheme(uri: Uri): SourceTypeBuilder? {
            val scheme = uri.scheme?.takeIf { it.isNotEmpty() } ?: return null
            return ExoMedia.Data.sourceTypeBuilders.firstOrNull { it.uriScheme.equals(scheme, ignoreCase = true) }
        }

        protected fun findByExtension(uri: Uri): SourceTypeBuilder? {
            val extension = MediaSourceUtil.getExtension(uri)?.takeIf { it.isNotEmpty() }
                    ?: return null
            return ExoMedia.Data.sourceTypeBuilders.firstOrNull { it.extension.equals(extension, ignoreCase = true) }
        }

        protected fun findByLooseComparison(uri: Uri): SourceTypeBuilder? {
            val uriString = uri.toString()
            return ExoMedia.Data.sourceTypeBuilders.firstOrNull { it.looseComparisonRegex?.toRegex()?.matches(uriString) == true }
        }
    }
}
