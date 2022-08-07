package com.devbrackets.android.exomedia.util

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat

/**
 * Retrieves the drawable specified with the `drawableRes` using the
 * `tintListRes` to correctly tintCompat it before returning the Drawable
 * object.
 *
 * @param drawableRes The resource id for the drawable to retrieve
 * @param tintListRes The resource id for the ColorStateList to use for tinting
 * @return The tinted drawable
 */
internal fun Context.tintListCompat(@DrawableRes drawableRes: Int, @ColorRes tintListRes: Int): Drawable {
  val drawable = ResourcesCompat.getDrawable(resources, drawableRes, theme)!!.mutate()
  return tintListCompat(drawable, tintListRes)
}

/**
 * Retrieves the drawable specified with the `drawable` using the
 * `tintListRes` to correctly tintCompat it before returning the Drawable
 * object.
 *
 * @param drawable The Drawable to tintCompat
 * @param tintListRes The resource id for the ColorStateList to use for tinting
 * @return The tinted drawable
 */
internal fun Context.tintListCompat(drawable: Drawable, @ColorRes tintListRes: Int): Drawable =
    DrawableCompat
        .wrap(drawable)
        .also {
          DrawableCompat.setTintList(it, getColorStateListCompat(tintListRes))
        }

/**
 * Retrieves the ColorStateList specified with the `colorRes`.  This
 * is a helper method to deal with the API differences for retrieving colors.
 *
 * @param colorRes The id for the ColorStateList to retrieve
 * @return The ColorStateList associated with `colorRes`
 */
internal fun Context.getColorStateListCompat(@ColorRes colorRes: Int): ColorStateList {
  return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    resources.getColorStateList(colorRes, theme)
  } else {
    AppCompatResources.getColorStateList(this, colorRes)
  }
}
