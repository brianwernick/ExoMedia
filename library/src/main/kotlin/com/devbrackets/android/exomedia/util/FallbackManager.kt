package com.devbrackets.android.exomedia.util

import android.content.Context
import android.os.Build
import com.devbrackets.android.exomedia.core.audio.AudioPlayerApi
import com.devbrackets.android.exomedia.core.video.VideoPlayerApi
import com.devbrackets.android.exomedia.core.video.surface.VideoSurface
import com.devbrackets.android.exomedia.fallback.audio.NativeAudioPlayer
import com.devbrackets.android.exomedia.fallback.video.NativeVideoPlayer
import java.util.*

/**
 * Determines if, and provides the fallback media player implementations on
 * devices that don't support the ExoPlayer.
 */
open class FallbackManager {
  private val incompatibleDevices = mapOf(
      "amazon" to DeviceModels(allModels = true)
  )

  /**
   * Determines if the ExoPlayer implementation or the fallback media player
   * should be used to play media.
   */
  open fun useFallback(): Boolean {
    return incompatibleDevices[Build.MANUFACTURER.toLowerCase(Locale.getDefault())]?.let {
      it.allModels || it.models.contains(Build.DEVICE.toLowerCase(Locale.getDefault()))
    } ?: false
  }

  /**
   * Retrieves an [AudioPlayerApi] implementation to use in situations where the
   * ExoPlayer isn't supported.
   */
  open fun getFallbackAudioPlayer(context: Context): AudioPlayerApi {
    return NativeAudioPlayer(context)
  }

  /**
   * Retrieves a [VideoPlayerApi] implementation to use in situations where the
   * ExoPlayer isn't supported
   */
  open fun getFallbackVideoPlayer(context: Context, surface: VideoSurface): VideoPlayerApi {
    return NativeVideoPlayer(context, surface)
  }

  data class DeviceModels(
      val models: Set<String> = emptySet(),
      val allModels: Boolean = false
  )
}