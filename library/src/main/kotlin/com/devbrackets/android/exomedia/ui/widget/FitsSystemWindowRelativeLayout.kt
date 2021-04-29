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

package com.devbrackets.android.exomedia.ui.widget

import android.content.Context
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.view.WindowInsets
import android.view.WindowInsets.Type.systemBars
import android.widget.RelativeLayout

/**
 * A RelativeLayout that will abide by the fitsSystemWindows flag without
 * consuming the event since Android has been designed to only allow
 * one view with fitsSystemWindows=true at a time.
 */
class FitsSystemWindowRelativeLayout : RelativeLayout {
  private val originalPadding: Rect by lazy { Rect(paddingLeft, paddingTop, paddingRight, paddingBottom) }

  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
  constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

  init {
    setup()
  }

  private fun setup() {
    fitsSystemWindows = true
    updatePadding(Rect())
  }

  /**
   * Makes sure the padding is correct for the orientation
   */
  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)

    //If the system navigation bar can move, then clear out the previous insets before
    // fitSystemWindows(...) or onApplyWindowInsets(...) is called
    //This fixes the issue https://github.com/brianwernick/ExoMedia/issues/33
    if (navBarCanMove()) {
      setup()
    }
  }

  override fun dispatchApplyWindowInsets(insets: WindowInsets?): WindowInsets {
    return super.dispatchApplyWindowInsets(insets).also {
      updatePadding(it.asRect())
    }
  }

  /**
   * Updates the layouts padding by using the original padding and adding
   * the values found in the insets.
   *
   * @param insets The Rect containing the additional insets to use for padding
   */
  private fun updatePadding(insets: Rect) {
    setPadding(
        originalPadding.left + insets.left,
        originalPadding.top + insets.top,
        originalPadding.right + insets.right,
        originalPadding.bottom + insets.bottom
    )
  }

  private fun WindowInsets.asRect(): Rect {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      val insets = getInsetsIgnoringVisibility(systemBars())
      return Rect(
          insets.left,
          insets.top,
          insets.right,
          insets.bottom
      )
    }

    return Rect(
        stableInsetLeft,
        stableInsetTop,
        stableInsetRight,
        stableInsetBottom
    )
  }

  /**
   * Determines if the Navigation controller bar can move.  This will typically only be
   * true for phones.
   *
   * @return True if the system navigation buttons can move sides
   */
  private fun navBarCanMove(): Boolean {
    return this.resources.configuration.smallestScreenWidthDp <= 600
  }
}
