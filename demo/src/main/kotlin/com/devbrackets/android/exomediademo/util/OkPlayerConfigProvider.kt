package com.devbrackets.android.exomediademo.util

import android.content.Context
import com.devbrackets.android.exomedia.core.source.data.DataSourceFactoryProvider
import com.devbrackets.android.exomedia.nmp.config.PlayerConfig
import com.devbrackets.android.exomedia.nmp.config.PlayerConfigBuilder
import com.devbrackets.android.exomedia.nmp.config.PlayerConfigProvider

/**
 * A custom PlayerConfigProvider that uses the OKHttpDataSource, this is referenced in the
 * `video_player_activity` VideoView layout attributes.
 */
@Suppress("unused")
class OkPlayerConfigProvider : PlayerConfigProvider {
  companion object {
    private var dataSourceFactoryProvider: DataSourceFactoryProvider? = null
  }

  override fun getConfig(context: Context): PlayerConfig {
    return PlayerConfigBuilder(context)
      .setDataSourceFactoryProvider(getDataSourceFactoryProvider(context))
      .build()
  }

  private fun getDataSourceFactoryProvider(context: Context): DataSourceFactoryProvider {
    dataSourceFactoryProvider?.let {
      return it
    }

    return OkHttpDataSourceFactoryProvider(context.applicationContext).apply {
      dataSourceFactoryProvider = this
    }
  }

}