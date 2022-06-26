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
