package com.devbrackets.android.exomedia.ui.widget.controls

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.SparseBooleanArray
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.IntRange
import androidx.annotation.LayoutRes
import com.devbrackets.android.exomedia.R
import com.devbrackets.android.exomedia.core.state.PlaybackState
import com.devbrackets.android.exomedia.ui.listener.VideoControlsButtonListener
import com.devbrackets.android.exomedia.ui.listener.VideoControlsSeekListener
import com.devbrackets.android.exomedia.ui.listener.VideoControlsVisibilityListener
import com.devbrackets.android.exomedia.ui.widget.VideoView
import com.devbrackets.android.exomedia.util.Repeater
import com.devbrackets.android.exomedia.util.millisToFormattedDuration
import com.devbrackets.android.exomedia.util.tintListCompat
import java.util.*
import kotlin.math.abs

/**
 * This is a simple abstraction for the [VideoView] to have a single "View" to add
 * or remove for the Default Video Controls.
 */
abstract class DefaultVideoControls : RelativeLayout, VideoControls {
  companion object {
    @JvmStatic
    val DEFAULT_CONTROL_HIDE_DELAY = 2_000L

    @JvmStatic
    protected val CONTROL_VISIBILITY_ANIMATION_LENGTH: Long = 300
  }

  protected lateinit var currentTimeTextView: TextView
  protected lateinit var endTimeTextView: TextView
  protected lateinit var titleTextView: TextView
  protected lateinit var subTitleTextView: TextView
  protected lateinit var playPauseButton: ImageButton
  protected lateinit var previousButton: ImageButton
  protected lateinit var nextButton: ImageButton
  protected lateinit var loadingProgressBar: ProgressBar

  protected lateinit var playDrawable: Drawable
  protected lateinit var pauseDrawable: Drawable

  protected var visibilityHandler = Handler(Looper.getMainLooper())
  protected var progressPollRepeater = Repeater {
    updateProgress()
  }

  protected var videoView: VideoView? = null

  var seekListener: VideoControlsSeekListener? = null
  var buttonsListener: VideoControlsButtonListener? = null
  var visibilityListener: VideoControlsVisibilityListener? = null

  protected var internalListener = InternalListener()

  protected var enabledViews = SparseBooleanArray()

  /**
   * The delay in milliseconds to wait to start the hide animation
   */
  protected var hideDelay = DEFAULT_CONTROL_HIDE_DELAY

  protected var isLoading = false
  protected var isVisible = true

  /**
   * `true` If the empty text blocks can be hidden [default: true]
   */
  protected var hideEmptyTextContainer = true
    set(value) {
      field = value
      updateTextContainerVisibility()
    }

  private var lastUpdatedPosition: Long = 0
  private var knownDuration: Long? = null

  /**
   * Used to retrieve the layout resource identifier to inflate
   *
   * @return The layout resource identifier to inflate
   */
  @get:LayoutRes
  protected abstract val layoutResource: Int

  open val extraViews: List<View>
    get() = LinkedList()

  /**
   * Determines if the `textContainer` doesn't have any text associated with it
   *
   * @return True if there is no text contained in the views in the `textContainer`
   */
  protected val isTextContainerEmpty: Boolean
    get() {
      if (!titleTextView.text.isNullOrEmpty()) {
        return false
      }

      return subTitleTextView.text.isNullOrEmpty()
    }

  /**
   * Sets the current video position, updating the seek bar
   * and the current time field
   *
   * @param position The position in milliseconds
   */
  abstract fun setPosition(@IntRange(from = 0) position: Long)

  /**
   * Performs the progress update on the current time field,
   * and the seek bar
   *
   * @param position The position in milliseconds
   * @param duration The duration of the video in milliseconds
   * @param bufferPercent The integer percent that is buffered [0, 100] inclusive
   */
  abstract fun updateProgress(
    @IntRange(from = 0) position: Long,
    @IntRange(from = 0) duration: Long,
    @IntRange(from = 0, to = 100) bufferPercent: Int
  )

  /**
   * Performs the control visibility animation for showing or hiding
   * this view
   *
   * @param toVisible True if the view should be visible at the end of the animation
   */
  protected abstract fun animateVisibility(toVisible: Boolean)

  /**
   * Update the current visibility of the text block independent of
   * the controls visibility
   */
  protected abstract fun updateTextContainerVisibility()

  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
  constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

  init {
    setup(context)
  }

