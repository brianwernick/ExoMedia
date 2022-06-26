package com.devbrackets.android.exomedia.nmp.config

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.util.Clock
import com.devbrackets.android.exomedia.AudioPlayer
import com.devbrackets.android.exomedia.core.renderer.PlayerRendererFactory
import com.devbrackets.android.exomedia.core.source.MediaSourceProvider
import com.devbrackets.android.exomedia.core.source.data.DataSourceFactoryProvider
import com.devbrackets.android.exomedia.core.source.data.DefaultDataSourceFactoryProvider
import com.devbrackets.android.exomedia.nmp.manager.UserAgentProvider
import com.devbrackets.android.exomedia.nmp.manager.WakeManager
import com.devbrackets.android.exomedia.nmp.manager.track.TrackManager
import com.devbrackets.android.exomedia.util.FallbackManager
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.analytics.AnalyticsCollector
import androidx.media3.exoplayer.analytics.DefaultAnalyticsCollector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSourceFactory
import androidx.media3.exoplayer.upstream.BandwidthMeter
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter

class PlayerConfigBuilder(private val context: Context) {
  private var analyticsCollector: AnalyticsCollector? = null
  private var bandwidthMeter: BandwidthMeter? = null
  private var handler: Handler? = null
  private var rendererFactory: RenderersFactory? = null
  private var trackManager: TrackManager? = null
  private var wakeManager: WakeManager? = null
  private var loadControl: LoadControl? = null
  private var userAgentProvider: UserAgentProvider? = null
  private var mediaSourceProvider: MediaSourceProvider? = null
  private var mediaSourceFactory: MediaSourceFactory? = null
  private var dataSourceFactoryProvider: DataSourceFactoryProvider? = null
  private var fallbackManager: FallbackManager? = null

  fun setAnalyticsCollector(analyticsCollector: AnalyticsCollector): PlayerConfigBuilder {
    this.analyticsCollector = analyticsCollector
    return this
  }

  fun setBandwidthMeter(bandwidthMeter: BandwidthMeter): PlayerConfigBuilder {
    this.bandwidthMeter = bandwidthMeter
    return this
  }

  fun setHandler(handler: Handler): PlayerConfigBuilder {
    this.handler = handler
    return this
  }

  fun setRendererFactory(factory: RenderersFactory): PlayerConfigBuilder {
    this.rendererFactory = factory
    return this
  }

  fun setTrackManager(trackManager: TrackManager): PlayerConfigBuilder {
    this.trackManager = trackManager
    return this
  }

  fun setWakeManager(wakeManager: WakeManager): PlayerConfigBuilder {
    this.wakeManager = wakeManager
    return this
  }

  /**
   * Specifies the [LoadControl] to use when building the [androidx.media3.exoplayer.ExoPlayer] instance
   * used in the [com.devbrackets.android.exomedia.ui.widget.VideoView] and [AudioPlayer]. This allows the
   * buffering amounts to be modified to better suit your needs which can be easily specified by using an instance of
   * [androidx.media3.exoplayer.DefaultLoadControl]. When the `loadControl` is `null`
   * the default instance of the [androidx.media3.exoplayer.DefaultLoadControl] will be used. This will only
   * take effect for any instances created *after* this was set.
   *
   * @param loadControl The [LoadControl] to use for any new [androidx.media3.exoplayer.ExoPlayer] instances
   */
  fun setLoadControl(loadControl: LoadControl): PlayerConfigBuilder {
    this.loadControl = loadControl
    return this
  }

  fun setUserAgentProvider(provider: UserAgentProvider): PlayerConfigBuilder {
    this.userAgentProvider = provider
    return this
  }

  fun setMediaSourceProvider(provider: MediaSourceProvider): PlayerConfigBuilder {
    this.mediaSourceProvider = provider
    return this
  }

  fun setMediaSourceFactory(factory: MediaSourceFactory): PlayerConfigBuilder {
    this.mediaSourceFactory = factory
    return this
  }

  /**
   * Specifies the provider to use when building [androidx.media3.exoplayer.upstream.DataSource.Factory]
   * instances for use with the [com.devbrackets.android.exomedia.core.source.builder.MediaSourceBuilder]s. This will
   * only be used for builders that haven't customized the [com.devbrackets.android.exomedia.core.source.builder.MediaSourceBuilder.buildDataSourceFactory]
   * method.
   *
   * @param provider The provider to use for the [com.devbrackets.android.exomedia.core.source.builder.MediaSourceBuilder]s
   */
  fun setDataSourceFactoryProvider(provider: DataSourceFactoryProvider): PlayerConfigBuilder {
    this.dataSourceFactoryProvider = provider
    return this
  }

  fun setFallbackManager(manager: FallbackManager): PlayerConfigBuilder {
    this.fallbackManager = manager
    return this
  }

  fun build(): PlayerConfig {
    val actualHandler = handler ?: Handler(Looper.getMainLooper())
    val actualAnalyticsCollector = analyticsCollector ?: DefaultAnalyticsCollector(Clock.DEFAULT)
    val rendererFactory = rendererFactory ?: PlayerRendererFactory(context)

    return PlayerConfig(
        context = context,
        fallbackManager = fallbackManager ?: FallbackManager(),
        analyticsCollector = actualAnalyticsCollector,
        bandwidthMeter = bandwidthMeter ?: DefaultBandwidthMeter.Builder(context).build(),
        handler = actualHandler,
        rendererFactory = rendererFactory,
        trackManager = trackManager ?: TrackManager(context),
        wakeManager = wakeManager ?: WakeManager(context),
        loadControl = loadControl ?: DefaultLoadControl(),
        userAgentProvider = userAgentProvider ?: UserAgentProvider(),
        mediaSourceProvider = mediaSourceProvider ?: MediaSourceProvider(),
        mediaSourceFactory = mediaSourceFactory ?: DefaultMediaSourceFactory(context),
        dataSourceFactoryProvider = dataSourceFactoryProvider ?: DefaultDataSourceFactoryProvider()
    )
  }
}