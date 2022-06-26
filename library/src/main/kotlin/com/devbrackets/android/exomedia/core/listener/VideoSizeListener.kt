package com.devbrackets.android.exomedia.core.listener

import androidx.media3.common.VideoSize

interface VideoSizeListener {
  fun onVideoSizeChanged(videoSize: VideoSize)
}