  /***
   * Updates the current timestamp
   *
   * @param position The position in milliseconds
   */
  protected fun updateCurrentTime(position: Long) {
    // optimization :
    // update the timestamp text per second regarding the 'reset' or 'seek' operations.
    if (abs(position - lastUpdatedPosition) >= 1_000 || lastUpdatedPosition == 0L) {
      lastUpdatedPosition = position

      currentTimeTextView.text = position.millisToFormattedDuration()
    }
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()

    if (videoView?.isPlaying == true) {
      updatePlaybackState(true)
    }
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()

    progressPollRepeater.stop()
  }

  override fun onAttachedToView(videoView: VideoView) {
    videoView.addView(this)
    this.videoView = videoView
  }

  override fun onDetachedFromView(videoView: VideoView) {
    videoView.removeView(this)
    this.videoView = null
  }

  /**
   * Specifies the callback to inform of button click events
   *
   * @param callback The callback
   */
  fun setButtonListener(callback: VideoControlsButtonListener?) {
    this.buttonsListener = callback
  }

  override fun onPlaybackStateChange(state: PlaybackState) {
    when (state) {
      PlaybackState.IDLE -> {}
      PlaybackState.PREPARING -> showLoading(true)
      PlaybackState.BUFFERING -> showLoading(false)
      PlaybackState.SEEKING -> showLoading(false)
      PlaybackState.READY -> finishLoading()
      PlaybackState.PLAYING -> updatePlaybackState(true)
      PlaybackState.PAUSED -> updatePlaybackState(false)
      PlaybackState.COMPLETED -> updatePlaybackState(false)
      PlaybackState.STOPPED -> updatePlaybackState(false)
      PlaybackState.RELEASED -> updatePlaybackState(false)
      PlaybackState.ERROR -> updatePlaybackState(false)
    }
  }

  /**
   * Informs the controls that the playback state has changed.  This will
   * update to display the correct views, and manage progress polling.
   *
   * @param isPlaying True if the media is currently playing
   */
  fun updatePlaybackState(isPlaying: Boolean) {
    playPauseButton.setImageDrawable(if (isPlaying) pauseDrawable else playDrawable)

    progressPollRepeater.start()
    finishLoading()

    if (isPlaying) {
      hideDelayed()
    } else {
      show()
    }
  }

  /**
   * Update the controls to indicate that the video
   * is loading.
   *
   * @param initialLoad `true` if the loading is the initial state, not for seeking or buffering
   */
  open fun showLoading(initialLoad: Boolean) {
    if (isLoading) {
      return
    }

    isLoading = true
  }

  /**
   * Update the controls to indicate that the video is no longer loading
   * which will re-display the play/pause, progress, etc. controls
   */
  open fun finishLoading() {
    if (!isLoading) {
      return
    }

    isLoading = false
  }

  /**
   * Sets the video duration in Milliseconds to display
   * at the end of the progress bar
   *
   * @param duration The duration of the video in milliseconds
   */
  open fun setDuration(@IntRange(from = 0) duration: Long) {
    knownDuration = duration
  }

  /**
   * Sets the title to display for the current item in playback
   *
   * @param title The title to display
   */
  fun setTitle(title: CharSequence?) {
    titleTextView.text = title
    updateTextContainerVisibility()
  }

  /**
   * Sets the subtitle to display for the current item in playback.  This will be displayed
   * as the second line of text
   *
   * @param subTitle The sub title to display
   */
  fun setSubTitle(subTitle: CharSequence?) {
    subTitleTextView.text = subTitle
    updateTextContainerVisibility()
  }

  /**
   * Sets the button state for the Previous button.  This will just
   * change the images specified with [.setPreviousDrawable],
   * or use the defaults if they haven't been set, and block any click events.
   *
   *
   * This method will NOT re-add buttons that have previously been removed with
   * [.setNextButtonRemoved].
   *
   * @param enabled If the Previous button is enabled [default: false]
   */
  fun setPreviousButtonEnabled(enabled: Boolean) {
    previousButton.isEnabled = enabled
    enabledViews.put(R.id.exomedia_controls_previous_btn, enabled)
  }

  /**
   * Sets the button state for the Next button.  This will just
   * change the images specified with [.setNextDrawable],
   * or use the defaults if they haven't been set, and block any click events.
   *
   * This method will NOT re-add buttons that have previously been removed with
   * [.setPreviousButtonRemoved].
   *
   * @param enabled If the Next button is enabled [default: false]
   */
  fun setNextButtonEnabled(enabled: Boolean) {
    nextButton.isEnabled = enabled
    enabledViews.put(R.id.exomedia_controls_next_btn, enabled)
  }

