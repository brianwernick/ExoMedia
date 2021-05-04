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

package com.devbrackets.android.exomedia.core.source.builder

import android.content.Context
import android.net.Uri
import android.os.Handler
import com.devbrackets.android.exomedia.core.source.data.DataSourceFactoryProvider
import com.devbrackets.android.exomedia.core.source.data.DefaultDataSourceFactoryProvider
import com.google.android.exoplayer2.drm.DrmSessionManagerProvider
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.TransferListener

abstract class MediaSourceBuilder {
  data class MediaSourceAttributes(
      val context: Context,
      val uri: Uri,
      val handler: Handler,
      val userAgent: String,
      val transferListener: TransferListener? = null,
      val drmSessionManagerProvider: DrmSessionManagerProvider? = null,
      val dataSourceFactoryProvider: DataSourceFactoryProvider = DefaultDataSourceFactoryProvider()
  )

  abstract fun build(attributes: MediaSourceAttributes): MediaSource

  fun buildDataSourceFactory(attributes: MediaSourceAttributes): DataSource.Factory {
    val dataSourceFactory = attributes.dataSourceFactoryProvider.provide(attributes.userAgent, attributes.transferListener)
    return DefaultDataSourceFactory(attributes.context, attributes.transferListener, dataSourceFactory)
  }
}
