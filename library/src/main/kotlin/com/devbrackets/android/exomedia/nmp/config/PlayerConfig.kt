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
import com.devbrackets.android.exomedia.core.source.data.DataSourceFactoryProvider
import com.devbrackets.android.exomedia.core.renderer.*
import com.devbrackets.android.exomedia.core.source.MediaSourceProvider
import com.devbrackets.android.exomedia.nmp.CorePlayerListeners
import com.devbrackets.android.exomedia.nmp.manager.UserAgentProvider
import com.devbrackets.android.exomedia.nmp.manager.WakeManager
import com.devbrackets.android.exomedia.nmp.manager.track.TrackManager
import com.devbrackets.android.exomedia.util.FallbackManager
import com.google.android.exoplayer2.LoadControl
import com.google.android.exoplayer2.analytics.AnalyticsCollector
import com.google.android.exoplayer2.source.MediaSourceFactory
import com.google.android.exoplayer2.upstream.BandwidthMeter

/**
 * Supplies the classes necessary for the [com.devbrackets.android.exomedia.nmp.ExoMediaPlayerImpl]
 * to play media. It is recommended to use the [PlayerConfigBuilder] to construct a new
 * instance as that will provide default implementations.
 */
data class PlayerConfig(
    val context: Context,
    val coreListeners: CorePlayerListeners,
    val fallbackManager: FallbackManager,
    val analyticsCollector: AnalyticsCollector,
    val bandwidthMeter: BandwidthMeter,
    val handler: Handler,
    val renderProviders: Map<RendererType, RenderProvider>,
    val trackManager: TrackManager,
    val wakeManager: WakeManager,
    val loadControl: LoadControl,
    val userAgentProvider: UserAgentProvider,
    val mediaSourceProvider: MediaSourceProvider,
    val mediaSourceFactory: MediaSourceFactory,
    val dataSourceFactoryProvider: DataSourceFactoryProvider
)