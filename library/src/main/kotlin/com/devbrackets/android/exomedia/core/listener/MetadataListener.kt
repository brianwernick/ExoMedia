package com.devbrackets.android.exomedia.core.listener

import androidx.media3.common.Metadata


/**
 * A listener for receiving ID3 metadata parsed from the media stream.
 */
interface MetadataListener {
  /**
   * Called each time there is a metadata associated with current playback time.
   *
   * @param metadata The metadata.
   */
  fun onMetadata(metadata: Metadata)
}
