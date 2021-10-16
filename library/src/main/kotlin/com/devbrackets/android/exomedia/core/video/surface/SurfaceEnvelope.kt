package com.devbrackets.android.exomedia.core.video.surface

import android.view.Surface
import androidx.annotation.IntRange
import com.devbrackets.android.exomedia.core.video.scale.ScaleType

interface SurfaceEnvelope {
  interface Callback {
    fun onSurfaceAvailable(envelope: SurfaceEnvelope)
    fun onSurfaceDestroyed(envelope: SurfaceEnvelope)
    fun onSurfaceSizeChanged(envelope: SurfaceEnvelope, width: Int, height: Int)
  }

  /**
   * Retrieves the current surface that is wrapped by this envelope.
   * If the surface is not yet available or has been released then `null` will
   * be returned.
   */
  fun getSurface(): Surface?

  /**
   * Clears the frames from the current surface. This should only be called when
   * the implementing video view has finished playback or otherwise released
   * the surface
   */
  fun clearSurface()

  fun release()

  /**
   * Sets how the video should be scaled in the view
   *
   * @param type The type of scaling to use for the video
   */
  fun setScaleType(type: ScaleType)

  /**
   * Updates the stored videoSize and updates the default buffer size
   * in the backing texture view.
   *
   * @param width The width for the video
   * @param height The height for the video
   * @return True if the surfaces DefaultBufferSize was updated
   */
  fun setVideoSize(width: Int, height: Int): Boolean

  /**
   * Updates the stored videoSize and updates the default buffer size
   * in the backing texture view.
   *
   * @param width The width for the video
   * @param height The height for the video
   * @param pixelWidthHeightRatio The pixel ratio of the width:height of the video being drawn to this surface
   * @return True if the surfaces DefaultBufferSize was updated
   */
  fun setVideoSize(width: Int, height: Int, pixelWidthHeightRatio: Float): Boolean

  /**
   * Sets the rotation for the Video
   *
   * @param rotation The rotation to apply to the video
   * @param fromUser True if the rotation was requested by the user, false if it is from a video configuration
   */
  fun setVideoRotation(
    @IntRange(from = 0, to = 359) rotation: Int,
    fromUser: Boolean
  )

  fun addCallback(callback: Callback)

  fun removeCallback(callback: Callback)
}