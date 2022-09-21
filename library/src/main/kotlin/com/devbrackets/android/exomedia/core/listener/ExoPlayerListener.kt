package com.devbrackets.android.exomedia.core.listener

import com.devbrackets.android.exomedia.core.state.PlaybackStateListener
import com.devbrackets.android.exomedia.listener.OnSeekCompletionListener
import com.devbrackets.android.exomedia.nmp.ExoMediaPlayer

/**
 * A listener for core [ExoMediaPlayer] events
 */
interface ExoPlayerListener : OnSeekCompletionListener, PlaybackStateListener {
  fun onError(player: ExoMediaPlayer, e: Exception?)

  fun onVideoSizeChanged(width: Int, height: Int, unAppliedRotationDegrees: Int, pixelWidthHeightRatio: Float)
}
