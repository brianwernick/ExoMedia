/*
 * Copyright (C) 2016 - 2018 ExoMedia Contributors
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

import android.net.Uri
import java.util.*

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

  return path.substring(periodIndex).toLowerCase(Locale.getDefault())
}
