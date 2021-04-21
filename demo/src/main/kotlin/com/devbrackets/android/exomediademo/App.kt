package com.devbrackets.android.exomediademo

import android.app.Application
import android.content.Context
import com.devbrackets.android.exomedia.nmp.config.PlayerConfig
import com.devbrackets.android.exomedia.nmp.config.PlayerConfigBuilder
import com.devbrackets.android.exomedia.nmp.config.PlayerConfigProvider
import com.devbrackets.android.exomediademo.manager.PlaylistManager
import com.devbrackets.android.exomediademo.util.OkHttpDataSourceFactoryProvider
import com.squareup.leakcanary.LeakCanary

class App : Application() {
    val playlistManager: PlaylistManager by lazy { PlaylistManager(this) }

    override fun onCreate() {
        super.onCreate()

        LeakCanary.install(this)
    }

    /**
     * A custom PlayerConfigProvider that uses the OKHttpDataSource, this is referenced in the
     * `video_player_activity` VideoView layout attributes.
     */
    @Suppress("unused")
    class OkPlayerConfigProvider: PlayerConfigProvider {
        override fun getConfig(context: Context): PlayerConfig {
            return PlayerConfigBuilder(context)
                .setDataSourceFactoryProvider(OkHttpDataSourceFactoryProvider(context))
                .build()
        }
    }
}
