package com.devbrackets.android.exomediademo

import android.app.Application
import com.devbrackets.android.exomedia.ExoMedia
import com.devbrackets.android.exomediademo.manager.PlaylistManager
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.TransferListener
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.squareup.leakcanary.LeakCanary
import okhttp3.OkHttpClient
import java.io.File

class App : Application() {
    val playlistManager: PlaylistManager by lazy { PlaylistManager(this) }

    override fun onCreate() {
        super.onCreate()

        LeakCanary.install(this)
        configureExoMedia()
    }

    private fun configureExoMedia() {
        // Registers the media sources to use the OkHttp client instead of the standard Apache one
        // Note: the OkHttpDataSourceFactory can be found in the ExoPlayer extension library `extension-okhttp`
        ExoMedia.setDataSourceFactoryProvider(object : ExoMedia.DataSourceFactoryProvider {
            private var instance: CacheDataSourceFactory? = null

            override fun provide(userAgent: String, listener: TransferListener?): DataSource.Factory {
                if (instance == null) {
                    // Updates the network data source to use the OKHttp implementation
                    val upstreamFactory = OkHttpDataSourceFactory(OkHttpClient(), userAgent, listener)

                    // Adds a cache around the upstreamFactory
                    val cache = SimpleCache(File(cacheDir, "ExoMediaCache"), LeastRecentlyUsedCacheEvictor((50 * 1024 * 1024).toLong()))
                    instance = CacheDataSourceFactory(cache, upstreamFactory, CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                }

                return instance!!
            }
        })
    }
}
