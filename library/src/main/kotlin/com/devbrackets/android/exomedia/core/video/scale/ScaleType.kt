package com.devbrackets.android.exomedia.core.video.scale

/**
 * See [android.widget.ImageView.ScaleType] for a description
 * for each type
 */
enum class ScaleType {
  CENTER,
  CENTER_CROP,
  CENTER_INSIDE,
  FIT_CENTER,
  FIT_XY,
  NONE;


  companion object {
    /**
     * Retrieves the [ScaleType] with the specified `ordinal`. If
     * the ordinal is outside the allowed ordinals then [.NONE] will be returned
     *
     * @param ordinal The ordinal value for the [ScaleType] to retrieve
     * @return The [ScaleType] associated with the `ordinal`
     */
    fun fromOrdinal(ordinal: Int): ScaleType {
      return values().getOrElse(ordinal) { NONE }
    }
  }
}