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

package com.devbrackets.android.exomedia.util

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.*
import androidx.core.graphics.drawable.DrawableCompat
import androidx.appcompat.widget.AppCompatDrawableManager
import android.util.TypedValue

/**
 * Retrieves the drawable specified with the `drawableRes` using the
 * `colorRes` to correctly tintCompat it before returning the Drawable
 * object.
 *
 * @param drawableRes The resource id for the drawable to retrieve
 * @param colorRes The resource id for the color to use for tinting
 * @return The tinted drawable
 */
fun Context.tintCompat(@DrawableRes drawableRes: Int, @ColorRes colorRes: Int): Drawable {
    val drawable = getDrawableCompat(drawableRes).mutate()
    return tintCompat(drawable, colorRes)
}

/**
 * Retrieves the drawable specified with the `drawable` using the
 * `colorRes` to correctly tintCompat it before returning the Drawable
 * object.
 *
 * @param drawable The Drawable to tintCompat
 * @param colorRes The resource id for the color to use for tinting
 * @return The tinted drawable
 */
fun Context.tintCompat(drawable: Drawable, @ColorRes colorRes: Int): Drawable =
        DrawableCompat
                .wrap(drawable)
                .also { DrawableCompat.setTint(it, getColorCompat(colorRes)) }


/**
 * Retrieves the drawable specified with the `drawableRes` using the
 * `tintListRes` to correctly tintCompat it before returning the Drawable
 * object.
 *
 * @param drawableRes The resource id for the drawable to retrieve
 * @param tintListRes The resource id for the ColorStateList to use for tinting
 * @return The tinted drawable
 */
fun Context.tintListCompat(@DrawableRes drawableRes: Int, @ColorRes tintListRes: Int): Drawable {
    val drawable = getDrawableCompat(drawableRes).mutate()
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
fun Context.tintListCompat(drawable: Drawable, @ColorRes tintListRes: Int): Drawable =
        DrawableCompat
                .wrap(drawable)
                .also { DrawableCompat.setTintList(it, getColorStateListCompat(tintListRes)) }

/**
 * Retrieves the drawable specified with the `resourceId`.  This
 * is a helper method to deal with the API differences for retrieving drawables
 *
 * @param drawableResourceId The id for the drawable to retrieve
 * @return The drawable associated with `resourceId`
 */
fun Context.getDrawableCompat(@DrawableRes drawableResourceId: Int): Drawable {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        resources.getDrawable(drawableResourceId, theme)
    } else {
        AppCompatDrawableManager.get().getDrawable(this, drawableResourceId)
    }
}

/**
 * Resolves the reference to an attribute, returning the root resource id.
 *
 * @param attr The attribute to resolve
 * @return The resource id pointing to the de-referenced attribute
 */
@AnyRes
fun Context.getResolvedResourceId(@AttrRes attr: Int): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute(attr, typedValue, true)

    return when (typedValue.type) {
        TypedValue.TYPE_REFERENCE -> typedValue.data
        else -> typedValue.resourceId
    }
}

/**
 * Retrieves the color specified with the `colorRes`.  This
 * is a helper method to deal with the API differences for retrieving colors.
 *
 * @param colorRes The id for the color to retrieve
 * @return The color associated with `colorRes`
 */
@ColorInt
fun Context.getColorCompat(@ColorRes colorRes: Int): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        resources.getColor(colorRes, theme)
    } else resources.getColor(colorRes)
}

/**
 * Retrieves the ColorStateList specified with the `colorRes`.  This
 * is a helper method to deal with the API differences for retrieving colors.
 *
 * @param colorRes The id for the ColorStateList to retrieve
 * @return The ColorStateList associated with `colorRes`
 */
fun Context.getColorStateListCompat(@ColorRes colorRes: Int): ColorStateList {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        resources.getColorStateList(colorRes, theme)
    } else resources.getColorStateList(colorRes)
}
