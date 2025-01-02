package com.devbrackets.android.exomediademo.ui.media

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import androidx.appcompat.widget.AppCompatImageButton
import androidx.media3.exoplayer.util.EventLogger
import com.devbrackets.android.exomedia.core.renderer.RendererType
import com.devbrackets.android.exomedia.ui.listener.VideoControlsSeekListener
import com.devbrackets.android.exomedia.ui.listener.VideoControlsVisibilityListener
import com.devbrackets.android.exomedia.ui.widget.controls.DefaultVideoControls
import com.devbrackets.android.exomediademo.App
import com.devbrackets.android.exomediademo.R
import com.devbrackets.android.exomediademo.data.MediaItem
import com.devbrackets.android.exomediademo.data.Samples
import com.devbrackets.android.exomediademo.databinding.VideoPlayerActivityBinding
import com.devbrackets.android.exomediademo.playlist.VideoApi
import com.devbrackets.android.exomediademo.playlist.manager.PlaylistManager
import com.devbrackets.android.exomediademo.ui.support.BindingActivity
import com.devbrackets.android.exomediademo.ui.support.CaptionPopupManager
import com.devbrackets.android.exomediademo.ui.support.CaptionPopupManager.Companion.CC_DEFAULT
import com.devbrackets.android.exomediademo.ui.support.CaptionPopupManager.Companion.CC_DISABLED
import com.devbrackets.android.exomediademo.ui.support.CaptionPopupManager.Companion.CC_GROUP_INDEX_MOD
import com.devbrackets.android.exomediademo.ui.support.FullscreenManager

open class VideoPlayerActivity : BindingActivity<VideoPlayerActivityBinding>(), VideoControlsSeekListener {
  companion object {
    const val EXTRA_INDEX = "EXTRA_INDEX"
    const val PLAYLIST_ID = 6 // Arbitrary, for the example (different from audio)

    fun intent(context: Context, sample: Samples.Sample): Intent {
      // NOTE:
      // We pass the index of the sample for simplicity, however you will likely
      // want to pass an ID for both the selected playlist (audio/video in this demo)
      // and the selected media item
      val index = Samples.video.indexOf(sample)

      return Intent(context, VideoPlayerActivity::class.java).apply {
        putExtra(EXTRA_INDEX, index)
      }
    }
  }

  private lateinit var videoApi: VideoApi
  private lateinit var playlistManager: PlaylistManager
  private lateinit var captionsButton: AppCompatImageButton

  private val selectedIndex by lazy { intent.extras?.getInt(EXTRA_INDEX, 0) ?: 0 }

  private val captionPopupManager = CaptionPopupManager()
  private val fullscreenManager by lazy {
    FullscreenManager(window) {
      (binding.videoView.videoControls as? DefaultVideoControls)?.show()
    }
  }

  override fun inflateBinding(layoutInflater: LayoutInflater): VideoPlayerActivityBinding {
    return VideoPlayerActivityBinding.inflate(layoutInflater)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    init()
  }

  override fun onStop() {
    super.onStop()
    if (videoApi.isPlaying) {
      playlistManager.invokeStop()
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    playlistManager.removeVideoApi(videoApi)
    playlistManager.invokeStop()
  }

  override fun onSeekStarted(): Boolean {
    playlistManager.invokeSeekStarted()
    return true
  }

  override fun onSeekEnded(seekTime: Long): Boolean {
    playlistManager.invokeSeekEnded(seekTime)
    return true
  }

  private fun init() {
    setupPlaylistManager()

    binding.videoView.handleAudioFocus = false
    binding.videoView.setAnalyticsListener(EventLogger())

    setupClosedCaptions()

    videoApi = VideoApi(binding.videoView)
    playlistManager.addVideoApi(videoApi)
    playlistManager.play(0, false)

    (binding.videoView.videoControls as? DefaultVideoControls)?.visibilityListener = ControlsVisibilityListener()
  }

  private fun setupClosedCaptions() {
    captionsButton = AppCompatImageButton(this).apply {
      setBackgroundResource(R.drawable.exomedia_controls_button_background)
      setImageResource(R.drawable.ic_closed_caption_white_24dp)
      setOnClickListener { showCaptionsMenu() }
    }

    (binding.videoView.videoControls as? DefaultVideoControls)?.let {
      it.seekListener = this
      if (binding.videoView.trackSelectionAvailable()) {
        it.addExtraView(captionsButton)
      }
    }

    binding.videoView.setOnVideoSizedChangedListener { intrinsicWidth, intrinsicHeight, pixelWidthHeightRatio ->
      val videoAspectRatio: Float = if (intrinsicWidth == 0 || intrinsicHeight == 0) {
        1f
      } else {
        intrinsicWidth * pixelWidthHeightRatio / intrinsicHeight
      }

      binding.subtitleFrameLayout.setAspectRatio(videoAspectRatio)
    }

    binding.videoView.setCaptionListener(binding.subtitleView)
  }

  /**
   * Retrieves the playlist instance and performs any generation
   * of content if it hasn't already been performed.
   */
  @SuppressLint("Range")
  private fun setupPlaylistManager() {
    playlistManager = (applicationContext as App).playlistManager

    val mediaItems = Samples.video.map {
      MediaItem(it, false)
    }

    playlistManager.setParameters(mediaItems, selectedIndex)
    playlistManager.id = PLAYLIST_ID.toLong()
  }

  private fun showCaptionsMenu() {
    val captionItems = captionPopupManager.getCaptionItems(binding.videoView)
    if (captionItems.isEmpty()) {
      return
    }

    captionPopupManager.showCaptionsMenu(captionItems, captionsButton) {
      onTrackSelected(it)
    }
  }

  private fun onTrackSelected(menuItem: MenuItem): Boolean {
    menuItem.isChecked = true

    when (val itemId = menuItem.itemId) {
      CC_DEFAULT -> binding.videoView.clearSelectedTracks(RendererType.CLOSED_CAPTION)
      CC_DISABLED -> binding.videoView.setRendererEnabled(RendererType.CLOSED_CAPTION, false)
      else -> {
        val trackIndex = itemId % CC_GROUP_INDEX_MOD
        val groupIndex = itemId / CC_GROUP_INDEX_MOD
        binding.videoView.setTrack(RendererType.CLOSED_CAPTION, groupIndex, trackIndex)
      }
    }

    return true
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
