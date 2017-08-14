package com.devbrackets.android.exomediademo;

import android.app.Application;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.devbrackets.android.exomedia.ExoMedia;
import com.devbrackets.android.exomediademo.manager.PlaylistManager;
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.squareup.leakcanary.LeakCanary;

import okhttp3.OkHttpClient;

public class App extends Application {

    private static App application;
    private static PlaylistManager playlistManager;

    @Override
    public void onCreate() {
        enableStrictMode();
        super.onCreate();

        application = this;
        playlistManager = new PlaylistManager();
        LeakCanary.install(this);

        configureExoMedia();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        application = null;
        playlistManager = null;
    }

    public static PlaylistManager getPlaylistManager() {
        return playlistManager;
    }

    public static App getApplication() {
        return application;
    }

    private void enableStrictMode() {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build());

        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build());
    }

    private void configureExoMedia() {
        // Registers the media sources to use the OkHttp client instead of the standard Apache one
        // Note: the OkHttpDataSourceFactory can be found in the ExoPlayer extension library `extension-okhttp`
        ExoMedia.setDataSourceFactoryProvider(new ExoMedia.DataSourceFactoryProvider() {
            @NonNull
            @Override
            public DataSource.Factory provide(@NonNull String userAgent, @Nullable TransferListener<? super DataSource> listener) {
                // Updates the network data source to use the OKHttp implementation
                DataSource.Factory upstreamFactory = new OkHttpDataSourceFactory(new OkHttpClient(), userAgent, listener);

                // Adds a cache around the upstreamFactory
                Cache cache = new SimpleCache(getCacheDir(), new LeastRecentlyUsedCacheEvictor(50 * 1024 * 1024));
                return new CacheDataSourceFactory(cache, upstreamFactory, CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
            }
        });
    }
}
