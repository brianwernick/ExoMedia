package com.devbrackets.android.exomedia.core.video.surface

import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.devbrackets.android.exomedia.core.video.scale.MatrixManager

class SurfaceViewSurfaceEnvelope(
  private val surface: SurfaceView,
  matrixManager: MatrixManager
) : BaseSurfaceEnvelope<SurfaceView>(surface, matrixManager), SurfaceHolder.Callback {

  init {
    surface.holder.addCallback(this)
  }

  override fun getSurface(): Surface? {
    return surface.holder.surface
  }

  override fun release() {
    super.release()
    surface.holder.removeCallback(this)
  }

  override fun surfaceCreated(holder: SurfaceHolder) {
    delegatingCallback.onSurfaceAvailable(this)
  }

  override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    delegatingCallback.onSurfaceSizeChanged(this, width, height)
  }

  override fun surfaceDestroyed(holder: SurfaceHolder) {
    delegatingCallback.onSurfaceDestroyed(this)
  }
}