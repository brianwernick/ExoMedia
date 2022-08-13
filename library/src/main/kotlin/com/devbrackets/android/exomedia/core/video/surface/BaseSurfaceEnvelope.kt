package com.devbrackets.android.exomedia.core.video.surface

import android.graphics.Point
import android.opengl.EGL14
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import androidx.annotation.IntRange
import com.devbrackets.android.exomedia.core.video.scale.MatrixManager
import com.devbrackets.android.exomedia.core.video.scale.ScaleType
import java.util.concurrent.locks.ReentrantLock
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext

abstract class BaseSurfaceEnvelope<T: View>(
  private val surface: T,
  private val matrixManager: MatrixManager
): View.OnAttachStateChangeListener, ViewTreeObserver.OnGlobalLayoutListener, SurfaceEnvelope {
  companion object {
    private const val MAX_DEGREES = 360

    /**
     * Because the TextureView itself doesn't contain a method to clear the surface
     * we need to use GL to perform the clear ourselves. This means initializing
     * a GL context, and specifying attributes. This is the attribute list for
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
     * we need to use GL to perform the clear ourselves. This means initializing
     * a GL context, and specifying attributes.  This is the attribute list for
     * that context
     */
    private val GL_CLEAR_CONTEXT_ATTRIBUTES = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE)
  }

  protected val delegatingCallback = DelegatingCallback()

  private var lastNotifiedSize = Point(0, 0)
  private var videoSize = Point(0, 0)

  private val globalLayoutMatrixListenerLock = ReentrantLock(true)

  @IntRange(from = 0, to = 359)
  protected var requestedUserRotation = 0

  @IntRange(from = 0, to = 359)
  protected var requestedConfigurationRotation = 0

  val scaleType: ScaleType
    get() = matrixManager.currentScaleType

  init {
    registerStateAndLayoutListeners()
  }

  override fun onViewAttachedToWindow(view: View) {
    globalLayoutMatrixListenerLock.lock()

    surface.viewTreeObserver.addOnGlobalLayoutListener(this)
    surface.removeOnAttachStateChangeListener(this)

    globalLayoutMatrixListenerLock.unlock()
  }

  override fun onViewDetachedFromWindow(view: View) {
    // Purposefully left blank
  }

  override fun onGlobalLayout() {
    // Updates the scale to make sure one is applied
    setScaleType(matrixManager.currentScaleType)
  }

  override fun release() {
    surface.viewTreeObserver.removeOnGlobalLayoutListener(this)
    surface.removeOnAttachStateChangeListener(this)
  }

  override fun setScaleType(type: ScaleType) {
    matrixManager.scale(surface, type)
  }

  override fun clearSurface() {
    try {
      val gl10 = EGLContext.getEGL() as EGL10
      val display = gl10.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
      gl10.eglInitialize(display, null)

      val configs = arrayOfNulls<EGLConfig>(1)
      gl10.eglChooseConfig(display, GL_CLEAR_CONFIG_ATTRIBUTES, configs, configs.size, IntArray(1))
      val context = gl10.eglCreateContext(display, configs[0], EGL10.EGL_NO_CONTEXT, GL_CLEAR_CONTEXT_ATTRIBUTES)
      val eglSurface = gl10.eglCreateWindowSurface(display, configs[0], surface, intArrayOf(EGL10.EGL_NONE))

      gl10.eglMakeCurrent(display, eglSurface, eglSurface, context)
      gl10.eglSwapBuffers(display, eglSurface)
      gl10.eglDestroySurface(display, eglSurface)
      gl10.eglMakeCurrent(display, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT)
      gl10.eglDestroyContext(display, context)

      gl10.eglTerminate(display)
    } catch (e: Exception) {
      Log.w("BaseSurfaceEnvelope", "Error clearing surface", e)
    }
  }

  override fun setVideoSize(width: Int, height: Int): Boolean {
    matrixManager.setIntrinsicVideoSize(width, height)

    videoSize.x = width
    videoSize.y = height

    return width != 0 && height != 0
  }

  override fun setVideoSize(width: Int, height: Int, pixelWidthHeightRatio: Float): Boolean {
    return setVideoSize((width * pixelWidthHeightRatio).toInt(), height)
  }

  override fun setVideoRotation(
    @IntRange(from = 0, to = 359) rotation: Int,
    fromUser: Boolean
  ) {
    if (fromUser) {
      setVideoRotation(rotation, requestedConfigurationRotation)
    } else {
      setVideoRotation(requestedUserRotation, rotation)
    }
  }

  override fun addCallback(callback: SurfaceEnvelope.Callback) {
    delegatingCallback.addCallback(callback)
  }

  override fun removeCallback(callback: SurfaceEnvelope.Callback) {
    delegatingCallback.removeCallback(callback)
  }

  /**
   * Specifies the rotation that should be applied to the video for both the user
   * requested value and the value specified in the videos configuration.
   *
   * @param userRotation The rotation the user wants to apply
   * @param configurationRotation The rotation specified in the configuration for the video
   */
  fun setVideoRotation(
    @IntRange(from = 0, to = 359) userRotation: Int,
    @IntRange(from = 0, to = 359) configurationRotation: Int
  ) {
    requestedUserRotation = userRotation
    requestedConfigurationRotation = configurationRotation

    matrixManager.rotate(surface, (userRotation + configurationRotation) % MAX_DEGREES)
  }

  private fun registerStateAndLayoutListeners() {
    globalLayoutMatrixListenerLock.lock()

    // If we're not attached defer adding the layout listener until we are
    if (surface.windowToken == null) {
      surface.addOnAttachStateChangeListener(this)
    } else {
      surface.viewTreeObserver.addOnGlobalLayoutListener(this)
    }

    globalLayoutMatrixListenerLock.unlock()
  }

  /**
   * A simple [SurfaceEnvelope.Callback] implementation that delegates the
   * calls to the registered callbacks.
   */
  protected class DelegatingCallback: SurfaceEnvelope.Callback {
    private val callbacks = mutableSetOf<SurfaceEnvelope.Callback>()

    override fun onSurfaceAvailable(envelope: SurfaceEnvelope) {
      callbacks.forEach { callback ->
        callback.onSurfaceAvailable(envelope)
      }
    }

    override fun onSurfaceDestroyed(envelope: SurfaceEnvelope) {
      callbacks.forEach { callback ->
        callback.onSurfaceDestroyed(envelope)
      }
    }

    override fun onSurfaceSizeChanged(envelope: SurfaceEnvelope, width: Int, height: Int) {
      callbacks.forEach { callback ->
        callback.onSurfaceSizeChanged(envelope, width, height)
      }
    }

    fun addCallback(callback: SurfaceEnvelope.Callback) {
      callbacks.add(callback)
    }

    fun removeCallback(callback: SurfaceEnvelope.Callback) {
      callbacks.remove(callback)
    }
  }
}