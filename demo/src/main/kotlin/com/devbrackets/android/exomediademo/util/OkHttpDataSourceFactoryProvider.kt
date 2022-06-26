package com.devbrackets.android.exomediademo.util

import android.content.Context
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.devbrackets.android.exomedia.core.source.data.DataSourceFactoryProvider
import okhttp3.OkHttpClient
import java.io.File

/**
 * An example implementation of the [DataSourceFactoryProvider] that uses OkHttp to
 * fetch and cache the data.
 *
 *  **NOTE:**
 *  The OkHttpDataSource.Factory can be found in the ExoPlayer extension library `extension-okhttp`
 */
class OkHttpDataSourceFactoryProvider(context: Context): DataSourceFactoryProvider {
  private val cacheDir = context.cacheDir
  private var instance: CacheDataSource.Factory? = null

  override fun provide(userAgent: String, listener: TransferListener?): DataSource.Factory {
    instance?.let {
      return it
    }

    // Updates the network data source to use the OKHttp implementation
    val upstreamFactory = OkHttpDataSource.Factory(OkHttpClient()).apply {
      setUserAgent(userAgent)
      setTransferListener(listener)
    }

    // Adds a cache around the upstreamFactory
    val cache = SimpleCache(File(cacheDir, "ExoMediaCache"), LeastRecentlyUsedCacheEvictor((50 * 1024 * 1024).toLong()))

    instance = CacheDataSource.Factory().apply {
      setCache(cache)
      setUpstreamDataSourceFactory(upstreamFactory)
      setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    return instance!!
  }

}