package com.devbrackets.android.exomedia.util;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.v4.graphics.drawable.DrawableCompat;

/**
 * A Utility for handling the changes in the Android Resources API
 * and the support Drawables to correctly retrieve tinted Drawables
 */
public class EMResourceUtil {
    public static Drawable tint(Context context, @DrawableRes int drawableRes, @ColorRes int colorRes) {
        Drawable drawable = getDrawable(context, drawableRes);
        return tint(context, drawable, colorRes);
    }

    public static Drawable tint(Context context, Drawable drawable, @ColorRes int colorRes) {
        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(drawable, getColor(context, colorRes));

        return drawable;
    }

    public static Drawable tintList(Context context, @DrawableRes int drawableRes, @ColorRes int tintListRes) {
        Drawable drawable = getDrawable(context, drawableRes);
        return tintList(context, drawable, tintListRes);
    }

    public static Drawable tintList(Context context, Drawable drawable, @ColorRes int tintListRes) {
        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTintList(drawable, getColorStateList(context, tintListRes));

        return drawable;
    }

    /**
     * Retrieves the drawable specified with the <code>resourceId</code>.  This
     * is a helper method to deal with the API differences for retrieving drawables
     *
     * @param drawableResourceId The id for the drawable to retrieve
     * @return The drawable associated with <code>resourceId</code>
     */
    public static Drawable getDrawable(Context context, @DrawableRes int drawableResourceId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return context.getResources().getDrawable(drawableResourceId, context.getTheme());
        }

        //noinspection deprecation
        return context.getResources().getDrawable(drawableResourceId);
    }

    public static int getColor(Context context, @ColorRes int colorRes) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return context.getResources().getColor(colorRes, context.getTheme());
        }

        //noinspection deprecation
        return context.getResources().getColor(colorRes);
    }

    public static ColorStateList getColorStateList(Context context, @ColorRes int colorRes) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return context.getResources().getColorStateList(colorRes, context.getTheme());
        }

        //noinspection deprecation
        return context.getResources().getColorStateList(colorRes);
    }
}
