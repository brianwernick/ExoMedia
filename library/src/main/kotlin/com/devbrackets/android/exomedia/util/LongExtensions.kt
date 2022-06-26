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
fun Long.millisToFormattedDuration(): String {
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
