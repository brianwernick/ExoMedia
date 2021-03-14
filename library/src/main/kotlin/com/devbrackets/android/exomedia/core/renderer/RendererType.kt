/*
 * Copyright (C) 2017 - 2021 ExoMedia Contributors
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

package com.devbrackets.android.exomedia.core.renderer

import com.google.android.exoplayer2.C

enum class RendererType(val exoPlayerTrackType: Int) {
  AUDIO(C.TRACK_TYPE_AUDIO),
  VIDEO(C.TRACK_TYPE_VIDEO),
  CLOSED_CAPTION(C.TRACK_TYPE_TEXT),
  METADATA(C.TRACK_TYPE_METADATA)
}