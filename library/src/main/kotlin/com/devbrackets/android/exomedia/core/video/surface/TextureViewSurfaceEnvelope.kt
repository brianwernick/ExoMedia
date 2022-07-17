package com.devbrackets.android.exomedia.core.video.surface

import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import com.devbrackets.android.exomedia.core.video.scale.MatrixManager

class TextureViewSurfaceEnvelope(
  private val surface: TextureView,
  matrixManager: MatrixManager
) : BaseSurfaceEnvelope<TextureView>(surface, matrixManager), TextureView.SurfaceTextureListener {
  private var backingSurface: Surface? = null

  init {
    surface.surfaceTextureListener = this
  }

  override fun getSurface(): Surface? {
    return backingSurface
  }

  override fun setVideoSize(width: Int, height: Int): Boolean {
    if (!super.setVideoSize(width, height)) {
      return false
    }

    surface.surfaceTexture?.setDefaultBufferSize(width, height) ?: return false

    return true
  }

  override fun release() {
    super.release()

    backingSurface = null
    surface.surfaceTextureListener = null
  }

  override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
    backingSurface = Surface(surface)
    delegatingCallback.onSurfaceAvailable(this)
  }

  override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
    delegatingCallback.onSurfaceSizeChanged(this, width, height)
  }

  override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
    delegatingCallback.onSurfaceDestroyed(this)
    return true
  }

  override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
    // Purposefully left blank
  }
}