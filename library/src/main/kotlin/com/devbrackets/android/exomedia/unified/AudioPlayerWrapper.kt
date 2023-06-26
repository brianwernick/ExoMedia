package com.devbrackets.android.exomedia.unified

import androidx.annotation.OptIn
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import com.devbrackets.android.exomedia.AudioPlayer
import com.devbrackets.android.exomedia.nmp.config.PlayerConfig
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * An implementation of the Media3 [Player] that uses the [AudioPlayer] as the backing.
 *
 * NOTE:
 * The [AudioPlayer] doesn't extend the Player interface so that the exposed APIs
 * are straightforward for simple use-cases.
 */
@OptIn(UnstableApi::class)
open class AudioPlayerWrapper(
  val audioPlayer: AudioPlayer,
  protected val playerConfig: PlayerConfig = audioPlayer.playerConfig
): SimpleBasePlayer(playerConfig.handler.looper) {
  companion object {
    @JvmStatic
    val SUPPORTED_AUDIO_COMMANDS = listOf(
      COMMAND_PLAY_PAUSE,
      COMMAND_STOP,
      COMMAND_SEEK_TO_DEFAULT_POSITION,
      COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
      COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
      COMMAND_SEEK_TO_PREVIOUS,
      COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
      COMMAND_SEEK_TO_NEXT,
      COMMAND_SEEK_BACK,
      COMMAND_SEEK_FORWARD,
      COMMAND_SET_SPEED_AND_PITCH,
      COMMAND_SET_REPEAT_MODE,
      COMMAND_GET_CURRENT_MEDIA_ITEM,
      COMMAND_GET_MEDIA_ITEMS_METADATA,
      COMMAND_SET_MEDIA_ITEM,
      COMMAND_GET_AUDIO_ATTRIBUTES,
      COMMAND_GET_VOLUME,
      COMMAND_GET_TEXT,
      COMMAND_SET_TRACK_SELECTION_PARAMETERS,
      COMMAND_GET_TRACKS
    )
  }

  protected var latestPlayWhenReady: Boolean = false

  override fun getState(): State {
    val allowedCommands = Player.Commands.Builder().apply {
      SUPPORTED_AUDIO_COMMANDS.forEach {
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
      true -> audioPlayer.start()
      false -> audioPlayer.pause()
    }

    return Futures.immediateVoidFuture()
  }

  override fun handlePrepare(): ListenableFuture<*> {
    // ExoMedia implementations automatically prepare the media
    return Futures.immediateVoidFuture()
  }

  override fun handleStop(): ListenableFuture<*> {
    this.latestPlayWhenReady = false
    audioPlayer.stop()

    return Futures.immediateVoidFuture()
  }

  override fun handleRelease(): ListenableFuture<*> {
    audioPlayer.release()
    return Futures.immediateVoidFuture()
  }

  override fun handleSetRepeatMode(repeatMode: Int): ListenableFuture<*> {
    audioPlayer.setRepeatMode(repeatMode)
    return Futures.immediateVoidFuture()
  }

  override fun handleSetPlaybackParameters(playbackParameters: PlaybackParameters): ListenableFuture<*> {
    audioPlayer.setPlaybackSpeed(playbackParameters.speed)
    audioPlayer.setPlaybackPitch(playbackParameters.pitch)

    return Futures.immediateVoidFuture()
  }

  override fun handleSetTrackSelectionParameters(trackSelectionParameters: TrackSelectionParameters): ListenableFuture<*> {
    audioPlayer.setTrackSelectionParameters(trackSelectionParameters)
    return Futures.immediateVoidFuture()
  }

  override fun handleSetVolume(volume: Float): ListenableFuture<*> {
    audioPlayer.volume = volume
    return Futures.immediateVoidFuture()
  }

  override fun handleSetMediaItems(mediaItems: MutableList<MediaItem>, startIndex: Int, startPositionMs: Long): ListenableFuture<*> {
    // We only support setting a single media item
    val mediaSource = mediaItems.firstOrNull()?.let {
      playerConfig.mediaSourceFactory.createMediaSource(it)
    }

    audioPlayer.setMedia(mediaSource)
    if (startPositionMs > 0) {
      audioPlayer.seekTo(startPositionMs)
    }

    return Futures.immediateVoidFuture()
  }

  override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int): ListenableFuture<*> {
    // We only support a single media item, so we ignore the index
    audioPlayer.seekTo(positionMs)
    return Futures.immediateVoidFuture()
  }
}