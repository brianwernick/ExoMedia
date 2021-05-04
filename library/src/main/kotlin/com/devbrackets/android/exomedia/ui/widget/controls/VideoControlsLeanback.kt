/*
 * Copyright (C) 2015 - 2019 ExoMedia Contributors
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

package com.devbrackets.android.exomedia.ui.widget.controls

import android.annotation.TargetApi
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.IntRange
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import com.devbrackets.android.exomedia.R
import com.devbrackets.android.exomedia.ui.animation.BottomViewHideShowAnimation
import com.devbrackets.android.exomedia.util.millisToFormattedTimeString
import com.devbrackets.android.exomedia.util.tintListCompat

/**
 * Provides playback controls for the [VideoView] on TV devices.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class VideoControlsLeanback : DefaultVideoControls {

  protected lateinit var controlsContainer: ViewGroup
  protected lateinit var textContainer: ViewGroup
  protected lateinit var progressBar: ProgressBar

  protected lateinit var rippleIndicator: ImageView
  protected lateinit var controlsParent: ViewGroup

  protected var fastForwardButton: ImageButton? = null
  protected var rewindButton: ImageButton? = null

  protected var currentFocus: View? = null
  protected var buttonFocusChangeListener = ButtonFocusChangeListener()

  override val layoutResource: Int
    get() = R.layout.exomedia_default_controls_leanback

  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
  constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

  override fun setup(context: Context) {
    super.setup(context)
    internalListener = LeanbackInternalListener()
    registerForInput()
    isFocusable = true
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    playPauseButton.requestFocus()
    currentFocus = playPauseButton
  }

  override fun retrieveViews() {
    super.retrieveViews()

    controlsContainer = findViewById(R.id.exomedia_controls_interactive_container)
    textContainer = findViewById(R.id.exomedia_controls_text_container)
    progressBar = findViewById(R.id.exomedia_controls_video_progress)
    rippleIndicator = findViewById(R.id.exomedia_controls_leanback_ripple)
    controlsParent = findViewById(R.id.exomedia_controls_parent)
    fastForwardButton = findViewById(R.id.exomedia_controls_fast_forward_btn)
    rewindButton = findViewById(R.id.exomedia_controls_rewind_btn)
  }

  override fun setPosition(position: Long) {
    currentTimeTextView.text = position.millisToFormattedTimeString()
    progressBar.progress = position.toInt()
  }

  override fun setDuration(duration: Long) {
    if (duration != progressBar.max.toLong()) {
      endTimeTextView.text = duration.millisToFormattedTimeString()
      progressBar.max = duration.toInt()
    }
  }

  override fun updateProgress(@IntRange(from = 0) position: Long, @IntRange(from = 0) duration: Long, @IntRange(from = 0, to = 100) bufferPercent: Int) {
    progressBar.secondaryProgress = (progressBar.max * (bufferPercent.toFloat() / 100)).toInt()
    progressBar.progress = position.toInt()

    updateCurrentTime(position)
  }

  override fun setRewindDrawable(drawable: Drawable) {
    rewindButton?.setImageDrawable(drawable)
  }

  override fun setFastForwardDrawable(drawable: Drawable) {
    fastForwardButton?.setImageDrawable(drawable)
  }

  override fun setRewindButtonEnabled(enabled: Boolean) {
    rewindButton?.let {
      it.isEnabled = enabled
      enabledViews.put(R.id.exomedia_controls_rewind_btn, enabled)
    }
  }

  override fun setFastForwardButtonEnabled(enabled: Boolean) {
    fastForwardButton?.let {
      it.isEnabled = enabled
      enabledViews.put(R.id.exomedia_controls_fast_forward_btn, enabled)
    }
  }

  override fun setRewindButtonRemoved(removed: Boolean) {
    rewindButton?.visibility = if (removed) View.GONE else View.VISIBLE
  }

  override fun setFastForwardButtonRemoved(removed: Boolean) {
    fastForwardButton?.visibility = if (removed) View.GONE else View.VISIBLE
  }

  override fun registerListeners() {
    super.registerListeners()
    rewindButton?.setOnClickListener { onRewindClick() }
    fastForwardButton?.setOnClickListener { onFastForwardClick() }

    //Registers the buttons for focus changes in order to update the ripple selector
    previousButton.onFocusChangeListener = buttonFocusChangeListener
    rewindButton?.onFocusChangeListener = buttonFocusChangeListener
    playPauseButton.onFocusChangeListener = buttonFocusChangeListener
    fastForwardButton?.onFocusChangeListener = buttonFocusChangeListener
    nextButton.onFocusChangeListener = buttonFocusChangeListener
  }

  override fun updateButtonDrawables() {
    updateButtonDrawables(R.color.exomedia_default_controls_leanback_button_selector)
  }

  override fun updateButtonDrawables(tintList: Int) {
    super.updateButtonDrawables(tintList)

    val rewindDrawable = context.tintListCompat(R.drawable.exomedia_ic_rewind_white, tintList)
    rewindButton?.setImageDrawable(rewindDrawable)

    val fastForwardDrawable = context.tintListCompat(R.drawable.exomedia_ic_fast_forward_white, tintList)
    fastForwardButton?.setImageDrawable(fastForwardDrawable)
  }

  override fun animateVisibility(toVisible: Boolean) {
    if (isVisible == toVisible) {
      return
    }

    if (!isLoading) {
      controlsParent.startAnimation(BottomViewHideShowAnimation(controlsParent, toVisible, CONTROL_VISIBILITY_ANIMATION_LENGTH))
    }

    isVisible = toVisible
    onVisibilityChanged()
  }

  override fun updateTextContainerVisibility() {
    if (!isVisible) {
      return
    }

    val emptyText = isTextContainerEmpty
    if (hideEmptyTextContainer && emptyText && textContainer.visibility == View.VISIBLE) {
      textContainer.clearAnimation()
      textContainer.startAnimation(BottomViewHideShowAnimation(textContainer, false, CONTROL_VISIBILITY_ANIMATION_LENGTH))
    } else if ((!hideEmptyTextContainer || !emptyText) && textContainer.visibility != View.VISIBLE) {
      textContainer.clearAnimation()
      textContainer.startAnimation(BottomViewHideShowAnimation(textContainer, true, CONTROL_VISIBILITY_ANIMATION_LENGTH))
    }
  }

  override fun showLoading(initialLoad: Boolean) {
    if (isLoading) {
      return
    }

    isLoading = true
    controlsContainer.visibility = View.GONE
    rippleIndicator.visibility = View.GONE
    loadingProgressBar.visibility = View.VISIBLE

    show()
  }

  override fun finishLoading() {
    if (!isLoading) {
      return
    }

    isLoading = false
    controlsContainer.visibility = View.VISIBLE
    rippleIndicator.visibility = View.VISIBLE
    loadingProgressBar.visibility = View.GONE

    updatePlaybackState(videoView != null && videoView!!.isPlaying)
  }

  /**
   * Performs the functionality to rewind the current video by
   * {@value #FAST_FORWARD_REWIND_AMOUNT} milliseconds.
   */
  protected fun onRewindClick() {
    if (buttonsListener == null || !buttonsListener!!.onRewindClicked()) {
      internalListener.onRewindClicked()
    }
  }

  /**
   * Performs the functionality to fast forward the current video by
   * {@value #FAST_FORWARD_REWIND_AMOUNT} milliseconds.
   */
  protected fun onFastForwardClick() {
    if (buttonsListener == null || !buttonsListener!!.onFastForwardClicked()) {
      internalListener.onFastForwardClicked()
    }
  }

  /**
   * Performs the functionality to inform any listeners that the video has been
   * seeked to the specified time.
   *
   * @param seekToTime The time to seek to in milliseconds
   */
  protected fun performSeek(seekToTime: Long) {
    if (seekListener == null || !seekListener!!.onSeekEnded(seekToTime)) {
      show()
      internalListener.onSeekEnded(seekToTime)
    }
  }

  /**
   * Temporarily shows the default controls, hiding after the standard
   * delay.  If the [.videoView] is not playing then the controls
   * will not be hidden.
   */
  protected fun showTemporary() {
    show()
    if (videoView?.isPlaying == true) {
      hideDelayed()
    }
  }

  /**
   * Registers all selectable fields for key events in order
   * to correctly handle navigation.
   */
  protected fun registerForInput() {
    val remoteKeyListener = RemoteKeyListener()
    setOnKeyListener(remoteKeyListener)

    //Registers each button to make sure we catch the key events
    playPauseButton.setOnKeyListener(remoteKeyListener)
    previousButton.setOnKeyListener(remoteKeyListener)
    nextButton.setOnKeyListener(remoteKeyListener)
    rewindButton?.setOnKeyListener(remoteKeyListener)
    fastForwardButton?.setOnKeyListener(remoteKeyListener)
  }

  /**
   * Focuses the next visible view specified in the `view`
   *
   * @param view The view to find the next focus for
   */
  protected fun focusNext(view: View?) {
    view ?: return

    val nextId = view.nextFocusRightId
    if (nextId == View.NO_ID) {
      return
    }

    val nextView = findViewById<View>(nextId)
    if (nextView.visibility != View.VISIBLE) {
      focusNext(nextView)
      return
    }

    nextView.requestFocus()
    currentFocus = nextView
    buttonFocusChangeListener.onFocusChange(nextView, true)
  }

  /**
   * Focuses the previous visible view specified in the `view`
   *
   * @param view The view to find the previous focus for
   */
  protected fun focusPrevious(view: View?) {
    view ?: return

    val previousId = view.nextFocusLeftId
    if (previousId == View.NO_ID) {
      return
    }

    val previousView = findViewById<View>(previousId)
    if (previousView.visibility != View.VISIBLE) {
      focusPrevious(previousView)
      return
    }

    previousView.requestFocus()
    currentFocus = previousView
    buttonFocusChangeListener.onFocusChange(previousView, true)
  }

  /**
   * A repeatListener to monitor the selected button and move the ripple
   * indicator when the focus shifts.
   */
  protected inner class ButtonFocusChangeListener : View.OnFocusChangeListener {
    override fun onFocusChange(view: View, hasFocus: Boolean) {
      if (!hasFocus) {
        return
      }

      //Performs the move animation
      val xDelta = getHorizontalDelta(view)
      rippleIndicator.startAnimation(RippleTranslateAnimation(xDelta))
    }

    protected fun getHorizontalDelta(selectedView: View): Int {
      val position = IntArray(2)
      selectedView.getLocationOnScreen(position)

      val viewX = position[0]
      rippleIndicator.getLocationOnScreen(position)

      val newRippleX = viewX - (rippleIndicator.width - selectedView.width) / 2
      return newRippleX - position[0]
    }
  }

  /**
   * A repeatListener to catch the key events so that we can correctly perform the
   * playback functionality and to hide/show the controls
   */
  protected inner class RemoteKeyListener : View.OnKeyListener {
    /**
     * NOTE: the view is not always the currently focused view, thus the
     * [.currentFocus] variable
     */
    override fun onKey(view: View, keyCode: Int, event: KeyEvent): Boolean {
      if (event.action != KeyEvent.ACTION_DOWN) {
        return false
      }

      when (keyCode) {
          KeyEvent.KEYCODE_BACK -> if (isVisible && canViewHide && !isLoading) {
              hide()
              return true
          } else if (controlsParent.animation != null) {
              //This occurs if we are animating the hide or show of the controls
              return true
          }

          KeyEvent.KEYCODE_DPAD_UP -> {
              showTemporary()
              return true
          }

          KeyEvent.KEYCODE_DPAD_DOWN -> {
              hide()
              return true
          }

          KeyEvent.KEYCODE_DPAD_LEFT -> {
              showTemporary()
              focusPrevious(currentFocus)
              return true
          }

          KeyEvent.KEYCODE_DPAD_RIGHT -> {
              showTemporary()
              focusNext(currentFocus)
              return true
          }

          KeyEvent.KEYCODE_DPAD_CENTER -> {
              showTemporary()
              currentFocus?.callOnClick()
              return true
          }

          KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
              onPlayPauseClick()
              return true
          }

          KeyEvent.KEYCODE_MEDIA_PLAY -> if (videoView != null && !videoView!!.isPlaying) {
              videoView?.start()
              return true
          }

          KeyEvent.KEYCODE_MEDIA_PAUSE -> if (videoView != null && videoView!!.isPlaying) {
              videoView?.pause()
              return true
          }

          KeyEvent.KEYCODE_MEDIA_NEXT -> {
              onNextClick()
              return true
          }

          KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
              onPreviousClick()
              return true
          }

          KeyEvent.KEYCODE_MEDIA_REWIND -> {
              onRewindClick()
              return true
          }

          KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
              onFastForwardClick()
              return true
          }
      }

      return false
    }
  }

  /**
   * An animation for moving the ripple indicator to the correctly
   * focused view.
   */
  protected inner class RippleTranslateAnimation(protected var xDelta: Int) : TranslateAnimation(0f, xDelta.toFloat(), 0f, 0f), Animation.AnimationListener {
    init {
      duration = 250L
      setAnimationListener(this)
    }

    override fun onAnimationStart(animation: Animation) {
      //Purposefully left blank
    }

    override fun onAnimationEnd(animation: Animation) {
      rippleIndicator.x = rippleIndicator.x + xDelta
      rippleIndicator.clearAnimation()
    }

    override fun onAnimationRepeat(animation: Animation) {
      //Purposefully left blank
    }
  }

  protected inner class LeanbackInternalListener : InternalListener() {
    override fun onFastForwardClicked(): Boolean {
      return videoView?.let {
        var newPosition = it.currentPosition + FAST_FORWARD_REWIND_AMOUNT
        if (newPosition > progressBar.max) {
          newPosition = progressBar.max.toLong()
        }

        performSeek(newPosition)
        true
      } ?: false
    }

    override fun onRewindClicked(): Boolean {
      return videoView?.let {
        var newPosition = it.currentPosition - FAST_FORWARD_REWIND_AMOUNT
        if (newPosition < 0) {
          newPosition = 0
        }

        performSeek(newPosition)
        true
      } ?: false
    }
  }

  companion object {
    protected val FAST_FORWARD_REWIND_AMOUNT = 10_000 //10 seconds
  }
}
