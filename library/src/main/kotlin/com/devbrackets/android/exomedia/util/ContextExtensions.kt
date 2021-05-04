package com.devbrackets.android.exomedia.util

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build

/**
 * Determines if the current device is a TV.
 *
 * @return True if the current device is a TV
 */
fun Context.isDeviceTV(): Boolean {
  val uiManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
  return uiManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
}
