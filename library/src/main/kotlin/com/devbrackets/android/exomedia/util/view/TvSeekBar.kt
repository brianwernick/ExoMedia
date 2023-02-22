package com.devbrackets.android.exomedia.util.view

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.appcompat.widget.AppCompatSeekBar

/**
 * A simple extension of the [AppCompatSeekBar] with some modifications for the
 * [com.devbrackets.android.exomedia.ui.widget.controls.VideoControlsTv] including:
 *  - Disabling the onKey incrementing (handled by the VideoControlsTv)
 */
open class TvSeekBar: AppCompatSeekBar {
  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
    // NOTE:
    // Whenever the seekbar max/min is set it re-calculates the increment so we
    // just "disable" it by always setting it to 0 for key events. This is not
    // ideal, however the main complication we see if this isn't done is a visual
    // artifact that doesn't affect the functionality.
    keyProgressIncrement = 0

    return super.onKeyDown(keyCode, event)
  }
}