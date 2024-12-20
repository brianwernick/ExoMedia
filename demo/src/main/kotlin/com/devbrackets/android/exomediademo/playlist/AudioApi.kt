package com.devbrackets.android.exomediademo.playlist

import android.content.Context
import android.net.Uri
import android.os.PowerManager
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.util.EventLogger
import com.devbrackets.android.exomedia.AudioPlayer
import com.devbrackets.android.exomediademo.data.MediaItem
import com.devbrackets.android.playlistcore.manager.BasePlaylistManager

class AudioApi(context: Context) : BaseMediaApi() {
  private val audioPlayer: AudioPlayer = AudioPlayer(context.applicationContext)

  override val isPlaying: Boolean
    get() = audioPlayer.isPlaying

  override val handlesOwnAudioFocus: Boolean
    get() = false

  override val currentPosition: Long
    get() = audioPlayer.currentPosition

  override val duration: Long
    get() = audioPlayer.duration

  override val bufferedPercent: Int
    get() = bufferPercent

  init {
    audioPlayer.setOnErrorListener(this)
    audioPlayer.setOnPreparedListener(this)
    audioPlayer.setOnCompletionListener(this)
    audioPlayer.setOnSeekCompletionListener(this)
    audioPlayer.setOnBufferUpdateListener(this)

    audioPlayer.setWakeLevel(PowerManager.PARTIAL_WAKE_LOCK)
    audioPlayer.setAudioAttributes(getAudioAttributes(C.USAGE_MEDIA, C.AUDIO_CONTENT_TYPE_MUSIC))
    audioPlayer.setAnalyticsListener(EventLogger(TAG))
  }

  override fun play() {
    audioPlayer.start()
  }

  override fun pause() {
    audioPlayer.pause()
  }

  override fun stop() {
    audioPlayer.stop()
  }

  override fun reset() {
    audioPlayer.reset()
  }

  override fun release() {
    audioPlayer.release()
  }

  override fun setVolume(@FloatRange(from = 0.0, to = 1.0) left: Float, @FloatRange(from = 0.0, to = 1.0) right: Float) {
    audioPlayer.volume = (left + right) / 2
  }

  override fun seekTo(@IntRange(from = 0L) milliseconds: Long) {
    audioPlayer.seekTo(milliseconds.toInt().toLong())
  }

  override fun handlesItem(item: MediaItem): Boolean {
    return item.mediaType == BasePlaylistManager.AUDIO
  }

  override fun playItem(item: MediaItem) {
    try {
      bufferPercent = 0
      audioPlayer.setMedia(Uri.parse(if (item.downloaded) item.downloadedMediaUri else item.mediaUrl))
    } catch (e: Exception) {
      //Purposefully left blank
    }
  }

  @Suppress("SameParameterValue")
  private fun getAudioAttributes(@C.AudioUsage usage: Int, @C.AudioContentType contentType: Int): AudioAttributes {
    return AudioAttributes.Builder()
      .setUsage(usage)
      .setContentType(contentType)
      .build()
  }

  companion object {
    private const val TAG = "AudioApi"
  }
}
