/*
 * Copyright (C) 2017 - 2018 ExoMedia Contributors
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

import com.devbrackets.android.exomedia.ExoMedia
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.TransferListener

abstract class MediaSourceBuilder {

    abstract fun build(context: Context, uri: Uri, userAgent: String, handler: Handler, transferListener: TransferListener?): MediaSource

    fun buildDataSourceFactory(context: Context, userAgent: String, listener: TransferListener?): DataSource.Factory {
        val provider = ExoMedia.Data.dataSourceFactoryProvider
        val dataSourceFactory: DataSource.Factory = provider?.provide(userAgent, listener)
                ?: DefaultHttpDataSourceFactory(userAgent, listener)
        return DefaultDataSourceFactory(context, listener, dataSourceFactory)
    }
}
