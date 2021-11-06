/*
 * Copyright (C) 2021 ExoMedia Contributors
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

package com.devbrackets.android.exomedia.nmp

import com.devbrackets.android.exomedia.core.listener.CaptionListener
import com.devbrackets.android.exomedia.core.listener.MetadataListener
import com.devbrackets.android.exomedia.core.listener.VideoSizeListener
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.text.Cue
import com.google.android.exoplayer2.video.VideoSize

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

  override fun onCues(cues: List<Cue>) {
    captionListener?.onCues(cues)
  }
}