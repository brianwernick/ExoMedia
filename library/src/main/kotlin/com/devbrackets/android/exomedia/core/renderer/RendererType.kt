package com.devbrackets.android.exomedia.core.renderer

import androidx.media3.common.C

enum class RendererType(val exoPlayerTrackType: Int) {
  AUDIO(C.TRACK_TYPE_AUDIO),
  VIDEO(C.TRACK_TYPE_VIDEO),
  CLOSED_CAPTION(C.TRACK_TYPE_TEXT),
  METADATA(C.TRACK_TYPE_METADATA),
  IMAGE(C.TRACK_TYPE_IMAGE),
  CAMERA_MOTION(C.TRACK_TYPE_CAMERA_MOTION)
}