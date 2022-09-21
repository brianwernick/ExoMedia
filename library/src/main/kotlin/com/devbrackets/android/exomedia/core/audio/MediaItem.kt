package com.devbrackets.android.exomedia.core.audio

import android.net.Uri
import androidx.media3.exoplayer.source.MediaSource

/**
 * Holds the source information associated with a media item that
 * has been requested to be played by an Audio or Video player.
 */
data class MediaItem(
  val uri: Uri?,
  val mediaSource: MediaSource?
)