package com.devbrackets.android.exomedia.ui.widget.controls

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration

open class VideoControlsProvider {

  open fun getControls(context: Context): VideoControls? {
    return if (isDeviceTv(context)) {
      VideoControlsLeanback(context)
    } else {
      VideoControlsMobile(context)
    }
  }

  protected fun isDeviceTv(context: Context): Boolean {
    val uiManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
    return uiManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
  }
}