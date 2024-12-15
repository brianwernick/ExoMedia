package com.devbrackets.android.exomedia.ui.widget.controls

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.IdRes
import androidx.annotation.IntRange
import androidx.annotation.LayoutRes
import androidx.media3.common.Timeline
import com.devbrackets.android.exomedia.R
import com.devbrackets.android.exomedia.core.state.PlaybackState
import com.devbrackets.android.exomedia.listener.OnTimelineChangedListener
import com.devbrackets.android.exomedia.ui.listener.VideoControlsButtonListener
import com.devbrackets.android.exomedia.ui.listener.VideoControlsSeekListener
import com.devbrackets.android.exomedia.ui.listener.VideoControlsVisibilityListener
import com.devbrackets.android.exomedia.ui.widget.VideoView
import com.devbrackets.android.exomedia.util.Repeater
import com.devbrackets.android.exomedia.util.millisToFormattedDuration
import com.devbrackets.android.exomedia.util.tintListCompat
import java.util.LinkedList
import kotlin.math.abs

/**
 * This is a simple abstraction for the [VideoView] to have a single "View" to add
 * or remove for the Default Video Controls.
 */
@Suppress("MemberVisibilityCanBePrivate")
abstract class DefaultVideoControls : RelativeLayout, VideoControls, OnTimelineChangedListener {
  companion object {
    @JvmStatic
    val DEFAULT_CONTROL_HIDE_DELAY = 2_500L
  }

  protected lateinit var currentTimeTextView: TextView
  protected lateinit var endTimeTextView: TextView
  protected lateinit var timeSeparatorView: View

  protected lateinit var titleTextView: TextView
  protected lateinit var subTitleTextView: TextView
  protected lateinit var playPauseButton: ImageButton
  protected lateinit var previousButton: ImageButton
  protected lateinit var nextButton: ImageButton
  protected lateinit var loadingProgressBar: ProgressBar
  protected lateinit var seekBar: SeekBar

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
  protected val configuration = Configuration()

  /**
   * The delay in milliseconds to wait to start the hide animation
   */
  protected var hideDelay = DEFAULT_CONTROL_HIDE_DELAY

  /**
   * Keeps track of the [LoadState] so that we can filter out duplicates and
   * properly report the [LoadState] when calling [onLoadEnded]
   */
  protected var currentLoadState: LoadState? = null
  protected var currentTimelineStyle: TimelineStyle = TimelineStyle.UNKNOWN
  protected var isVisible = true
  protected var userInteracting = false

  protected var lastUpdatedPosition: Long = 0
  protected var knownDuration: Long? = null

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
   * Performs the control visibility animation for showing or hiding
   * this view
   *
   * @param toVisible True if the view should be visible at the end of the animation
   */
  protected abstract fun animateVisibility(toVisible: Boolean)


  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
  constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

  init {
    setup(context)
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
    videoView.setOnTimelineChangedListener(this)
    this.videoView = videoView
  }

