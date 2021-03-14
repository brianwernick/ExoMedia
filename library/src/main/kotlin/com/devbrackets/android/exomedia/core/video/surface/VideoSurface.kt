package com.devbrackets.android.exomedia.core.video.surface

import com.devbrackets.android.exomedia.core.video.scale.ScaleType
import androidx.annotation.IntRange

interface VideoSurface {
  /**
   * Defines how the video is scaled within the surface.
   */
  var scaleType: ScaleType

  fun clearSurface()

  /**
   * Specifies if the [.onMeasure] should pay attention to the specified
   * aspect ratio for the video (determined from [.videoSize].
   *
   * @param enabled `true` if [onMeasure] should pay attention to the videos aspect ratio
   */
  fun setMeasureBasedOnAspectRatioEnabled(enabled: Boolean)

  /**
   * Sets the rotation for the Video
   *
   * @param rotation The rotation to apply to the video
   * @param fromUser True if the rotation was requested by the user, false if it is from a video configuration
   */
  fun setVideoRotation(@IntRange(from = 0, to = 359) rotation: Int, fromUser: Boolean)

  /**
   * Notifies the surface that the video being drawn has changed size. This can be used
   * to determine if and how the video needs to be scaled.
   *
   * @param width The unscaled width of the video being drawn to this surface
   * @param height The unscaled height of the video being drawn to this surface
   * @param pixelWidthHeightRatio The pixel ratio of the width:height of the video being drawn to this surface
   */
  fun onVideoSizeChanged(width: Int, height: Int, pixelWidthHeightRatio: Float)

  /**
   * Updates the stored videoSize and updates the default buffer size
   * in the backing texture view.
   *
   * @param width The width for the video
   * @param height The height for the video
   * @return True if the surfaces DefaultBufferSize was updated
   */
  fun updateVideoSize(width: Int, height: Int): Boolean

}