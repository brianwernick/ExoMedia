package com.devbrackets.android.exomedia.util

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build

/**
 * Determines if the current device is a TV.
 *
 * @param context The context to use for determining the device information
 * @return True if the current device is a TV
 */
fun Context.isDeviceTV(): Boolean {
    //Since Android TV is only API 21+ that is the only time we will compare configurations
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        val uiManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        uiManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    } else false
}
