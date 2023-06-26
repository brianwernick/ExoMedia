package com.devbrackets.android.exomedia.core.source.builder

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource

@OptIn(UnstableApi::class)
class HlsMediaSourceBuilder : MediaSourceBuilder() {
  override fun build(attributes: MediaSourceAttributes): MediaSource {
    val dataSourceFactory = buildDataSourceFactory(attributes)
    val mediaItem = MediaItem.Builder().setUri(attributes.uri).build()

    return HlsMediaSource.Factory(dataSourceFactory)
      .setDrmSessionManagerProvider(attributes.drmSessionManagerProvider)
      .createMediaSource(mediaItem)
  }
}
