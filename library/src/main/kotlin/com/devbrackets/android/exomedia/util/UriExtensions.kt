package com.devbrackets.android.exomedia.util

import android.net.Uri

/**
 * A utility to handle the checks and comparisons when determining
 * the format for a media item.
 */
fun Uri.getExtension(): String {
  var path: String = lastPathSegment ?: return ""

  var periodIndex = path.lastIndexOf('.')
  if (periodIndex == -1 && pathSegments.size > 1) {
    //Checks the second to last segment to handle manifest urls (e.g. "TearsOfSteelTeaser.ism/manifest")
    path = pathSegments[pathSegments.size - 2]
    periodIndex = path.lastIndexOf('.')
  }

  //If there is no period, prepend one to the last segment in case it is the extension without a period
  if (periodIndex == -1) {
    periodIndex = 0
    path = ".$lastPathSegment"
  }

  return path.substring(periodIndex).lowercase()
}
