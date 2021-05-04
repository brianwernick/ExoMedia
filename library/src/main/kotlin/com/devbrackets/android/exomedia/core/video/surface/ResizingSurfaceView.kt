/*
 * Copyright (C) 2016 - 2021 ExoMedia Contributors,
 * Copyright (C) 2014 The Android Open Source Project
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

package com.devbrackets.android.exomedia.core.video.surface

import android.content.Context
import android.content.res.Configuration
import android.graphics.Point
import androidx.annotation.IntRange
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.view.ViewTreeObserver
import com.devbrackets.android.exomedia.core.video.scale.MatrixManager
import com.devbrackets.android.exomedia.core.video.scale.ScaleType
import java.util.concurrent.locks.ReentrantLock
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext

/**
 * A SurfaceView that reSizes itself according to the requested layout type
 * once we have a video
 */
open class ResizingSurfaceView : SurfaceView, VideoSurface {
  protected var onSizeChangeListener: OnSizeChangeListener? = null

  protected var lastNotifiedSize = Point(0, 0)
  protected var videoSize = Point(0, 0)

  protected var matrixManager = MatrixManager()

  protected var attachedListener: OnAttachStateChangeListener = AttachedListener()
  protected var globalLayoutMatrixListener: ViewTreeObserver.OnGlobalLayoutListener = GlobalLayoutMatrixListener()
  protected val globalLayoutMatrixListenerLock = ReentrantLock(true)

  @IntRange(from = 0, to = 359)
  protected var requestedUserRotation = 0

  @IntRange(from = 0, to = 359)
  protected var requestedConfigurationRotation = 0

  protected var measureBasedOnAspectRatio = false

  override var scaleType: ScaleType
    get() = matrixManager.currentScaleType
    set(scaleType) {
      matrixManager.scale(this, scaleType)
    }

