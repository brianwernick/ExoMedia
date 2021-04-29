package com.devbrackets.android.exomediademo.ui.activity

import android.os.Bundle
import com.devbrackets.android.exomedia.ui.listener.VideoControlsVisibilityListener
import com.devbrackets.android.exomedia.ui.widget.controls.DefaultVideoControls
import com.devbrackets.android.exomediademo.ui.support.FullscreenManager
import kotlinx.android.synthetic.main.video_player_activity.*

/**
 * A simple example of making a fullscreen video player activity.
 *
 * ***NOTE:*** the VideoView setup is done in the [VideoPlayerActivity]
 */
class FullScreenVideoPlayerActivity : VideoPlayerActivity() {
  private val fullscreenManager by lazy {
    FullscreenManager(window) {
      videoView.showControls()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    fullscreenManager.initUiFlags()

    (videoView.videoControls as? DefaultVideoControls)?.visibilityListener = ControlsVisibilityListener()
  }

  public override fun onDestroy() {
    super.onDestroy()
    fullscreenManager.resetUiFlags()
  }

  /**
   * A Listener for the [DefaultVideoControls]
   * so that we can re-enter fullscreen mode when the controls are hidden.
   */
  private inner class ControlsVisibilityListener : VideoControlsVisibilityListener {
    override fun onControlsShown() {
      fullscreenManager.exitFullscreen()
    }

    override fun onControlsHidden() {
      fullscreenManager.enterFullscreen()
    }
  }
}
