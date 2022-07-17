package com.devbrackets.android.exomedia.core.video.layout

import android.content.Context
import android.util.AttributeSet
import android.util.Size
import android.widget.FrameLayout

/**
 * A Layout that resizes itself based on the specified Aspect Ratio.
 * This is useful for keeping a SurfaceView or TextureView as the same
 * ratio as the content being played to avoid stretching or condensing
 * the video.
 */
class AspectRatioLayout @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
  private var ratioSize = Size(0, 0)

  /**
   * Determines if the aspect ratio specified in [setAspectRatio] should be used
   * when calculating the size of this layout.
   */
  var honorAspectRatio: Boolean = false
    set(value) {
      field = value
      refreshLayout()
    }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    if (!honorAspectRatio || ratioSize.width <= 0 || ratioSize.height <= 0) {
      return
    }

    val widthSpecMode = MeasureSpec.getMode(widthMeasureSpec)
    val heightSpecMode = MeasureSpec.getMode(heightMeasureSpec)

    val size = if (widthSpecMode == MeasureSpec.EXACTLY && heightSpecMode == MeasureSpec.EXACTLY) {
      measureExact(widthMeasureSpec, heightMeasureSpec)
    } else if (widthSpecMode == MeasureSpec.EXACTLY) {
      measureExactWidth(widthMeasureSpec, heightMeasureSpec)
    } else if (heightSpecMode == MeasureSpec.EXACTLY) {
      measureExactHeight(widthMeasureSpec, heightMeasureSpec)
    } else {
      measureDefault(widthMeasureSpec, heightMeasureSpec)
    }

    // Calls 'super' to ensure children are properly handled
    val newWidthSpec = MeasureSpec.makeMeasureSpec(size.width, MeasureSpec.EXACTLY)
    val newHeightSpec = MeasureSpec.makeMeasureSpec(size.height, MeasureSpec.EXACTLY)
    super.onMeasure(newWidthSpec, newHeightSpec)
  }

  fun setAspectRatio(width: Int, height: Int, pixelWidthHeightRatio: Float) {
    ratioSize = Size(
      (width * pixelWidthHeightRatio).toInt(),
      height
    )

    refreshLayout()
  }

  private fun measureExact(widthMeasureSpec: Int, heightMeasureSpec: Int): Size {
    val width = MeasureSpec.getSize(widthMeasureSpec)
    val height = MeasureSpec.getSize(heightMeasureSpec)

    val widthByRatio = height * ratioSize.width / ratioSize.height
    val heightByRatio = width * ratioSize.height / ratioSize.width

    if (widthByRatio > width) {
      return Size(width, heightByRatio)
    }

    return Size(widthByRatio, height)
  }

  private fun measureExactWidth(widthMeasureSpec: Int, heightMeasureSpec: Int): Size {
    val width = MeasureSpec.getSize(widthMeasureSpec)
    val height = coerceSize(width * ratioSize.height / ratioSize.width, heightMeasureSpec)

    return Size(width, height)
  }

  private fun measureExactHeight(widthMeasureSpec: Int, heightMeasureSpec: Int): Size {
    val height = MeasureSpec.getSize(heightMeasureSpec)
    val width = coerceSize(height * ratioSize.width / ratioSize.height, widthMeasureSpec)

    return Size(width, height)
  }

  private fun measureDefault(widthMeasureSpec: Int, heightMeasureSpec: Int): Size {
    var width = ratioSize.width
    var height = ratioSize.height

    if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST && height > MeasureSpec.getSize(heightMeasureSpec)) {
      height = MeasureSpec.getSize(heightMeasureSpec)
      width = height * ratioSize.width / ratioSize.height
    }

    if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.AT_MOST && width > MeasureSpec.getSize(widthMeasureSpec)) {
      width = MeasureSpec.getSize(widthMeasureSpec)
      height = width * ratioSize.height / ratioSize.width
    }

    return Size(width, height)
  }

  private fun coerceSize(size: Int, measureSpec: Int): Int {
    val specSize = MeasureSpec.getSize(measureSpec)
    if (MeasureSpec.getMode(measureSpec) == MeasureSpec.AT_MOST && size > specSize) {
      return specSize
    }

    return size
  }

  private fun refreshLayout() {
    if (!isInLayout) {
      requestLayout()
    }
  }
}