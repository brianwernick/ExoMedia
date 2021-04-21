package com.devbrackets.android.exomedia.core.listener

interface VideoSizeListener {
  fun onVideoSizeChanged(width: Int, height: Int, unAppliedRotationDegrees: Int, pixelWidthHeightRatio: Float)
}