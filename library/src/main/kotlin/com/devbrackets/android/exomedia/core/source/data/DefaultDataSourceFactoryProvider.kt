package com.devbrackets.android.exomedia.core.source.data

import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.TransferListener

class DefaultDataSourceFactoryProvider: DataSourceFactoryProvider {
  override fun provide(userAgent: String, listener: TransferListener?): DataSource.Factory {
    return DefaultHttpDataSource.Factory().apply {
      setUserAgent(userAgent)
      setTransferListener(listener)
    }
  }
}