  override fun onDetachedFromView(videoView: VideoView) {
    videoView.removeView(this)
    videoView.setOnTimelineChangedListener(null)
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
      PlaybackState.PREPARING -> reportLoadStarted(state)
      PlaybackState.BUFFERING -> reportLoadStarted(state)
      PlaybackState.SEEKING -> reportLoadStarted(state)
      PlaybackState.READY -> updatePlaybackState(false)
      PlaybackState.PLAYING -> updatePlaybackState(true)
      PlaybackState.PAUSED -> updatePlaybackState(false)
      PlaybackState.COMPLETED -> updatePlaybackState(false)
      PlaybackState.STOPPED -> updatePlaybackState(false)
      PlaybackState.RELEASED -> updatePlaybackState(false)
      PlaybackState.ERROR -> updatePlaybackState(false)
    }
  }

  override fun onTimelineChanged(timeline: Timeline) {
    // An empty timeline can occur when loading or an error occurred
    if (timeline.isEmpty) {
      return onTimelineStyleUpdated(TimelineStyle.UNKNOWN)
    }


    // Check if the end is live/dynamic
    val window = Timeline.Window()
    timeline.getWindow(timeline.windowCount -1, window)
    if (window.isPlaceholder) {
      return onTimelineStyleUpdated(TimelineStyle.UNKNOWN)
    } else if (!window.isLive) {
      return onTimelineStyleUpdated(TimelineStyle.ON_DEMAND)
    }


    // Determine if this is an EVENT or LIVE stream by checking the first window
    timeline.getWindow(0, window)
    if (window.isPlaceholder) {
      return onTimelineStyleUpdated(TimelineStyle.UNKNOWN)
    }

    val rollingStart = window.isDynamic || window.isLive
    val style = when {
      rollingStart -> TimelineStyle.LIVE
      else -> TimelineStyle.EVENT
    }

    onTimelineStyleUpdated(style)
  }

  protected fun onTimelineStyleUpdated(style: TimelineStyle) {
    if (style == currentTimelineStyle) {
      return
    }

    videoView?.let {
      updatePositionText(it.currentPosition, style)
      updateDurationText(it.duration, style)
    }

    currentTimelineStyle = style
  }

  /**
   * Informs the controls that the playback state has changed. This will
   * update to display the correct views, and manage progress polling.
   *
   * @param isPlaying True if the media is currently playing
   */
  fun updatePlaybackState(isPlaying: Boolean) {
    playPauseButton.setImageDrawable(if (isPlaying) pauseDrawable else playDrawable)
    progressPollRepeater.start()

    reportLoadEnded()

    if (isPlaying) {
      hideDelayed()
    } else {
      show()
    }
  }

  /**
   * Reports that the loading has started via [onLoadStarted] using the [playbackState]
   * to determine the correct [LoadState] to report. If [onLoadEnded] has not been notified
   * since the previous call to this method, the call will be treated as a duplicate even if
   * the [playbackState] is different.
   *
   * @param playbackState The [PlaybackState] used to determine the correct [LoadState] to report
   */
  protected open fun reportLoadStarted(playbackState: PlaybackState) {
    val newState = when (playbackState) {
      PlaybackState.PREPARING -> LoadState.PREPARING
      PlaybackState.BUFFERING -> LoadState.BUFFERING
      PlaybackState.SEEKING -> LoadState.SEEKING
      else -> null
    }

    // We can occasionally get a PREPARING after a SEEKING or BUFFERING so we ensure that we can
    // escalate priority (lower value is higher priority) when reporting
    val newPriority = newState?.ordinal ?: 99
    val currentPriority = currentLoadState?.ordinal ?: 100
    if (newPriority >= currentPriority) {
      return
    }

    newState?.let {
      currentLoadState = it
      onLoadStarted(it)
    }
  }

  /**
   * Reports that the loading has ended via [onLoadEnded], using the
   * [currentLoadState] and other local state information to
   * determine the correct [LoadState].
   */
  protected open fun reportLoadEnded() {
    currentLoadState?.let {
      onLoadEnded(it)
    }

    currentLoadState = null
  }

  /**
   * Update the controls to indicate that the video is loading.
   *
   * @param state The [LoadState] representing why loading started
   */
  open fun onLoadStarted(state: LoadState) {
    // Purposefully left blank
  }

  /**
   * Update the controls to indicate that the video is no longer loading
   * which will re-display the play/pause, progress, etc. controls
   *
   * @param state The [LoadState] representing what was just completed
   */
  open fun onLoadEnded(state: LoadState?) {
    // Purposefully left blank
  }

  /**
   * Sets the current video position, updating the seek bar
   * and the current time field
   *
   * @param position The position in milliseconds
   */
  open fun setPosition(@IntRange(from = 0) position: Long) {
    seekBar.progress = position.toInt()
    updatePositionText(position)
  }

  /**
   * Sets the video duration in Milliseconds to display
   * at the end of the progress bar
   *
   * @param duration The duration of the video in milliseconds
   */
  open fun setDuration(@IntRange(from = 0) duration: Long) {
    if (duration != seekBar.max.toLong()) {
      seekBar.max = duration.toInt()
      updateDurationText(duration)
    }
  }

  /***
   * Updates the display of the current timestamp by formatting the [position] in to
   * a human readable (hh:)mm:ss format
   *
   * @param position The position in milliseconds
   */
  protected fun updatePositionText(position: Long) {
    updatePositionText(position, currentTimelineStyle)
  }

  /***
   * Updates the display of the current timestamp by formatting the [position] in to
   * a human readable (hh:)mm:ss format. If the [style] is Live or an Event then
   * the position text will represent that
   *
   * @param position The position in milliseconds
   * @param style The [TimelineStyle] to use for presenting the position text
   */
  protected fun updatePositionText(position: Long, style: TimelineStyle) {
    if (style == currentTimelineStyle && abs(position - lastUpdatedPosition) < 1_000 && lastUpdatedPosition != 0L) {
      return
    }

    lastUpdatedPosition = position

    when (style) {
      TimelineStyle.UNKNOWN,
      TimelineStyle.ON_DEMAND,
      TimelineStyle.EVENT -> {
        currentTimeTextView.text = position.millisToFormattedDuration()
      }
      TimelineStyle.LIVE -> {
        // TODO: for live should we show how delayed we are instead?
        currentTimeTextView.text = position.millisToFormattedDuration()
      }
    }
  }

  /***
   * Updates the display of the duration by formatting the [duration] in to
   * a human readable (hh:)mm:ss format
   *
   * @param duration The duration in milliseconds
   */
  protected fun updateDurationText(duration: Long) {
    updateDurationText(duration, currentTimelineStyle)
  }

  /***
   * Updates the display of the duration by formatting the [duration] in to
   * a human readable (hh:)mm:ss format. If the [style] is Live or an Event
   * then the duration text will represent that the duration is changing
   *
   * @param duration The duration in milliseconds
   * @param style The [TimelineStyle] to use for presenting the duration text
   */
  protected fun updateDurationText(duration: Long, style: TimelineStyle) {
    if (style == currentTimelineStyle && knownDuration == duration) {
      return
    }

    knownDuration = duration

    when (style) {
      TimelineStyle.UNKNOWN,
      TimelineStyle.ON_DEMAND,
      TimelineStyle.EVENT -> {
        endTimeTextView.text = duration.millisToFormattedDuration()
      }
      TimelineStyle.LIVE -> {
        endTimeTextView.text = context.getString(R.string.exomedia_controls_live)
      }
    }
  }

  /**
   * Sets the title to display for the current item in playback
   *
   * @param title The title to display
   */
  fun setTitle(title: CharSequence?) {
    titleTextView.text = title
  }

  /**
   * Sets the subtitle to display for the current item in playback. This will be displayed
   * as the second line of text
   *
   * @param subTitle The sub title to display
   */
  fun setSubTitle(subTitle: CharSequence?) {
    subTitleTextView.text = subTitle
  }

  /**
   * Sets the button state for the Previous button. This method will NOT re-add
   * buttons that have previously been removed with [setPreviousButtonRemoved].
   *
   * @param enabled If the Previous button is enabled [default: false]
   */
  fun setPreviousButtonEnabled(enabled: Boolean) {
    previousButton.isEnabled = enabled
    configuration.setEnabled(R.id.exomedia_controls_previous_btn, enabled)
  }

  /**
   * Sets the button state for the Next button. This method will NOT re-add
   * buttons that have previously been removed with [setNextButtonRemoved].
   *
   * @param enabled If the Next button is enabled [default: false]
   */
  fun setNextButtonEnabled(enabled: Boolean) {
    nextButton.isEnabled = enabled
    configuration.setEnabled(R.id.exomedia_controls_next_btn, enabled)
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
    // Purposefully left blank
  }

  /**
   * Sets the button state for the Fast Forward button. This will just
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
    // Purposefully left blank
  }

  /**
   * Adds or removes the Previous button. This will change the visibility
   * of the button, if you want to change the enabled/disabled images see [setPreviousButtonEnabled]
   *
   * @param removed If the Previous button should be removed [default: true]
   */
  fun setPreviousButtonRemoved(removed: Boolean) {
    previousButton.visibility = if (removed) View.GONE else View.VISIBLE
    configuration.setRemoved(R.id.exomedia_controls_previous_btn, removed)
  }

  /**
   * Adds or removes the Next button. This will change the visibility
   * of the button, if you want to change the enabled/disabled images see [setNextButtonEnabled]
   *
   * @param removed If the Next button should be removed [default: true]
   */
  fun setNextButtonRemoved(removed: Boolean) {
    nextButton.visibility = if (removed) View.GONE else View.VISIBLE
    configuration.setRemoved(R.id.exomedia_controls_next_btn, removed)
  }

  /**
   * Adds or removes the Rewind button. This will change the visibility
   * of the button, if you want to change the enabled/disabled images see [setRewindButtonEnabled]
   *
   * @param removed If the Rewind button should be removed [default: true]
   */
  open fun setRewindButtonRemoved(removed: Boolean) {
    // Purposefully left blank
  }

  /**
   * Adds or removes the FastForward button. This will change the visibility
   * of the button, if you want to change the enabled/disabled images see [setFastForwardButtonEnabled]
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
    if (currentLoadState != null) {
      return
    }

    //Makes sure we don't have a separate hide animation scheduled
    visibilityHandler.removeCallbacksAndMessages(null)
    clearAnimation()

    animateVisibility(false)
  }

  /**
   * Specifies the default delay to use when hiding the controls after user interaction
   * or when playback is resumed. The default value is defined by [DEFAULT_CONTROL_HIDE_DELAY]
   *
   * @param delayMs The default delay to use when hiding the controls in milliseconds
   */
  fun setDefaultHideDelay(delayMs: Long) {
    if (delayMs < 0) {
      return
    }

    hideDelay = delayMs
  }

  /**
   * After the specified delay the view will be hidden. If the user is interacting
   * with the controls then we wait until after they are done to start the delay.
   */
  fun hideDelayed() {
    hideDelayed(hideDelay)
  }

  /**
   * After the specified delay the view will be hidden. If the user is interacting
   * with the controls then we wait until after they are done to start the delay.
   *
   * @param delay The delay in milliseconds to wait to start the hide animation
   */
  open fun hideDelayed(delay: Long) {
    if (delay < 0 || currentLoadState != null) {
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
    updateButtonDrawables(R.color.exomedia_controls_button_foreground)
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
   * Performs the functionality when the PlayPause button is clicked. This
   * includes invoking the callback method if it is enabled, posting the bus
   * event, and toggling the video playback.
   */
  protected fun onPlayPauseClick() {
    if (buttonsListener?.onPlayPauseClicked() != true) {
      internalListener.onPlayPauseClicked()
    }
  }

  /**
   * Performs the functionality to inform any listeners that the play
   * button has been clicked
   */
  protected fun onPlayClick() {
    if (buttonsListener?.onPlayClicked() != true) {
      internalListener.onPlayClicked()
    }
  }

  /**
   * Performs the functionality to inform any listeners that the pause
   * button has been clicked
   */
  protected fun onPauseClick() {
    if (buttonsListener?.onPauseClicked() != true) {
      internalListener.onPauseClicked()
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
    timeSeparatorView = findViewById(R.id.exomedia_controls_time_separator)

    titleTextView = findViewById(R.id.exomedia_controls_title)
    subTitleTextView = findViewById(R.id.exomedia_controls_sub_title)
    playPauseButton = findViewById(R.id.exomedia_controls_play_pause_btn)
    previousButton = findViewById(R.id.exomedia_controls_previous_btn)
    nextButton = findViewById(R.id.exomedia_controls_next_btn)
    loadingProgressBar = findViewById(R.id.exomedia_controls_video_loading)
    seekBar = findViewById(R.id.exomedia_controls_video_seek)
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
   * Performs the progress update on the current time field,
   * and the seek bar
   *
   * @param position The position in milliseconds
   * @param duration The duration of the video in milliseconds
   * @param bufferPercent The integer percent that is buffered [0, 100] inclusive
   */
  protected open fun updateProgress(
    @IntRange(from = 0) position: Long,
    @IntRange(from = 0) duration: Long,
    @IntRange(from = 0, to = 100) bufferPercent: Int
  ) {
    if (!userInteracting) {
      seekBar.secondaryProgress = (seekBar.max * (bufferPercent.toFloat() / 100)).toInt()
      seekBar.progress = position.toInt()

      updatePositionText(position)
    }
  }

  protected class Configuration {
    private val itemConfigs = mutableMapOf<Int, ItemConfig>()

    fun isEnabled(@IdRes id: Int): Boolean {
      return getViewConfiguration(id)?.enabled ?: true
    }

    fun setEnabled(@IdRes id: Int, enabled: Boolean) {
      updateViewConfiguration(id) {
        it.copy(enabled = enabled)
      }
    }

    fun isRemoved(@IdRes id: Int): Boolean {
      return getViewConfiguration(id)?.removed ?: false
    }

    fun setRemoved(@IdRes id: Int, removed: Boolean) {
      updateViewConfiguration(id) {
        it.copy(removed = removed)
      }
    }

    /**
     * Helper function to get the Visibility value for the view with [id]
     */
    fun visibility(@IdRes id: Int): Int {
      return if (isRemoved(id)) View.GONE else View.VISIBLE
    }

    private fun getViewConfiguration(@IdRes id: Int): ItemConfig? {
      return synchronized(this) {
        itemConfigs[id]
      }
    }

    private fun updateViewConfiguration(@IdRes id: Int, action: (ItemConfig) -> ItemConfig?) {
      synchronized(this) {
        when (val newConfig = action(itemConfigs[id] ?: ItemConfig())) {
          null -> itemConfigs.remove(id)
          else -> itemConfigs[id] = newConfig
        }
      }
    }

    private data class ItemConfig(
      val enabled: Boolean = true,
      val removed: Boolean = false
    )
  }

  enum class LoadState {
    /**
     * Occurs when the media content is being prepared for playback. This
     * is specific to each media item specified.
     */
    PREPARING,

    /**
     * Occurs when the media being played needs to buffer before playback can resume.
     * This will typically occur when streaming content over a slow connection.
     */
    BUFFERING,

    /**
     * Occurs when the user performs a seek to a different timestamp in the media.
     */
    SEEKING
  }

  /**
   * Indicates the presentation style to use for the progress bar and current/end
   * position indicators in the UI.
   */
  protected enum class TimelineStyle {
    /**
     * Represents that we haven't received enough information to determine the
     * style for the timeline. This is likely due to the media being loaded/prepared
     * or an error occurring during the preparation/playback.
     */
    UNKNOWN,

    /**
     * The loaded media represents live data, meaning that the playable content is
     * constantly rolling forwards. i.e. the start and end media is constantly changing.
     *
     * An example of a live stream would be a security camera feed (w/backup).
     */
    LIVE,

    /**
     * The loaded media represents an ongoing event; meaning that new content is constantly
     * being appended to the media. i.e. the media at the end is constantly changing (growing)
     * while the media at the start is static.
     *
     * An example of an event would be an ongoing video presentation
     */
    EVENT,

    /**
     * The loaded media represents content that is unchanging.
     *
     * An example of OnDemand media would be a movie or episode
     */
    ON_DEMAND
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

    override fun onPlayClicked(): Boolean {
      return videoView?.let {
        if (!it.isPlaying) {
          it.start()
          true
        } else {
          false
        }
      } ?: false
    }

    override fun onPauseClicked(): Boolean {
      return videoView?.let {
        if (it.isPlaying) {
          it.pause()
          true
        } else {
          false
        }
      } ?: false
    }

    override fun onPreviousClicked(): Boolean {
      // Purposefully left blank
      return false
    }

    override fun onNextClicked(): Boolean {
      // Purposefully left blank
      return false
    }

    override fun onRewindClicked(): Boolean {
      // Purposefully left blank
      return false
    }

    override fun onFastForwardClicked(): Boolean {
      // Purposefully left blank
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