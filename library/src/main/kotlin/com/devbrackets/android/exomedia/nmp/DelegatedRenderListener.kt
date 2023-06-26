package com.devbrackets.android.exomedia.nmp

import androidx.annotation.OptIn
import androidx.media3.common.Metadata
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import com.devbrackets.android.exomedia.core.listener.CaptionListener
import com.devbrackets.android.exomedia.core.listener.MetadataListener
import com.devbrackets.android.exomedia.core.listener.VideoSizeListener

@OptIn(UnstableApi::class)
internal class DelegatedRenderListener: Player.Listener {
  private var captionListener: CaptionListener? = null
  private var metadataListener: MetadataListener? = null
  private var videoSizeListener: VideoSizeListener? = null

  fun setCaptionListener(listener: CaptionListener?) {
    captionListener = listener
  }

  fun setMetadataListener(listener: MetadataListener?) {
    metadataListener = listener
  }

  fun setVideoSizeListener(listener: VideoSizeListener?) {
    videoSizeListener = listener
  }

  override fun onVideoSizeChanged(videoSize: VideoSize) {
    videoSizeListener?.onVideoSizeChanged(videoSize)
  }

  override fun onMetadata(metadata: Metadata) {
    metadataListener?.onMetadata(metadata)
  }

  @Deprecated(
    "Replace with onCues(CueGroup)",
    ReplaceWith(
      expression = "onCues(CueGroup(cues, 0))"
    )
  )
  override fun onCues(cues: List<Cue>) {
    onCues(CueGroup(cues, 0))
  }

  override fun onCues(cueGroup: CueGroup) {
    captionListener?.onCues(cueGroup)
  }
}