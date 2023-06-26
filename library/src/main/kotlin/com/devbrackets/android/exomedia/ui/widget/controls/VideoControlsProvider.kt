package com.devbrackets.android.exomedia.ui.widget.controls

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration

open class VideoControlsProvider {

  /**
   * Provides the [VideoControls] to use with the given [Context].
   *
   * @param context The [Context] to use when constructing the [VideoControls] implementation
   * @return The [VideoControls] implementation to use for the provided [context].
   */
  open fun getControls(context: Context): VideoControls? {
    return if (isDeviceTv(context)) {
      VideoControlsTv(context)
    } else {
      VideoControlsMobile(context)
    }
  }

  protected fun isDeviceTv(context: Context): Boolean {
    val uiManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
    return uiManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
  }
}