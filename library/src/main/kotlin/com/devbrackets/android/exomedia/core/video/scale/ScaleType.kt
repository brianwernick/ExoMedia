/*
 * Copyright (C) 2016 - 2019 ExoMedia Contributors
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

package com.devbrackets.android.exomedia.core.video.scale

/**
 * See [android.widget.ImageView.ScaleType] for a description
 * for each type
 */
enum class ScaleType {
  CENTER,
  CENTER_CROP,
  CENTER_INSIDE,
  FIT_CENTER,
  FIT_XY,
  NONE;


  companion object {
    /**
     * Retrieves the [ScaleType] with the specified `ordinal`. If
     * the ordinal is outside the allowed ordinals then [.NONE] will be returned
     *
     * @param ordinal The ordinal value for the [ScaleType] to retrieve
     * @return The [ScaleType] associated with the `ordinal`
     */
    fun fromOrdinal(ordinal: Int): ScaleType {
      return values().getOrElse(ordinal) { NONE }
    }
  }
}