package com.devbrackets.android.exomedia.core.source.builder

import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.dash.DefaultDashChunkSource
import androidx.media3.exoplayer.source.MediaSource

class DashMediaSourceBuilder : MediaSourceBuilder() {
  override fun build(attributes: MediaSourceAttributes): MediaSource {
    val factoryAttributes = attributes.copy(
        transferListener = null
    )

    val dataSourceFactory = buildDataSourceFactory(factoryAttributes)
    val meteredDataSourceFactory = buildDataSourceFactory(attributes)
    val mediaItem = MediaItem.Builder().setUri(attributes.uri).build()

    return DashMediaSource.Factory(DefaultDashChunkSource.Factory(meteredDataSourceFactory), dataSourceFactory)
        .setDrmSessionManagerProvider(attributes.drmSessionManagerProvider)
        .createMediaSource(mediaItem)
  }
}
