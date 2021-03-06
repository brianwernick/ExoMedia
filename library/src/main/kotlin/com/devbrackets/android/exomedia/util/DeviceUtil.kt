/*
 * Copyright (C) 2015 - 2019 ExoMedia Contributors
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

package com.devbrackets.android.exomedia.util

import android.content.Context
import android.os.Build

/**
 * A Utility class to help determine characteristics about the device
 */
class DeviceUtil {

  private val NON_COMPATIBLE_DEVICES = listOf(Device("Amazon"))

  fun supportsExoPlayer(context: Context): Boolean {
    return if (!isNotCompatible()) {
      true
    } else context.isAmazonTvOrAmazonWithLollipopSdkOrNewerDevice()
  }

  /**
   * Determines if the current device is not compatible based on the list of devices
   * that don't correctly support the ExoPlayer
   *
   * @return True if the current device is not compatible
   */
  private fun isNotCompatible(): Boolean {
    for (device in NON_COMPATIBLE_DEVICES) {
      if (Build.MANUFACTURER.equals(device.manufacturer, ignoreCase = true)) {
        return device.model?.let {
          Build.DEVICE.equals(it, ignoreCase = true)
        } ?: true
      }
    }
    return false
  }

  private fun Context.isAmazonTvOrAmazonWithLollipopSdkOrNewerDevice() =
      Build.MANUFACTURER.equals("Amazon", ignoreCase = true) &&
          (isDeviceTV() || Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)

  private data class Device(
      val manufacturer: String,
      val model: String? = null
  )
}