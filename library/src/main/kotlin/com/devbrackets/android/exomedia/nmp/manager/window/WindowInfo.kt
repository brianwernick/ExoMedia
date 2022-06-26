package com.devbrackets.android.exomedia.nmp.manager.window

import androidx.media3.common.Timeline

data class WindowInfo(
  val previousWindowIndex: Int,
  val currentWindowIndex: Int,
  val nextWindowIndex: Int,
  val currentWindow: Timeline.Window
)