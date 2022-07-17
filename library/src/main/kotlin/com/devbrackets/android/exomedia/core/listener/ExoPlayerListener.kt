package com.devbrackets.android.exomedia.core.listener

import androidx.media3.common.Player.State
import com.devbrackets.android.exomedia.core.state.PlaybackStateListener
import com.devbrackets.android.exomedia.listener.OnSeekCompletionListener
import com.devbrackets.android.exomedia.nmp.ExoMediaPlayer

/**
 * A listener for core [ExoMediaPlayer] events
 */
interface ExoPlayerListener : OnSeekCompletionListener, PlaybackStateListener {
  @Deprecated("Use onPlaybackStateChange")
  fun onStateChanged(playWhenReady: Boolean, @State playbackState: Int)

  fun onError(player: ExoMediaPlayer, e: Exception?)

  fun onVideoSizeChanged(width: Int, height: Int, unAppliedRotationDegrees: Int, pixelWidthHeightRatio: Float)
}