  /**
   * Sets the button state for the Rewind button.  This will just
   * change the images specified with [.setRewindDrawable],
   * or use the defaults if they haven't been set
   *
   *
   * This method will NOT re-add buttons that have previously been removed with
   * [.setRewindButtonRemoved].
   *
   * @param enabled If the Rewind button is enabled [default: false]
   */
  open fun setRewindButtonEnabled(enabled: Boolean) {
    //Purposefully left blank
  }

  /**
   * Sets the button state for the Fast Forward button.  This will just
   * change the images specified with [.setFastForwardDrawable],
   * or use the defaults if they haven't been set
   *
   *
   * This method will NOT re-add buttons that have previously been removed with
   * [.setFastForwardButtonRemoved].
   *
   * @param enabled If the Rewind button is enabled [default: false]
   */
  open fun setFastForwardButtonEnabled(enabled: Boolean) {
    //Purposefully left blank
  }

  /**
   * Adds or removes the Previous button.  This will change the visibility
   * of the button, if you want to change the enabled/disabled images see [.setPreviousButtonEnabled]
   *
   * @param removed If the Previous button should be removed [default: true]
   */
  fun setPreviousButtonRemoved(removed: Boolean) {
    previousButton.visibility = if (removed) View.GONE else View.VISIBLE
  }

  /**
   * Adds or removes the Next button.  This will change the visibility
   * of the button, if you want to change the enabled/disabled images see [.setNextButtonEnabled]
   *
   * @param removed If the Next button should be removed [default: true]
   */
  fun setNextButtonRemoved(removed: Boolean) {
    nextButton.visibility = if (removed) View.GONE else View.VISIBLE
  }

  /**
   * Adds or removes the Rewind button.  This will change the visibility
   * of the button, if you want to change the enabled/disabled images see [.setRewindButtonEnabled]
   *
   * @param removed If the Rewind button should be removed [default: true]
   */
  open fun setRewindButtonRemoved(removed: Boolean) {
    // Purposefully left blank
  }

  /**
   * Adds or removes the FastForward button.  This will change the visibility
   * of the button, if you want to change the enabled/disabled images see [.setFastForwardButtonEnabled]
   *
   * @param removed If the FastForward button should be removed [default: true]
   */
  open fun setFastForwardButtonRemoved(removed: Boolean) {
    // Purposefully left blank
  }

  open fun addExtraView(view: View) {
    // Purposefully left blank
  }

  open fun removeExtraView(view: View) {
    // Purposefully left blank
  }

  /**
   * Immediately starts the animation to show the controls
   */
  open fun show() {
    // Makes sure we don't have a hide animation scheduled
    visibilityHandler.removeCallbacksAndMessages(null)
    clearAnimation()

    animateVisibility(true)
  }

  protected fun hide(delayed: Boolean) {
    if (delayed) {
      hideDelayed()
    } else {
      hide()
    }
  }

  /**
   * Immediately starts the animation to hide the controls
   */
  fun hide() {
    if (isLoading) {
      return
    }

    //Makes sure we don't have a separate hide animation scheduled
    visibilityHandler.removeCallbacksAndMessages(null)
    clearAnimation()

    animateVisibility(false)
  }

  /**
   * After the specified delay the view will be hidden.  If the user is interacting
   * with the controls then we wait until after they are done to start the delay.
   */
  fun hideDelayed() {
    hideDelayed(hideDelay)
  }

  /**
   * After the specified delay the view will be hidden.  If the user is interacting
   * with the controls then we wait until after they are done to start the delay.
   *
   * @param delay The delay in milliseconds to wait to start the hide animation
   */
  open fun hideDelayed(delay: Long) {
    hideDelay = delay

    if (delay < 0 || isLoading) {
      return
    }

    visibilityHandler.postDelayed({ hide() }, delay)
  }

  /**
   * Registers any internal listeners to perform the playback controls,
   * such as play/pause, next, and previous
   */
  protected open fun registerListeners() {
    playPauseButton.setOnClickListener { onPlayPauseClick() }
    previousButton.setOnClickListener { onPreviousClick() }
    nextButton.setOnClickListener { onNextClick() }
  }

  /**
   * Updates the drawables used for the buttons to AppCompatTintDrawables
   */
  protected open fun updateButtonDrawables() {
    updateButtonDrawables(R.color.exomedia_default_controls_button_selector)
  }

