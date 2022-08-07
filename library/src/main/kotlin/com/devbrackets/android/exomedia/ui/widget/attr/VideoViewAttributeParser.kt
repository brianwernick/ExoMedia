package com.devbrackets.android.exomedia.ui.widget.attr

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.util.Log
import androidx.annotation.StyleableRes
import com.devbrackets.android.exomedia.R
import com.devbrackets.android.exomedia.core.video.scale.ScaleType
import com.devbrackets.android.exomedia.nmp.config.DefaultPlayerConfigProvider
import com.devbrackets.android.exomedia.nmp.config.PlayerConfigProvider
import com.devbrackets.android.exomedia.ui.widget.controls.VideoControlsProvider

class VideoViewAttributeParser {
  companion object {
    private const val LOG_TAG = "VideoViewAttrParser"
  }

  fun parse(context: Context, attrs: AttributeSet?): VideoViewAttributes {
    if (attrs == null) {
      return VideoViewAttributes()
    }

    val typedArray = context.obtainStyledAttributes(attrs, R.styleable.VideoView)

    val useTextureViewBacking = typedArray.getBoolean(R.styleable.VideoView_useTextureViewBacking, false)

    val scaleType = if (typedArray.hasValue(R.styleable.VideoView_videoScale)) {
      ScaleType.fromOrdinal(typedArray.getInt(R.styleable.VideoView_videoScale, -1))
    } else {
      null
    }

    val measureBasedOnAspectRatio = if (typedArray.hasValue(R.styleable.VideoView_measureBasedOnAspectRatio)) {
      typedArray.getBoolean(R.styleable.VideoView_measureBasedOnAspectRatio, false)
    } else {
      false
    }

    val configProvider: PlayerConfigProvider = typedArray.getOrDefault(R.styleable.VideoView_playerConfigProvider) {
      DefaultPlayerConfigProvider()
    }

    val controlsProvider = typedArray.getOrDefault(R.styleable.VideoView_videoControlsProvider) {
      VideoControlsProvider()
    }

    typedArray.recycle()

    return VideoViewAttributes(
      useTextureViewBacking = useTextureViewBacking,
      scaleType = scaleType,
      measureBasedOnAspectRatio = measureBasedOnAspectRatio,
      playerConfigProvider = configProvider,
      videoControlsProvider = controlsProvider
    )
  }

  @Suppress("UNCHECKED_CAST")
  private fun <T> TypedArray.getOrDefault(@StyleableRes styleable: Int, default: () -> T): T {
    val className = getString(styleable)
    if (className.isNullOrBlank()) {
      return default()
    }

    return try {
      Class.forName(className).getConstructor().newInstance() as T
    } catch (e: Exception) {
      Log.e(LOG_TAG, "Unable to construct class for name $className", e)
      default()
    }
  }
}