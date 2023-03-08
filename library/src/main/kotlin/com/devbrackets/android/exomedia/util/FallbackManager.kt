package com.devbrackets.android.exomedia.util

import android.os.Build
import com.devbrackets.android.exomedia.core.audio.AudioPlayerApi
import com.devbrackets.android.exomedia.core.video.VideoPlayerApi
import com.devbrackets.android.exomedia.core.video.surface.SurfaceEnvelope
import com.devbrackets.android.exomedia.fallback.audio.NativeAudioPlayer
import com.devbrackets.android.exomedia.fallback.video.NativeVideoPlayer
import com.devbrackets.android.exomedia.nmp.config.PlayerConfig

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
    return incompatibleDevices[Build.MANUFACTURER.lowercase()]?.let {
      it.allModels || it.models.contains(Build.DEVICE.lowercase())
    } ?: false
  }

  /**
   * Retrieves an [AudioPlayerApi] implementation to use in situations where the
   * ExoPlayer isn't supported.
   */
  open fun getFallbackAudioPlayer(config: PlayerConfig): AudioPlayerApi {
    return NativeAudioPlayer(config)
  }

  /**
   * Retrieves a [VideoPlayerApi] implementation to use in situations where the
   * ExoPlayer isn't supported
   */
  open fun getFallbackVideoPlayer(config: PlayerConfig, surface: SurfaceEnvelope): VideoPlayerApi {
    return NativeVideoPlayer(config, surface)
  }

  data class DeviceModels(
    val models: Set<String> = emptySet(),
    val allModels: Boolean = false
  )
}