  protected open fun updateButtonDrawables(@ColorRes tintList: Int) {
    playDrawable = context.tintListCompat(R.drawable.exomedia_ic_play_arrow_white, tintList)
    pauseDrawable = context.tintListCompat(R.drawable.exomedia_ic_pause_white, tintList)
    playPauseButton.setImageDrawable(playDrawable)

    val previousDrawable = context.tintListCompat(R.drawable.exomedia_ic_skip_previous_white, tintList)
    previousButton.setImageDrawable(previousDrawable)

    val nextDrawable = context.tintListCompat(R.drawable.exomedia_ic_skip_next_white, tintList)
    nextButton.setImageDrawable(nextDrawable)
  }

  /**
   * Performs the functionality when the PlayPause button is clicked.  This
   * includes invoking the callback method if it is enabled, posting the bus
   * event, and toggling the video playback.
   */
  protected fun onPlayPauseClick() {
    if (buttonsListener?.onPlayPauseClicked() != true) {
      internalListener.onPlayPauseClicked()
    }
  }

  /**
   * Performs the functionality to inform any listeners that the previous
   * button has been clicked
   */
  protected fun onPreviousClick() {
    if (buttonsListener?.onPreviousClicked() != true) {
      internalListener.onPreviousClicked()
    }
  }

  /**
   * Performs the functionality to inform any listeners that the next
   * button has been clicked
   */
  protected fun onNextClick() {
    if (buttonsListener?.onNextClicked() != true) {
      internalListener.onNextClicked()
    }
  }

  /**
   * Performs any initialization steps such as retrieving views, registering listeners,
   * and updating any drawables.
   *
   * @param context The context to use for retrieving the correct layout
   */
  protected open fun setup(context: Context) {
    View.inflate(context, layoutResource, this)
    retrieveViews()

    registerListeners()
    updateButtonDrawables()
  }

  /**
   * Retrieves the view references from the xml layout
   */
  protected open fun retrieveViews() {
    currentTimeTextView = findViewById(R.id.exomedia_controls_current_time)
    endTimeTextView = findViewById(R.id.exomedia_controls_end_time)
    titleTextView = findViewById(R.id.exomedia_controls_title)
    subTitleTextView = findViewById(R.id.exomedia_controls_sub_title)
    playPauseButton = findViewById(R.id.exomedia_controls_play_pause_btn)
    previousButton = findViewById(R.id.exomedia_controls_previous_btn)
    nextButton = findViewById(R.id.exomedia_controls_next_btn)
    loadingProgressBar = findViewById(R.id.exomedia_controls_video_loading)
  }

  /**
   * Performs the functionality to inform the callback
   * that the DefaultControls visibility has changed
   */
  protected fun onVisibilityChanged() {
    if (isVisible) {
      visibilityListener?.onControlsShown()
    } else {
      visibilityListener?.onControlsHidden()
    }
  }

  /**
   * Called by the [progressPollRepeater] to update the progress
   * bar using the [videoView] to retrieve the correct information
   */
  protected fun updateProgress() {
    videoView?.let {
      if (knownDuration == null || it.duration != knownDuration) {
        setDuration(it.duration)
      }

      updateProgress(it.currentPosition, it.duration, it.bufferPercentage)
    }
  }

  /**
   * An internal class used to handle the default functionality for the
   * VideoControls
   */
  protected open inner class InternalListener : VideoControlsSeekListener, VideoControlsButtonListener {
    private var pausedForSeek = false

    override fun onPlayPauseClicked(): Boolean {
      return videoView?.let {
        if (it.isPlaying) {
          it.pause()
        } else {
          it.start()
        }
        true
      } ?: false
    }

    override fun onPreviousClicked(): Boolean {
      //Purposefully left blank
      return false
    }

    override fun onNextClicked(): Boolean {
      //Purposefully left blank
      return false
    }

    override fun onRewindClicked(): Boolean {
      //Purposefully left blank
      return false
    }

    override fun onFastForwardClicked(): Boolean {
      //Purposefully left blank
      return false
    }

    override fun onSeekStarted(): Boolean {
      if (videoView == null) {
        return false
      }

      if (videoView?.isPlaying == true) {
        pausedForSeek = true
        videoView?.pause(true)
      }

      show()
      return true
    }

    override fun onSeekEnded(seekTime: Long): Boolean {
      if (videoView == null) {
        return false
      }

      videoView?.seekTo(seekTime)

      if (pausedForSeek) {
        pausedForSeek = false
        videoView?.start()
        hideDelayed()
      }

      return true
    }
  }
}