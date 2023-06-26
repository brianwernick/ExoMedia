package com.devbrackets.android.exomedia.core.source.builder

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.extractor.DefaultExtractorsFactory

@OptIn(UnstableApi::class)
class DefaultMediaSourceBuilder : MediaSourceBuilder() {
  override fun build(attributes: MediaSourceAttributes): MediaSource {
    val dataSourceFactory = buildDataSourceFactory(attributes)
    val mediaItem = MediaItem.Builder().setUri(attributes.uri).build()

    return ProgressiveMediaSource.Factory(dataSourceFactory, DefaultExtractorsFactory())
      .setDrmSessionManagerProvider(attributes.drmSessionManagerProvider)
      .createMediaSource(mediaItem)
  }
}
