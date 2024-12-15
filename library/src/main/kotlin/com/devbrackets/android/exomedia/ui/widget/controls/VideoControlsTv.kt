package com.devbrackets.android.exomedia.ui.widget.controls

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.core.view.forEach
import com.devbrackets.android.exomedia.BuildConfig
import com.devbrackets.android.exomedia.R
import com.devbrackets.android.exomedia.util.view.DelegatedOnKeyListener
import com.devbrackets.android.exomedia.util.view.UnhandledMediaKeyLogger
import java.util.LinkedList

/**
 * Provides playback controls for the [com.devbrackets.android.exomedia.ui.widget.VideoView]
 * on TVs.
 */
class VideoControlsTv : DefaultVideoControls {
  companion object {
    private const val FAST_FORWARD_REWIND_AMOUNT = 10_000L // 10 seconds
    private const val TV_CONTROLS_HIDE_DELAY = 5_000L
  }

  private lateinit var extraViewsContainer: LinearLayout
  private lateinit var container: ViewGroup

  private var rewindEnabled: Boolean = true
  private var fastForwardEnabled: Boolean = true

  override val layoutResource: Int
    get() = R.layout.exomedia_controls_tv

  override val extraViews: List<View>
    get() {
      val childCount = extraViewsContainer.childCount
      if (childCount <= 0) {
        return super.extraViews
      }
      val children = LinkedList<View>()
      for (i in 0 until childCount) {
        children.add(extraViewsContainer.getChildAt(i))
      }

      return children
    }

  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
  constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

  override fun setup(context: Context) {
    super.setup(context)

    setDefaultHideDelay(TV_CONTROLS_HIDE_DELAY)
    internalListener = TvInternalListener()
    registerForInput()
  }

  override fun retrieveViews() {
    super.retrieveViews()
    extraViewsContainer = findViewById(R.id.exomedia_controls_extra_container)
    container = findViewById(R.id.exomedia_controls_container)
  }

  override fun setRewindButtonEnabled(enabled: Boolean) {
    rewindEnabled = enabled
  }

  override fun setFastForwardButtonEnabled(enabled: Boolean) {
    fastForwardEnabled = enabled
  }

  override fun setRewindButtonRemoved(removed: Boolean) {
    // No-Op: There's no visible FF/RW UI
  }

  override fun setFastForwardButtonRemoved(removed: Boolean) {
    // No-Op: There's no visible FF/RW UI
  }

  override fun registerListeners() {
    super.registerListeners()
    seekBar.setOnSeekBarChangeListener(SeekBarChanged())
  }

  override fun addExtraView(view: View) {
    extraViewsContainer.addView(view)
  }

  override fun removeExtraView(view: View) {
    extraViewsContainer.removeView(view)
  }

  override fun show() {
    super.show()

    if (videoView?.isPlaying == true) {
      hide(true)
    }
  }

  override fun hideDelayed(delay: Long) {
    if (delay < 0 || currentLoadState != null) {
      return
    }

    // If the user is interacting with controls we don't want to start the delayed hide yet
    if (!userInteracting) {
      visibilityHandler.postDelayed({ animateVisibility(false) }, delay)
    }
  }

  override fun animateVisibility(toVisible: Boolean) {
    if (isVisible == toVisible) {
      return
    }

    val endAlpha = if (toVisible) 1F else 0F
    container.animate().alpha(endAlpha).start()

    isVisible = toVisible
    onVisibilityChanged()
  }

  override fun onLoadStarted(state: LoadState) {
    loadingProgressBar.visibility = View.VISIBLE
    playPauseButton.visibility = View.INVISIBLE

    if (state == LoadState.PREPARING) {
      seekBar.visibility = View.INVISIBLE

      currentTimeTextView.visibility = View.INVISIBLE
      timeSeparatorView.visibility = View.INVISIBLE
      endTimeTextView.visibility = View.INVISIBLE

      previousButton.visibility = View.INVISIBLE
      nextButton.visibility = View.INVISIBLE

      extraViewsContainer.visibility = View.INVISIBLE
    }

    show()
  }