  interface OnSizeChangeListener {
    fun onVideoSurfaceSizeChange(width: Int, height: Int)
  }

  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
  constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    if (!measureBasedOnAspectRatio) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec)
      notifyOnSizeChangeListener(measuredWidth, measuredHeight)
      return
    }

    var width = View.getDefaultSize(videoSize.x, widthMeasureSpec)
    var height = View.getDefaultSize(videoSize.y, heightMeasureSpec)

    if (videoSize.x <= 0 || videoSize.y <= 0) {
      setMeasuredDimension(width, height)
      notifyOnSizeChangeListener(width, height)
      return
    }

    val widthSpecMode = MeasureSpec.getMode(widthMeasureSpec)
    val widthSpecSize = MeasureSpec.getSize(widthMeasureSpec)
    val heightSpecMode = MeasureSpec.getMode(heightMeasureSpec)
    val heightSpecSize = MeasureSpec.getSize(heightMeasureSpec)

    if (widthSpecMode == MeasureSpec.EXACTLY && heightSpecMode == MeasureSpec.EXACTLY) {
      width = widthSpecSize
      height = heightSpecSize

      // for compatibility, we adjust size based on aspect ratio
      if (videoSize.x * height < width * videoSize.y) {
        width = height * videoSize.x / videoSize.y
      } else if (videoSize.x * height > width * videoSize.y) {
        height = width * videoSize.y / videoSize.x
      }
    } else if (widthSpecMode == MeasureSpec.EXACTLY) {
      // only the width is fixed, adjust the height to match aspect ratio if possible
      width = widthSpecSize
      height = width * videoSize.y / videoSize.x
      if (heightSpecMode == MeasureSpec.AT_MOST && height > heightSpecSize) {
        // couldn't match aspect ratio within the constraints
        height = heightSpecSize
      }
    } else if (heightSpecMode == MeasureSpec.EXACTLY) {
      // only the height is fixed, adjust the width to match aspect ratio if possible
      height = heightSpecSize
      width = height * videoSize.x / videoSize.y
      if (widthSpecMode == MeasureSpec.AT_MOST && width > widthSpecSize) {
        // couldn't match aspect ratio within the constraints
        width = widthSpecSize
      }
    } else {
      // neither the width nor the height are fixed, try to use actual video size
      width = videoSize.x
      height = videoSize.y
      if (heightSpecMode == MeasureSpec.AT_MOST && height > heightSpecSize) {
        // too tall, decrease both width and height
        height = heightSpecSize
        width = height * videoSize.x / videoSize.y
      }
      if (widthSpecMode == MeasureSpec.AT_MOST && width > widthSpecSize) {
        // too wide, decrease both width and height
        width = widthSpecSize
        height = width * videoSize.y / videoSize.x
      }
    }

    setMeasuredDimension(width, height)
    notifyOnSizeChangeListener(width, height)
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    updateMatrixOnLayout()
    super.onConfigurationChanged(newConfig)
  }

  /**
   * Clears the frames from the current surface.  This should only be called when
   * the implementing video view has finished playback or otherwise released
   * the surface
   */
  override fun clearSurface() {
    try {
      val gl10 = EGLContext.getEGL() as EGL10
      val display = gl10.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
      gl10.eglInitialize(display, null)

      val configs = arrayOfNulls<EGLConfig>(1)
      gl10.eglChooseConfig(display, GL_CLEAR_CONFIG_ATTRIBUTES, configs, configs.size, IntArray(1))
      val context = gl10.eglCreateContext(display, configs[0], EGL10.EGL_NO_CONTEXT, GL_CLEAR_CONTEXT_ATTRIBUTES)
      val eglSurface = gl10.eglCreateWindowSurface(display, configs[0], this, intArrayOf(EGL10.EGL_NONE))

      gl10.eglMakeCurrent(display, eglSurface, eglSurface, context)
      gl10.eglSwapBuffers(display, eglSurface)
      gl10.eglDestroySurface(display, eglSurface)
      gl10.eglMakeCurrent(display, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT)
      gl10.eglDestroyContext(display, context)

      gl10.eglTerminate(display)
    } catch (e: Exception) {
      Log.e(TAG, "Error clearing surface", e)
    }

  }

  override fun updateVideoSize(width: Int, height: Int): Boolean {
    matrixManager.setIntrinsicVideoSize(width, height)
    updateMatrixOnLayout()

    videoSize.x = width
    videoSize.y = height

    return width != 0 && height != 0
  }

  override fun setMeasureBasedOnAspectRatioEnabled(enabled: Boolean) {
    this.measureBasedOnAspectRatio = enabled
    requestLayout()
  }

  override fun setVideoRotation(@IntRange(from = 0, to = 359) rotation: Int, fromUser: Boolean) {
    setVideoRotation(if (fromUser) rotation else requestedUserRotation, if (!fromUser) rotation else requestedConfigurationRotation)
  }

  override fun onVideoSizeChanged(width: Int, height: Int, pixelWidthHeightRatio: Float) {
    if (updateVideoSize((width * pixelWidthHeightRatio).toInt(), height)) {
      requestLayout()
    }
  }

  /**
   * Specifies the rotation that should be applied to the video for both the user
   * requested value and the value specified in the videos configuration.
   *
   * @param userRotation The rotation the user wants to apply
   * @param configurationRotation The rotation specified in the configuration for the video
   */
  fun setVideoRotation(@IntRange(from = 0, to = 359) userRotation: Int, @IntRange(from = 0, to = 359) configurationRotation: Int) {
    requestedUserRotation = userRotation
    requestedConfigurationRotation = configurationRotation

    matrixManager.rotate(this, (userRotation + configurationRotation) % MAX_DEGREES)
  }

  /**
   * Requests for the Matrix to be updated on layout changes.  This will
   * ensure that the scaling is correct and the rotation is not lost or
   * applied incorrectly.
   */
  protected fun updateMatrixOnLayout() {
    globalLayoutMatrixListenerLock.lock()

    // if we're not attached defer adding the layout repeatListener until we are
    if (windowToken == null) {
      addOnAttachStateChangeListener(attachedListener)
    } else {
      viewTreeObserver.addOnGlobalLayoutListener(globalLayoutMatrixListener)
    }

    globalLayoutMatrixListenerLock.unlock()
  }

  /**
   * Performs the functionality to notify the repeatListener that the
   * size of the surface has changed filtering out duplicate calls.
   *
   * @param width The new width
   * @param height The new height
   */
  protected fun notifyOnSizeChangeListener(width: Int, height: Int) {
    if (lastNotifiedSize.x == width && lastNotifiedSize.y == height) {
      return
    }

    lastNotifiedSize.x = width
    lastNotifiedSize.y = height

    updateMatrixOnLayout()
    onSizeChangeListener?.onVideoSurfaceSizeChange(width, height)
  }

  /**
   * This is separated from the [ResizingSurfaceView.onAttachedToWindow]
   * so that we have control over when it is added and removed
   */
  private inner class AttachedListener : OnAttachStateChangeListener {
    override fun onViewAttachedToWindow(view: View) {
      globalLayoutMatrixListenerLock.lock()

      viewTreeObserver.addOnGlobalLayoutListener(globalLayoutMatrixListener)
      removeOnAttachStateChangeListener(this)

      globalLayoutMatrixListenerLock.unlock()
    }

    override fun onViewDetachedFromWindow(view: View) {
      //Purposefully left blank
    }
  }

  /**
   * Listens to the global layout to reapply the scale
   */
  private inner class GlobalLayoutMatrixListener : ViewTreeObserver.OnGlobalLayoutListener {
    override fun onGlobalLayout() {
      // Updates the scale to make sure one is applied
      scaleType = matrixManager.currentScaleType
      viewTreeObserver.removeOnGlobalLayoutListener(this)
    }
  }

  companion object {
    private val TAG = "ResizingSurfaceView"
    protected val MAX_DEGREES = 360

    /**
     * A version of the EGL14.EGL_CONTEXT_CLIENT_VERSION so that we can
     * reference it without being on API 17+
     */
    private val EGL_CONTEXT_CLIENT_VERSION = 0x3098

    /**
     * Because the TextureView itself doesn't contain a method to clear the surface
     * we need to use GL to perform the clear ourselves.  This means initializing
     * a GL context, and specifying attributes.  This is the attribute list for
     * the configuration of that context
     */
    private val GL_CLEAR_CONFIG_ATTRIBUTES = intArrayOf(
        EGL10.EGL_RED_SIZE, 8,
        EGL10.EGL_GREEN_SIZE, 8,
        EGL10.EGL_BLUE_SIZE, 8,
        EGL10.EGL_ALPHA_SIZE, 8,
        EGL10.EGL_RENDERABLE_TYPE,
        EGL10.EGL_WINDOW_BIT,
        EGL10.EGL_NONE,
        0,
        EGL10.EGL_NONE
    )

    /**
     * Because the TextureView itself doesn't contain a method to clear the surface
     * we need to use GL to perform teh clear ourselves.  This means initializing
     * a GL context, and specifying attributes.  This is the attribute list for
     * that context
     */
    private val GL_CLEAR_CONTEXT_ATTRIBUTES = intArrayOf(EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE)
  }
}