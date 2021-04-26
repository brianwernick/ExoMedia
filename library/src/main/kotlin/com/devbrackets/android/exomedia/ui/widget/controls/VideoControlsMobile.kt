/*
 * Copyright (C) 2015 - 2021 ExoMedia Contributors
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

import android.content.Context
import androidx.annotation.IntRange
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import com.devbrackets.android.exomedia.R
import com.devbrackets.android.exomedia.util.millisToFormattedTimeString
import java.util.*

/**
 * Provides playback controls for the [VideoView] on Mobile
 * (Phone, Tablet, etc.) devices.
 */
class VideoControlsMobile : DefaultVideoControls {
  protected lateinit var seekBar: SeekBar
  protected lateinit var extraViewsContainer: LinearLayout
  protected lateinit var container: ViewGroup

  protected var userInteracting = false

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

  override fun setPosition(@IntRange(from = 0) position: Long) {
    currentTimeTextView.text = position.millisToFormattedTimeString()
    seekBar.progress = position.toInt()
  }

  override fun setDuration(@IntRange(from = 0) duration: Long) {
    if (duration != seekBar.max.toLong()) {
      endTimeTextView.text = duration.millisToFormattedTimeString()
      seekBar.max = duration.toInt()
    }
  }

  override fun updateProgress(@IntRange(from = 0) position: Long, @IntRange(from = 0) duration: Long, @IntRange(from = 0, to = 100) bufferPercent: Int) {
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

  override fun hideDelayed(delay: Long) {
    hideDelay = delay

    if (delay < 0 || !canViewHide || isLoading) {
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
  protected inner class SeekBarChanged : SeekBar.OnSeekBarChangeListener {
    private var seekToTime: Long = 0

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
      if (!fromUser) {
        return
      }

      seekToTime = progress.toLong()
      currentTimeTextView.text = seekToTime.millisToFormattedTimeString()
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
}