  override fun onLoadEnded(state: LoadState?) {
    currentLoadState = null
    loadingProgressBar.visibility = View.GONE
    seekBar.visibility = View.VISIBLE
    container.visibility = View.VISIBLE

    currentTimeTextView.visibility = View.VISIBLE
    timeSeparatorView.visibility = View.VISIBLE
    endTimeTextView.visibility = View.VISIBLE

    playPauseButton.visibility = View.VISIBLE
    playPauseButton.isEnabled = true

    previousButton.visibility = configuration.visibility(R.id.exomedia_controls_previous_btn)
    previousButton.isEnabled = configuration.isEnabled(R.id.exomedia_controls_previous_btn)

    nextButton.visibility = configuration.visibility(R.id.exomedia_controls_next_btn)
    nextButton.isEnabled = configuration.isEnabled(R.id.exomedia_controls_next_btn)

    extraViewsContainer.visibility = View.VISIBLE

    updatePlaybackState(videoView?.isPlaying == true)

    if (state == LoadState.PREPARING) {
      seekBar.requestFocus()
    }
  }

  /**
   * Performs the functionality to rewind the current video by
   * {@value #FAST_FORWARD_REWIND_AMOUNT} milliseconds.
   */
  private fun onRewindClick() {
    if (rewindEnabled && buttonsListener?.onRewindClicked() != true) {
      internalListener.onRewindClicked()
    }
  }

  /**
   * Performs the functionality to fast forward the current video by
   * {@value #FAST_FORWARD_REWIND_AMOUNT} milliseconds.
   */
  private fun onFastForwardClick() {
    if (fastForwardEnabled && buttonsListener?.onFastForwardClicked() != true) {
      internalListener.onFastForwardClicked()
    }
  }

  /**
   * Performs the functionality to inform any listeners that the video has been
   * seeked to the specified time.
   *
   * @param seekToTime The time to seek to in milliseconds
   */
  private fun performSeek(seekToTime: Long) {
    if (seekListener?.onSeekEnded(seekToTime) != true) {
      show()
      internalListener.onSeekEnded(seekToTime)
    }
  }

  /**
   * Registers all selectable fields for key events in order
   * to correctly handle navigation and media key.
   */
  private fun registerForInput() {
    val delegatedOnKeyListener = DelegatedOnKeyListener().apply {
      register(HiddenKeyListener())
      register(InteractionKeyListener())
      register(SeekKeyListener())
      register(MediaKeyListener())
    }

    val hierarchyListener = OnKeyHierarchyListener(delegatedOnKeyListener)
    hierarchyListener.setForViewGroup(container)
    hierarchyListener.setForViewGroup(extraViewsContainer)

    if (BuildConfig.DEBUG) {
      UnhandledMediaKeyLogger().register(container)
    }
  }

