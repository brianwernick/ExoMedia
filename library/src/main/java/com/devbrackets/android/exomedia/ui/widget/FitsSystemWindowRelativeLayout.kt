/*
 * Copyright (C) 2015 - 2018 ExoMedia Contributors
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

import android.annotation.TargetApi
import android.content.Context
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.view.WindowInsets
import android.widget.RelativeLayout

/**
 * A RelativeLayout that will abide by the fitsSystemWindows flag without
 * consuming the event since Android has been designed to only allow
 * one view with fitsSystemWindows=true at a time.
 */
class FitsSystemWindowRelativeLayout : RelativeLayout {
    private val originalPadding: Rect by lazy { Rect(paddingLeft, paddingTop, paddingRight, paddingBottom) }

    constructor(context: Context) : super(context) {
        setup()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        setup()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        setup()
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        setup()
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

    override fun fitSystemWindows(insets: Rect): Boolean {
        updatePadding(insets)
        return false
    }

    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        val windowInsets = Rect(
                insets.systemWindowInsetLeft,
                insets.systemWindowInsetTop,
                insets.systemWindowInsetRight,
                insets.systemWindowInsetBottom
        )

        fitSystemWindows(windowInsets)
        return insets
    }

    /**
     * Updates the views padding so that any children views are correctly shown next to, and
     * below the system bars (NavigationBar and Status/SystemBar) instead of behind them.
     */
    private fun setup() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            fitsSystemWindows = true
        }

        updatePadding(Rect())
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
