package com.devbrackets.android.exomediademo.ui.activity

import android.os.Bundle
import android.view.View

import com.devbrackets.android.exomedia.listener.VideoControlsVisibilityListener
import com.devbrackets.android.exomedia.ui.widget.VideoControls
import com.devbrackets.android.exomedia.ui.widget.VideoView
import kotlinx.android.synthetic.main.video_player_activity.*

/**
 * A simple example of making a fullscreen video player activity.
 *
 *
 * ***NOTE:*** the VideoView setup is done in the [VideoPlayerActivity]
 */
class FullScreenVideoPlayerActivity : VideoPlayerActivity() {
    private val fullScreenListener = FullScreenListener()

    /**
     * Determines the appropriate fullscreen flags based on the
     * systems API version.
     *
     * @return The appropriate decor view flags to enter fullscreen mode when supported
     */
    private val fullscreenUiFlags: Int
        get() = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)

    private val stableUiFlags: Int
        get() = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initUiFlags()

        videoView.videoControls?.setVisibilityListener(ControlsVisibilityListener())
    }

    public override fun onDestroy() {
        super.onDestroy()

        // Resets the flags
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    }

    private fun goFullscreen() {
        setUiFlags(true)
    }

    private fun exitFullscreen() {
        setUiFlags(false)
    }

    /**
     * Correctly sets up the fullscreen flags to avoid popping when we switch
     * between fullscreen and not
     */
    private fun initUiFlags() {
        window.decorView.systemUiVisibility = stableUiFlags
        window.decorView.setOnSystemUiVisibilityChangeListener(fullScreenListener)
    }

    /**
     * Applies the correct flags to the windows decor view to enter
     * or exit fullscreen mode
     *
     * @param fullscreen True if entering fullscreen mode
     */
    private fun setUiFlags(fullscreen: Boolean) {
        window.decorView.systemUiVisibility = if (fullscreen) fullscreenUiFlags else stableUiFlags
    }

    /**
     * Listens to the system to determine when to show the default controls
     * for the [VideoView]
     */
    private inner class FullScreenListener : View.OnSystemUiVisibilityChangeListener {
        var lastVisibility = 0
            private set

        override fun onSystemUiVisibilityChange(visibility: Int) {
            // NOTE: if the screen is double tapped in just the right way (or wrong way)
            // the SYSTEM_UI_FLAG_HIDE_NAVIGATION flag is dropped. Because of this we
            // no longer get notified of the temporary change when the screen is tapped
            // (i.e. the VideoControls get the touch event instead of the OS). So we store
            // the visibility off for use in the ControlsVisibilityListener for verification
            lastVisibility = visibility
            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                videoView.showControls()
            }
        }
    }

    /**
     * A Listener for the [VideoControls]
     * so that we can re-enter fullscreen mode when the controls are hidden.
     */
    private inner class ControlsVisibilityListener : VideoControlsVisibilityListener {
        override fun onControlsShown() {
            if (fullScreenListener.lastVisibility != View.SYSTEM_UI_FLAG_VISIBLE) {
                exitFullscreen()
            }
        }

        override fun onControlsHidden() {
            goFullscreen()
        }
    }
}
