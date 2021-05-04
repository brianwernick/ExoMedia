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

package com.devbrackets.android.exomedia.ui.animation

import android.content.Context
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.TranslateAnimation

/**
 * An animation used to slide [com.devbrackets.android.exomedia.ui.widget.DefaultVideoControls]
 * in and out from the bottom of the screen when changing visibilities.
 */
class BottomViewHideShowAnimation(private val animationView: View, private val toVisible: Boolean, duration: Long) : AnimationSet(false) {

  init {
    //Creates the Alpha animation for the transition
    val startAlpha = (if (toVisible) 0 else 1).toFloat()
    val endAlpha = (if (toVisible) 1 else 0).toFloat()

    val alphaAnimation = AlphaAnimation(startAlpha, endAlpha)
    alphaAnimation.duration = duration


    //Creates the Translate animation for the transition
    val startY = if (toVisible) getHideShowDelta(animationView) else 0
    val endY = if (toVisible) 0 else getHideShowDelta(animationView)
    val translateAnimation = TranslateAnimation(0f, 0f, startY.toFloat(), endY.toFloat())
    translateAnimation.interpolator = if (toVisible) LinearOutSlowInInterpolator() else FastOutLinearInInterpolator()
    translateAnimation.duration = duration


    //Adds the animations to the set
    addAnimation(alphaAnimation)
    addAnimation(translateAnimation)

    setAnimationListener(Listener())
  }

  private fun getHideShowDelta(view: View): Int {
    val displayMetrics = DisplayMetrics()
    val display = (view.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
    display.getMetrics(displayMetrics)

    val screenHeight = displayMetrics.heightPixels
    return screenHeight - view.top
  }

  private inner class Listener : AnimationListener {

    override fun onAnimationStart(animation: Animation) {
      animationView.visibility = View.VISIBLE
    }

    override fun onAnimationEnd(animation: Animation) {
      animationView.visibility = if (toVisible) View.VISIBLE else View.GONE
    }

    override fun onAnimationRepeat(animation: Animation) {
      //Purposefully left blank
    }
  }
}
