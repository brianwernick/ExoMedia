package com.devbrackets.android.exomedia.core.source.builder

import android.content.Context
import android.net.Uri
import android.os.Handler
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.exoplayer.drm.DefaultDrmSessionManagerProvider
import com.devbrackets.android.exomedia.core.source.data.DataSourceFactoryProvider
import com.devbrackets.android.exomedia.core.source.data.DefaultDataSourceFactoryProvider
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.source.MediaSource

abstract class MediaSourceBuilder {
  data class MediaSourceAttributes(
    val context: Context,
    val uri: Uri,
    val handler: Handler,
    val userAgent: String,
    val transferListener: TransferListener? = null,
    val drmSessionManagerProvider: DrmSessionManagerProvider = DefaultDrmSessionManagerProvider(),
    val dataSourceFactoryProvider: DataSourceFactoryProvider = DefaultDataSourceFactoryProvider()
  )

  abstract fun build(attributes: MediaSourceAttributes): MediaSource

  fun buildDataSourceFactory(attributes: MediaSourceAttributes): DataSource.Factory {
    val dataSourceFactory = attributes.dataSourceFactoryProvider.provide(attributes.userAgent, attributes.transferListener)

    return DefaultDataSource.Factory(attributes.context, dataSourceFactory).apply {
      setTransferListener(attributes.transferListener)
    }
  }
}
