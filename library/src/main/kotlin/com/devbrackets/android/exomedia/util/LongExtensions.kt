/*
 * Copyright (C) 2015 -  2018 ExoMedia Contributors
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

import android.text.format.DateUtils

/**
 * Formats the specified milliseconds to a human readable format
 * in the form of (Hours : Minutes : Seconds).  If the specified
 * milliseconds is less than 0 the resulting format will be
 * "--:--" to represent an unknown time
 *
 * @return The human readable time
 */
fun Long.millisToFormattedTimeString(): String {
  if (this < 0) {
    return "--:--"
  }
  val seconds = this % DateUtils.MINUTE_IN_MILLIS / DateUtils.SECOND_IN_MILLIS
  val minutes = this % DateUtils.HOUR_IN_MILLIS / DateUtils.MINUTE_IN_MILLIS
  val hours = this % DateUtils.DAY_IN_MILLIS / DateUtils.HOUR_IN_MILLIS

  return if (hours > 0) {
    "%d:%02d:%02d".format(hours, minutes, seconds)
  } else "%02d:%02d".format(minutes, seconds)

}
