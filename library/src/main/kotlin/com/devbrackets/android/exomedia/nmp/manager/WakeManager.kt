/*
 * Copyright (C) 2015-2021 ExoMedia Contributors,
 * Copyright (C) 2015 The Android Open Source Project
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

package com.devbrackets.android.exomedia.nmp.manager

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.PowerManager
import android.util.Log

class WakeManager(
    private val context: Context
) {
  companion object {
    private const val TAG = "ExoMediaPlayer"
    private const val WAKE_LOCK_TIMEOUT = 1_000L
  }

  private var wakeLock: PowerManager.WakeLock? = null

  private val hasWakeLockPermission by lazy {
    context.packageManager.checkPermission(Manifest.permission.WAKE_LOCK, context.packageName) == PackageManager.PERMISSION_GRANTED
  }

  private val powerManager by lazy {
    context.getSystemService(Context.POWER_SERVICE) as PowerManager
  }

  /**
   * This function has the MediaPlayer access the low-level power manager
   * service to control the device's power usage while playing is occurring.
   * The parameter is a combination of [android.os.PowerManager] wake flags.
   *
   * Use of this method requires [android.Manifest.permission.WAKE_LOCK]
   * permission.
   *
   * By default, no attempt is made to keep the device awake during playback.
   *
   * @param levelAndFlags The wake lock level and any flags to apply, see [PowerManager.newWakeLock]
   */
  fun setWakeLevel(levelAndFlags: Int) {
    var wasHeld = false
    wakeLock?.let { lock ->
      if (lock.isHeld) {
        wasHeld = true
        lock.release()
      }

      wakeLock = null
    }

    // Verifies we have permissions to perform the wakelock
    if (!hasWakeLockPermission) {
      Log.e(TAG, "Unable to acquire WAKE_LOCK due to missing manifest permission \"${Manifest.permission.WAKE_LOCK}\"")
      return
    }

    //Acquires the wakelock
    wakeLock = powerManager.newWakeLock(levelAndFlags or PowerManager.ON_AFTER_RELEASE, this::class.java.name)
    wakeLock?.setReferenceCounted(false)

    stayAwake(wasHeld)
  }

  /**
   * Used with playback state changes to correctly acquire and
   * release the wakelock if the user has enabled it with [setWakeLevel].
   * If the [wakeLock] is null then no action will be performed.
   *
   * @param awake True if the wakelock should be acquired
   */
  fun stayAwake(awake: Boolean) {
    wakeLock?.let { lock ->
      if (awake && !lock.isHeld) {
        lock.acquire(WAKE_LOCK_TIMEOUT)
      } else if (!awake && lock.isHeld) {
        lock.release()
      }
    }
  }
}