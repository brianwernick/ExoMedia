/*
 * Copyright (C) 2016 - 2019 ExoMedia Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.devbrackets.android.exomedia.core.video.scale

import android.graphics.Point
import androidx.annotation.IntRange
import android.util.Log
import android.view.View

import java.lang.ref.WeakReference
import kotlin.math.max
import kotlin.math.min

open class MatrixManager {
  companion object {
    private const val TAG = "MatrixManager"
    protected const val QUARTER_ROTATION = 90
  }

  protected var intrinsicVideoSize = Point(0, 0)

  @IntRange(from = 0, to = 359)
  protected var currentRotation = 0
    get() = requestedRotation ?: field

  var currentScaleType = ScaleType.FIT_CENTER
    get() = requestedScaleType ?: field

  protected var requestedRotation: Int? = null
  protected var requestedScaleType: ScaleType? = null
  protected var requestedModificationView = WeakReference<View>(null)

  fun reset() {
    setIntrinsicVideoSize(0, 0)
    currentRotation = 0
  }

  fun ready(): Boolean {
    return intrinsicVideoSize.x > 0 && intrinsicVideoSize.y > 0
  }

  fun setIntrinsicVideoSize(@IntRange(from = 0) width: Int, @IntRange(from = 0) height: Int) {
    val currentWidthHeightSwapped = currentRotation / QUARTER_ROTATION % 2 == 1
    intrinsicVideoSize.x = if (currentWidthHeightSwapped) height else width
    intrinsicVideoSize.y = if (currentWidthHeightSwapped) width else height

    if (ready()) {
      applyRequestedModifications()
    }
  }

  fun rotate(view: View, @IntRange(from = 0, to = 359) rotation: Int) {
    if (!ready()) {
      requestedRotation = rotation
      requestedModificationView = WeakReference(view)
      return
    }

    val swapWidthHeight = rotation / QUARTER_ROTATION % 2 == 1
    val currentWidthHeightSwapped = currentRotation / QUARTER_ROTATION % 2 == 1

    //Makes sure the width and height are correctly swapped
    if (swapWidthHeight != currentWidthHeightSwapped) {
      val tempX = intrinsicVideoSize.x
      intrinsicVideoSize.x = intrinsicVideoSize.y
      intrinsicVideoSize.y = tempX

      //We re-apply the scale to make sure it is correct
      scale(view, currentScaleType)
    }

    currentRotation = rotation
    view.rotation = rotation.toFloat()
  }

  /**
   * Performs the requested scaling on the `view`'s matrix
   *
   * @param view The View to alter the matrix to achieve the requested scale type
   * @param scaleType The type of scaling to use for the specified view
   * @return True if the scale was applied
   */
  fun scale(view: View, scaleType: ScaleType): Boolean {
    if (!ready()) {
      requestedScaleType = scaleType
      requestedModificationView = WeakReference(view)
      return false
    }

    if (view.height == 0 || view.width == 0) {
      Log.d(TAG, "Unable to apply scale with a view size of (" + view.width + ", " + view.height + ")")
      return false
    }

    currentScaleType = scaleType
    when (scaleType) {
      ScaleType.CENTER -> applyCenter(view)
      ScaleType.CENTER_CROP -> applyCenterCrop(view)
      ScaleType.CENTER_INSIDE -> applyCenterInside(view)
      ScaleType.FIT_CENTER -> applyFitCenter(view)
      ScaleType.FIT_XY -> applyFitXy(view)
      ScaleType.NONE -> setScale(view, 1f, 1f)
    }

    return true
  }

  /**
   * Applies the [ScaleType.CENTER] to the specified matrix.  This will
   * perform no scaling as this just indicates that the video should be centered
   * in the View
   *
   * @param view The view to apply the transformation to
   */
  protected fun applyCenter(view: View) {
    val xScale = intrinsicVideoSize.x.toFloat() / view.width
    val yScale = intrinsicVideoSize.y.toFloat() / view.height

    setScale(view, xScale, yScale)
  }

  /**
   * Applies the [ScaleType.CENTER_CROP] to the specified matrix.  This will
   * make sure the smallest side fits the parent container, cropping the other
   *
   * @param view The view to apply the transformation to
   */
  protected fun applyCenterCrop(view: View) {
    var xScale = view.width.toFloat() / intrinsicVideoSize.x
    var yScale = view.height.toFloat() / intrinsicVideoSize.y

    val scale = max(xScale, yScale)
    xScale = scale / xScale
    yScale = scale / yScale

    setScale(view, xScale, yScale)
  }

  /**
   * Applies the [ScaleType.CENTER_INSIDE] to the specified matrix.  This will
   * only perform scaling if the video is too large to fit completely in the `view`
   * in which case it will be scaled to fit
   *
   * @param view The view to apply the transformation to
   */
  protected fun applyCenterInside(view: View) {
    if (intrinsicVideoSize.x <= view.width && intrinsicVideoSize.y <= view.height) {
      applyCenter(view)
    } else {
      applyFitCenter(view)
    }
  }

  /**
   * Applies the [ScaleType.FIT_CENTER] to the specified matrix.  This will
   * scale the video so that the largest side will always match the `view`
   *
   * @param view The view to apply the transformation to
   */
  protected fun applyFitCenter(view: View) {
    var xScale = view.width.toFloat() / intrinsicVideoSize.x
    var yScale = view.height.toFloat() / intrinsicVideoSize.y

    val scale = min(xScale, yScale)
    xScale = scale / xScale
    yScale = scale / yScale
    setScale(view, xScale, yScale)
  }

  /**
   * Applies the [ScaleType.FIT_XY] to the specified matrix.  This will
   * scale the video so that both the width and height will always match that of
   * the `view`
   *
   * @param view The view to apply the transformation to
   */
  protected fun applyFitXy(view: View) {
    setScale(view, 1f, 1f)
  }

  /**
   * Applies the specified scale modification to the view
   *
   * @param view The view to scale
   * @param xScale The scale to apply to the x axis
   * @param yScale The scale to apply to the y axis
   */
  protected fun setScale(view: View, xScale: Float, yScale: Float) {
    //If the width and height have been swapped, we need to re-calculate the scales based on the swapped sizes
    val currentWidthHeightSwapped = currentRotation / QUARTER_ROTATION % 2 == 1
    if (currentWidthHeightSwapped) {
      view.scaleX = yScale * view.height / view.width
      view.scaleY = xScale * view.width / view.height
      return
    }

    view.scaleX = xScale
    view.scaleY = yScale
  }

  /**
   * Applies any scale or rotation that was requested before the MatrixManager was
   * ready to apply those modifications.
   */
  protected fun applyRequestedModifications() {
    requestedModificationView.get()?.let { view ->
      requestedRotation?.let {
        rotate(view, it)
        requestedRotation = null
      }

      requestedScaleType?.let {
        scale(view, it)
        requestedScaleType = null
      }
    }

    requestedModificationView = WeakReference<View>(null)
  }
}