package com.devbrackets.android.exomedia.unified

import androidx.annotation.OptIn
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import com.devbrackets.android.exomedia.nmp.config.PlayerConfig
import com.devbrackets.android.exomedia.ui.widget.VideoView
import com.devbrackets.android.exomedia.unified.AudioPlayerWrapper.Companion.SUPPORTED_AUDIO_COMMANDS
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * An implementation of the Media3 [Player] that uses the [VideoView] as the backing.
 *
 * NOTE:
 * The [VideoView] doesn't extend the Player interface so that the exposed APIs
 * are straightforward for simple use-cases.
 */
@OptIn(UnstableApi::class)
open class VideoViewWrapper(
  val videoView: VideoView,
  protected val playerConfig: PlayerConfig = videoView.videoPlayer.playerConfig
): SimpleBasePlayer(playerConfig.handler.looper) {
  companion object {
    @JvmStatic
    val SUPPORTED_VIDEO_COMMANDS = listOf<Int>(
      // TODO: add support for dynamically swapping the video surface (SurfaceEnvelope in the VideoView)
//      COMMAND_SET_VIDEO_SURFACE
    )
  }

  protected var latestPlayWhenReady: Boolean = false

  override fun getState(): State {
    val allowedCommands = Player.Commands.Builder().apply {
      SUPPORTED_AUDIO_COMMANDS.forEach {
        add(it)
      }

      SUPPORTED_VIDEO_COMMANDS.forEach {
        add(it)
      }
    }.build()

    return State.Builder()
      .setAvailableCommands(allowedCommands)
      .setPlayWhenReady(latestPlayWhenReady, PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
      .build()
  }

  override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
    this.latestPlayWhenReady = playWhenReady

    when (playWhenReady) {
      true -> videoView.start()
      false -> videoView.pause()
    }

    return Futures.immediateVoidFuture()
  }

  override fun handlePrepare(): ListenableFuture<*> {
    // ExoMedia implementations automatically prepare the media
    return Futures.immediateVoidFuture()
  }

  override fun handleStop(): ListenableFuture<*> {
    this.latestPlayWhenReady = false
    videoView.stop()

    return Futures.immediateVoidFuture()
  }

  override fun handleRelease(): ListenableFuture<*> {
    videoView.release()
    return Futures.immediateVoidFuture()
  }

  override fun handleSetRepeatMode(repeatMode: Int): ListenableFuture<*> {
    videoView.setRepeatMode(repeatMode)
    return Futures.immediateVoidFuture()
  }

  override fun handleSetPlaybackParameters(playbackParameters: PlaybackParameters): ListenableFuture<*> {
    videoView.setPlaybackSpeed(playbackParameters.speed)
    videoView.setPlaybackPitch(playbackParameters.pitch)

    return Futures.immediateVoidFuture()
  }

  override fun handleSetTrackSelectionParameters(trackSelectionParameters: TrackSelectionParameters): ListenableFuture<*> {
    videoView.setTrackSelectionParameters(trackSelectionParameters)
    return Futures.immediateVoidFuture()
  }

  override fun handleSetVolume(volume: Float): ListenableFuture<*> {
    videoView.volume = volume
    return Futures.immediateVoidFuture()
  }

  override fun handleSetMediaItems(mediaItems: MutableList<MediaItem>, startIndex: Int, startPositionMs: Long): ListenableFuture<*> {
    // We only support setting a single media item
    val mediaSource = mediaItems.firstOrNull()?.let {
      playerConfig.mediaSourceFactory.createMediaSource(it)
    }

    videoView.setMedia(mediaSource)
    if (startPositionMs > 0) {
      videoView.seekTo(startPositionMs)
    }

    return Futures.immediateVoidFuture()
  }

  override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int): ListenableFuture<*> {
    // We only support a single media item, so we ignore the index
    videoView.seekTo(positionMs)
    return Futures.immediateVoidFuture()
  }
}