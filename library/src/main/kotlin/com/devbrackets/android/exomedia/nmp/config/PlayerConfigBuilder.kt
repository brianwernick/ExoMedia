/*
 * Copyright (C) 2021 ExoMedia Contributors
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

package com.devbrackets.android.exomedia.nmp.config

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.devbrackets.android.exomedia.AudioPlayer
import com.devbrackets.android.exomedia.core.renderer.PlayerRendererFactory
import com.devbrackets.android.exomedia.core.source.MediaSourceProvider
import com.devbrackets.android.exomedia.core.source.data.DataSourceFactoryProvider
import com.devbrackets.android.exomedia.core.source.data.DefaultDataSourceFactoryProvider
import com.devbrackets.android.exomedia.nmp.manager.UserAgentProvider
import com.devbrackets.android.exomedia.nmp.manager.WakeManager
import com.devbrackets.android.exomedia.nmp.manager.track.TrackManager
import com.devbrackets.android.exomedia.util.FallbackManager
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.LoadControl
import com.google.android.exoplayer2.RenderersFactory
import com.google.android.exoplayer2.analytics.AnalyticsCollector
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.MediaSourceFactory
import com.google.android.exoplayer2.upstream.BandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.util.Clock

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
   * Specifies the [LoadControl] to use when building the [com.google.android.exoplayer2.ExoPlayer] instance
   * used in the [com.devbrackets.android.exomedia.ui.widget.VideoView] and [AudioPlayer]. This allows the
   * buffering amounts to be modified to better suit your needs which can be easily specified by using an instance of
   * [com.google.android.exoplayer2.DefaultLoadControl]. When the `loadControl` is `null`
   * the default instance of the [com.google.android.exoplayer2.DefaultLoadControl] will be used. This will only
   * take effect for any instances created *after* this was set.
   *
   * @param loadControl The [LoadControl] to use for any new [com.google.android.exoplayer2.ExoPlayer] instances
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
   * Specifies the provider to use when building [com.google.android.exoplayer2.upstream.DataSource.Factory]
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
    val actualAnalyticsCollector = analyticsCollector ?: AnalyticsCollector(Clock.DEFAULT)
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