package com.devbrackets.android.exomedia.core.source.builder

import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.smoothstreaming.DefaultSsChunkSource
import androidx.media3.exoplayer.smoothstreaming.SsMediaSource
import androidx.media3.exoplayer.source.MediaSource

class SsMediaSourceBuilder : MediaSourceBuilder() {
  override fun build(attributes: MediaSourceAttributes): MediaSource {
    val factoryAttributes = attributes.copy(
        transferListener = null
    )

    val dataSourceFactory = buildDataSourceFactory(factoryAttributes)
    val meteredDataSourceFactory = buildDataSourceFactory(attributes)
    val mediaItem = MediaItem.Builder().setUri(attributes.uri).build()

    return SsMediaSource.Factory(DefaultSsChunkSource.Factory(meteredDataSourceFactory), dataSourceFactory)
        .setDrmSessionManagerProvider(attributes.drmSessionManagerProvider)
        .createMediaSource(mediaItem)
  }
}
