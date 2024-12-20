package com.devbrackets.android.exomediademo.ui.media.audio

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.devbrackets.android.exomediademo.App
import com.devbrackets.android.exomediademo.data.MediaItem
import com.devbrackets.android.exomediademo.data.Samples
import com.devbrackets.android.exomediademo.playlist.manager.PlaylistManager
import com.devbrackets.android.exomediademo.ui.media.AudioPlayerActivity
import com.devbrackets.android.exomediademo.ui.support.compose.seek.SeekState
import com.devbrackets.android.playlistcore.data.MediaProgress
import com.devbrackets.android.playlistcore.data.PlaybackState
import com.devbrackets.android.playlistcore.listener.PlaylistListener
import com.devbrackets.android.playlistcore.listener.ProgressListener
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

internal class AudioPlayerViewModel(
  private val savedStateHandle: SavedStateHandle,
  val playlistManager: PlaylistManager
) : ViewModel(), PlaylistListener<MediaItem>, ProgressListener {
  companion object {
    fun factory(context: Context): ViewModelProvider.Factory {
      return Factory(context.applicationContext as Application)
    }
  }

  private val selectedPosition by lazy {
    savedStateHandle.get<Int>(AudioPlayerActivity.EXTRA_INDEX) ?: 0
  }

  private val mutablePlaybackState = MutableStateFlow<PlaybackState?>(null)
  val playbackState: Flow<PlaybackState?> = mutablePlaybackState

  private val mutablePlaybackItem = MutableStateFlow<PlaybackItem?>(null)
  val playbackItem: Flow<PlaybackItem?> = mutablePlaybackItem

  // PlaylistCore just modifies a single object so we use a SharedFlow instead
  // of a StateFlow
  private val mutableMediaProgress = MutableSharedFlow<MediaProgress?>(
    replay = 1,
    extraBufferCapacity = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
  )
  val playbackPosition: Flow<Long?> = mutableMediaProgress.map { it?.position }
  val playbackDuration: Flow<Long?> = mutableMediaProgress.map { it?.duration }

  override fun onPlaybackStateChanged(playbackState: PlaybackState): Boolean {
    mutablePlaybackState.value = playbackState
    return true
  }

  override fun onPlaylistItemChanged(currentItem: MediaItem?, hasNext: Boolean, hasPrevious: Boolean): Boolean {
    val item = PlaybackItem(
      title = currentItem?.title,
      album = currentItem?.album,
      artworkUrl = currentItem?.artworkUrl,
      hasNext = hasNext,
      hasPrevious = hasPrevious
    )

    mutablePlaybackItem.value = item
    mutableMediaProgress.tryEmit(null)

    return true
  }

  override fun onProgressUpdated(mediaProgress: MediaProgress): Boolean {
    mutableMediaProgress.tryEmit(mediaProgress)
    return true
  }

  override fun onCleared() {
    super.onCleared()

    playlistManager.unRegisterPlaylistListener(this)
    playlistManager.unRegisterProgressListener(this)
  }

  fun connectPlaylist() {
    playlistManager.registerPlaylistListener(this)
    playlistManager.registerProgressListener(this)

    // Makes sure to retrieve the current playback information
    updateCurrentPlaybackInformation()
  }

  fun playPause() {
    playlistManager.invokePausePlay()
  }

  fun seekPrevious() {
    playlistManager.invokePrevious()
  }

  fun seekNext() {
    playlistManager.invokeNext()
  }

  fun seek(state: SeekState) {
    when (state) {
      is SeekState.Started -> playlistManager.invokeSeekStarted()
      is SeekState.Seeking -> { /* No Op */ }
      is SeekState.Finished -> playlistManager.invokeSeekEnded(state.position)
    }
  }

  private fun updateCurrentPlaybackInformation() {
    playlistManager.currentItemChange?.let {
      onPlaylistItemChanged(it.currentItem, it.hasNext, it.hasPrevious)
    }

    if (playlistManager.currentPlaybackState != PlaybackState.STOPPED) {
      onPlaybackStateChanged(playlistManager.currentPlaybackState)
    }

    playlistManager.currentProgress?.let {
      onProgressUpdated(it)
    }
  }

  /**
   * Starts the audio playback if necessary.
   */
  @SuppressLint("Range")
  fun startPlayback() {
    val forceStart = setupPlaylistManager()

    //If we are changing audio files, or we haven't played before then start the playback
    if (forceStart || playlistManager.currentPosition != selectedPosition) {
      playlistManager.currentPosition = selectedPosition
      playlistManager.play(0, false)
    }
  }

  /**
   * Retrieves the playlist instance and performs any generation
   * of content if it hasn't already been performed.
   *
   * @return True if the content was generated
   */
  @SuppressLint("Range")
  private fun setupPlaylistManager(): Boolean {
    // There is nothing to do if the currently playing values are the same
    if (playlistManager.id == AudioPlayerActivity.PLAYLIST_ID.toLong()) {
      return false
    }

    val mediaItems = Samples.audio.map {
      MediaItem(it, true)
    }

    playlistManager.setParameters(mediaItems, selectedPosition)
    playlistManager.id = AudioPlayerActivity.PLAYLIST_ID.toLong()

    return true
  }

  data class PlaybackItem(
    val title: String?,
    val album: String?,
    val artworkUrl: String?,
    val hasNext: Boolean,
    val hasPrevious: Boolean
  )

  private class Factory(
    private val application: Application
  ) : AbstractSavedStateViewModelFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
      key: String,
      modelClass: Class<T>,
      handle: SavedStateHandle
    ): T {
      return AudioPlayerViewModel(
        savedStateHandle = handle,
        playlistManager = (application as App).playlistManager
      ) as T
    }
  }
}