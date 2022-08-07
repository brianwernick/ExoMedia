package com.devbrackets.android.exomedia.ui.widget.controls

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.annotation.IntRange
import com.devbrackets.android.exomedia.R
import com.devbrackets.android.exomedia.util.millisToFormattedDuration
import java.util.*

/**
 * Provides playback controls for the [com.devbrackets.android.exomedia.ui.widget.VideoView]
 * on mobile devices (Phone, Tablet, etc.).
 */
class VideoControlsMobile : DefaultVideoControls {
  private lateinit var seekBar: SeekBar
  private lateinit var extraViewsContainer: LinearLayout
  private lateinit var container: ViewGroup

  private var userInteracting = false

  override val layoutResource: Int
    get() = R.layout.exomedia_default_controls_mobile

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
    setOnTouchListener(TouchListener(context))
  }

  override fun setPosition(@IntRange(from = 0) position: Long) {
    currentTimeTextView.text = position.millisToFormattedDuration()
    seekBar.progress = position.toInt()
  }

  override fun setDuration(@IntRange(from = 0) duration: Long) {
    super.setDuration(duration)
    if (duration != seekBar.max.toLong()) {
      endTimeTextView.text = duration.millisToFormattedDuration()
      seekBar.max = duration.toInt()
    }
  }

  override fun updateProgress(
    @IntRange(from = 0) position: Long,
    @IntRange(from = 0) duration: Long,
    @IntRange(from = 0, to = 100) bufferPercent: Int
  ) {
    if (!userInteracting) {
      seekBar.secondaryProgress = (seekBar.max * (bufferPercent.toFloat() / 100)).toInt()
      seekBar.progress = position.toInt()

      updateCurrentTime(position)
    }
  }

  override fun registerListeners() {
    super.registerListeners()
    seekBar.setOnSeekBarChangeListener(SeekBarChanged())
  }

  override fun retrieveViews() {
    super.retrieveViews()
    seekBar = findViewById(R.id.exomedia_controls_video_seek)
    extraViewsContainer = findViewById(R.id.exomedia_controls_extra_container)
    container = findViewById(R.id.exomedia_controls_container)
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
    hideDelay = delay

    if (delay < 0 || isLoading) {
      return
    }

    //If the user is interacting with controls we don't want to start the delayed hide yet
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

  override fun updateTextContainerVisibility() {
    // Purposefully left blank
  }

  override fun showLoading(initialLoad: Boolean) {
    if (isLoading) {
      return
    }

    isLoading = true
    loadingProgressBar.visibility = View.VISIBLE
    playPauseButton.visibility = View.INVISIBLE

    if (!initialLoad) {
      playPauseButton.isEnabled = false
      previousButton.isEnabled = false
      nextButton.isEnabled = false
    }

    show()
  }

  override fun finishLoading() {
    if (!isLoading) {
      return
    }

    isLoading = false
    loadingProgressBar.visibility = View.GONE
    container.visibility = View.VISIBLE

    playPauseButton.isEnabled = true
    playPauseButton.visibility = View.VISIBLE
    previousButton.isEnabled = enabledViews.get(R.id.exomedia_controls_previous_btn, true)
    nextButton.isEnabled = enabledViews.get(R.id.exomedia_controls_next_btn, true)

    updatePlaybackState(videoView?.isPlaying == true)
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
      currentTimeTextView.text = seekToTime.millisToFormattedDuration()
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
   * Monitors the view click events to show and hide the video controls if they have been specified.
   */
  private inner class TouchListener(context: Context) : GestureDetector.SimpleOnGestureListener(), OnTouchListener {
    private val gestureDetector by lazy {
      GestureDetector(context, this)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View, event: MotionEvent): Boolean {
      gestureDetector.onTouchEvent(event)
      return true
    }

    override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
      // Toggles between hiding and showing the controls
      if (isVisible) {
        hide(false)
      } else {
        show()
      }

      return true
    }
  }
}