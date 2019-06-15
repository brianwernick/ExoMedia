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
import android.support.annotation.*
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.widget.AppCompatDrawableManager
import android.util.TypedValue

/**
 * A Utility for handling the changes in the Android Resources API
 * and the support Drawables to correctly retrieve tinted Drawables
 *
 * TODO Extension Functions
 */
object ResourceUtil {
    /**
     * Retrieves the drawable specified with the `drawableRes` using the
     * `colorRes` to correctly tint it before returning the Drawable
     * object.
     *
     * @param context The context to use for retrieving the drawable
     * @param drawableRes The resource id for the drawable to retrieve
     * @param colorRes The resource id for the color to use for tinting
     * @return The tinted drawable
     */
    fun tint(context: Context, @DrawableRes drawableRes: Int, @ColorRes colorRes: Int): Drawable {
        val drawable = getDrawable(context, drawableRes).mutate()
        return tint(context, drawable, colorRes)
    }

    /**
     * Retrieves the drawable specified with the `drawable` using the
     * `colorRes` to correctly tint it before returning the Drawable
     * object.
     *
     * @param context The context to use for retrieving the drawable
     * @param drawable The Drawable to tint
     * @param colorRes The resource id for the color to use for tinting
     * @return The tinted drawable
     */
    fun tint(context: Context, drawable: Drawable, @ColorRes colorRes: Int): Drawable {
        return DrawableCompat.wrap(drawable).also {
            DrawableCompat.setTint(it, getColor(context, colorRes))
        }
    }

    /**
     * Retrieves the drawable specified with the `drawableRes` using the
     * `tintListRes` to correctly tint it before returning the Drawable
     * object.
     *
     * @param context The context to use for retrieving the drawable
     * @param drawableRes The resource id for the drawable to retrieve
     * @param tintListRes The resource id for the ColorStateList to use for tinting
     * @return The tinted drawable
     */
    fun tintList(context: Context, @DrawableRes drawableRes: Int, @ColorRes tintListRes: Int): Drawable {
        val drawable = getDrawable(context, drawableRes).mutate()
        return tintList(context, drawable, tintListRes)
    }

    /**
     * Retrieves the drawable specified with the `drawable` using the
     * `tintListRes` to correctly tint it before returning the Drawable
     * object.
     *
     * @param context The context to use for retrieving the drawable
     * @param drawable The Drawable to tint
     * @param tintListRes The resource id for the ColorStateList to use for tinting
     * @return The tinted drawable
     */
    fun tintList(context: Context, drawable: Drawable, @ColorRes tintListRes: Int): Drawable {
        return DrawableCompat.wrap(drawable).also {
            DrawableCompat.setTintList(it, getColorStateList(context, tintListRes))
        }
    }

    /**
     * Retrieves the drawable specified with the `resourceId`.  This
     * is a helper method to deal with the API differences for retrieving drawables
     *
     * @param context The context to use when retrieving the drawable
     * @param drawableResourceId The id for the drawable to retrieve
     * @return The drawable associated with `resourceId`
     */
    fun getDrawable(context: Context, @DrawableRes drawableResourceId: Int): Drawable {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            context.resources.getDrawable(drawableResourceId, context.theme)
        } else AppCompatDrawableManager.get().getDrawable(context, drawableResourceId)

    }

    /**
     * Resolves the reference to an attribute, returning the root resource id.
     *
     * @param context The context to use when determining the root id
     * @param attr The attribute to resolve
     * @return The resource id pointing to the de-referenced attribute
     */
    @AnyRes
    fun getResolvedResourceId(context: Context, @AttrRes attr: Int): Int {
        val typedValue = TypedValue()
        val theme = context.theme
        theme.resolveAttribute(attr, typedValue, true)

        return if (typedValue.type == TypedValue.TYPE_REFERENCE) {
            typedValue.data
        } else typedValue.resourceId
    }

    /**
     * Retrieves the color specified with the `colorRes`.  This
     * is a helper method to deal with the API differences for retrieving colors.
     *
     * @param context The context to use when retrieving the color
     * @param colorRes The id for the color to retrieve
     * @return The color associated with `colorRes`
     */
    @ColorInt
    fun getColor(context: Context, @ColorRes colorRes: Int): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.resources.getColor(colorRes, context.theme)
        } else context.resources.getColor(colorRes)
    }

    /**
     * Retrieves the ColorStateList specified with the `colorRes`.  This
     * is a helper method to deal with the API differences for retrieving colors.
     *
     * @param context The context to use when retrieving the color
     * @param colorRes The id for the ColorStateList to retrieve
     * @return The ColorStateList associated with `colorRes`
     */
    fun getColorStateList(context: Context, @ColorRes colorRes: Int): ColorStateList {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.resources.getColorStateList(colorRes, context.theme)
        } else context.resources.getColorStateList(colorRes)
    }
}