  /**
   * Listens to the seek bar change events and correctly handles the changes
   */
  private inner class SeekBarChanged : SeekBar.OnSeekBarChangeListener {
    private var seekToTime: Long = 0

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
      if (!fromUser) {
        return
      }

      seekToTime = progress.toLong()

      // NOTE: we don't call updateCurrentTime because the SeekBar's logic will only allow 20 steps to increment
      // the position (setOnKeyIncrement()) so the progress reported here is likely different from the expected
      // amount specified by FAST_FORWARD_REWIND_AMOUNT. Because touch interaction on TV interfaces is unlikely
      // this should be safe enough.
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
      userInteracting = true

      if (seekListener?.onSeekStarted() != true) {
        internalListener.onSeekStarted()
      }
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
      userInteracting = false

      if (seekListener?.onSeekEnded(seekToTime) != true) {
        internalListener.onSeekEnded(seekToTime)
      }
    }
  }

  /**
   * A simple [View.OnKeyListener] that ensures that interactions cause the
   * controls to be visible.
   */
  private inner class InteractionKeyListener: OnKeyListener {
    override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
      if (event == null || event.action != KeyEvent.ACTION_UP) {
        return false
      }

      when (keyCode) {
        KeyEvent.KEYCODE_BACK,
        KeyEvent.KEYCODE_B,
        KeyEvent.KEYCODE_NAVIGATE_OUT,
        KeyEvent.KEYCODE_ESCAPE -> {
          if (isVisible && currentLoadState == null) {
            hide()
            return true
          }
        }
        else -> show()
      }

      return false
    }
  }

  /**
   * Monitors key events to handle pausing/playing when the controls are hidden
   */
  private inner class HiddenKeyListener: OnKeyListener {
    override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
      if (isVisible || event == null || event.action != KeyEvent.ACTION_UP) {
        return false
      }

      return when(keyCode) {
        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
          onPlayPauseClick()
          true
        }
        else -> false
      }
    }
  }

  /**
   * Handles performing a fast-forward or rewind when clicking left or right with the [seekBar] focused
   */
  private inner class SeekKeyListener: OnKeyListener {
    override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
      if (v != seekBar || event == null || event.action != KeyEvent.ACTION_UP) {
        return false
      }

      return when (keyCode) {
        // We treat the center click as play/pause as a UX optimization since this is a common pattern
        KeyEvent.KEYCODE_DPAD_CENTER -> {
          onPlayPauseClick()
          true
        }
        KeyEvent.KEYCODE_DPAD_RIGHT -> {
          onFastForwardClick()
          true
        }
        KeyEvent.KEYCODE_DPAD_LEFT -> {
          onRewindClick()
          true
        }
        else -> false
      }
    }
  }

  /**
   * Handles common media keys
   */
  private inner class MediaKeyListener : OnKeyListener {
    override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
      if (event == null || event.action != KeyEvent.ACTION_UP) {
        return false
      }

      when (keyCode) {
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
          onPlayPauseClick()
          return true
        }

        KeyEvent.KEYCODE_MEDIA_PLAY -> {
          onPlayClick()
          return true
        }

        KeyEvent.KEYCODE_MEDIA_PAUSE -> {
          onPauseClick()
          return true
        }

        KeyEvent.KEYCODE_MEDIA_NEXT,
        KeyEvent.KEYCODE_BUTTON_R1 -> {
          onNextClick()
          return true
        }

        KeyEvent.KEYCODE_MEDIA_PREVIOUS,
        KeyEvent.KEYCODE_BUTTON_L1 -> {
          onPreviousClick()
          return true
        }

        KeyEvent.KEYCODE_MEDIA_REWIND,
        KeyEvent.KEYCODE_BUTTON_L2 -> {
          onRewindClick()
          return true
        }

        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
        KeyEvent.KEYCODE_BUTTON_R2 -> {
          onFastForwardClick()
          return true
        }
      }

      return false
    }
  }

  /**
   * An [ViewGroup.OnHierarchyChangeListener] that monitors view additions and removals
   * to register the [onKeyListener] when the new view (child) is focusable.
   *
   * @param onKeyListener The [View.OnKeyListener] to register with the new views (children) when they are focusable
   */
  private class OnKeyHierarchyListener(
    private val onKeyListener: OnKeyListener
  ): OnHierarchyChangeListener {
    override fun onChildViewAdded(parent: View?, child: View?) {
      if (child?.isFocusable == true) {
        child.setOnKeyListener(onKeyListener)
      }
    }

    override fun onChildViewRemoved(parent: View?, child: View?) {
      // No unregister method exists so we No-Op
    }

    /**
     * Handles setting this as the change listener for the [viewGroup]. Additionally
     * this will also iterate through existing children and call the [onChildViewAdded]
     * function for each, insuring that all existing and future children will be
     * properly registered.
     *
     * @param viewGroup The [ViewGroup] to subscribe to hierarchy changes for child [View.OnKeyListener] registration
     */
    fun setForViewGroup(viewGroup: ViewGroup) {
      viewGroup.setOnHierarchyChangeListener(this)

      viewGroup.forEach { child ->
        onChildViewAdded(viewGroup, child)
      }
    }
  }

  private inner class TvInternalListener : InternalListener() {
    override fun onFastForwardClicked(): Boolean {
      return seekBy(FAST_FORWARD_REWIND_AMOUNT)
    }

    override fun onRewindClicked(): Boolean {
      return seekBy(-FAST_FORWARD_REWIND_AMOUNT)
    }

    private fun seekBy(amountMillis: Long): Boolean {
      val view = videoView
      if (view == null || !view.isAttachedToWindow) {
        return false
      }

      val position = (view.currentPosition + amountMillis).coerceIn(0, seekBar.max.toLong())
      performSeek(position)

      return true
    }
  }
}
