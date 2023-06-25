package com.devbrackets.android.exomedia.unified.ext

import com.devbrackets.android.exomedia.AudioPlayer
import com.devbrackets.android.exomedia.ui.widget.VideoView
import com.devbrackets.android.exomedia.unified.AudioPlayerWrapper
import com.devbrackets.android.exomedia.unified.VideoViewWrapper

/**
 * Wraps the [VideoView] in a Media3 [androidx.media3.common.Player] to
 * support integrating with the rest of the Media3 framework for playlists,
 * player selection, etc.
 *
 * @return A Media3 [androidx.media3.common.Player] that uses the [VideoView] as the implementation
 */
fun VideoView.asMedia3Player(): VideoViewWrapper {
  return VideoViewWrapper(this)
}

/**
 * Wraps the [AudioPlayer] in a Media3 [androidx.media3.common.Player] to
 * support integrating with the rest of the Media3 framework for playlists,
 * player selection, etc.
 *
 * @return A Media3 [androidx.media3.common.Player] that uses the [AudioPlayer] as the implementation
 */
fun AudioPlayer.asMedia3Player(): AudioPlayerWrapper {
  return AudioPlayerWrapper(this)
}