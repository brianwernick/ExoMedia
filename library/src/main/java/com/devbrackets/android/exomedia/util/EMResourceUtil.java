package com.devbrackets.android.exomedia.util;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.AnyRes;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.TypedValue;

/**
 * A Utility for handling the changes in the Android Resources API
 * and the support Drawables to correctly retrieve tinted Drawables
 */
public class EMResourceUtil {
    /**
     * Retrieves the drawable specified with the <code>drawableRes</code> using the
     * <code>colorRes</code> to correctly tint it before returning the Drawable
     * object.
     *
     * @param context The context to use for retrieving the drawable
     * @param drawableRes The resource id for the drawable to retrieve
     * @param colorRes The resource id for the color to use for tinting
     * @return The tinted drawable
     */
    public static Drawable tint(Context context, @DrawableRes int drawableRes, @ColorRes int colorRes) {
        Drawable drawable = getDrawable(context, drawableRes);
        drawable = drawable.mutate();
        return tint(context, drawable, colorRes);
    }

    /**
     * Retrieves the drawable specified with the <code>drawable</code> using the
     * <code>colorRes</code> to correctly tint it before returning the Drawable
     * object.
     *
     * @param context The context to use for retrieving the drawable
     * @param drawable The Drawable to tint
     * @param colorRes The resource id for the color to use for tinting
     * @return The tinted drawable
     */
    public static Drawable tint(Context context, Drawable drawable, @ColorRes int colorRes) {
        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(drawable, getColor(context, colorRes));

        return drawable;
    }

    /**
     * Retrieves the drawable specified with the <code>drawableRes</code> using the
     * <code>tintListRes</code> to correctly tint it before returning the Drawable
     * object.
     *
     * @param context The context to use for retrieving the drawable
     * @param drawableRes The resource id for the drawable to retrieve
     * @param tintListRes The resource id for the ColorStateList to use for tinting
     * @return The tinted drawable
     */
    public static Drawable tintList(Context context, @DrawableRes int drawableRes, @ColorRes int tintListRes) {
        Drawable drawable = getDrawable(context, drawableRes);
        drawable = drawable.mutate();
        return tintList(context, drawable, tintListRes);
    }

    /**
     * Retrieves the drawable specified with the <code>drawable</code> using the
     * <code>tintListRes</code> to correctly tint it before returning the Drawable
     * object.
     *
     * @param context The context to use for retrieving the drawable
     * @param drawable The Drawable to tint
     * @param tintListRes The resource id for the ColorStateList to use for tinting
     * @return The tinted drawable
     */
    public static Drawable tintList(Context context, Drawable drawable, @ColorRes int tintListRes) {
        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTintList(drawable, getColorStateList(context, tintListRes));

        return drawable;
    }

    /**
     * Retrieves the drawable specified with the <code>resourceId</code>.  This
     * is a helper method to deal with the API differences for retrieving drawables
     *
     * @param context The context to use when retrieving the drawable
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

    /**
     * Resolves the reference to an attribute, returning the root resource id.
     *
     * @param context The context to use when determining the root id
     * @param attr The attribute to resolve
     * @return The resource id pointing to the de-referenced attribute
     */
    @AnyRes
    public static int getResolvedResourceId(Context context, @AttrRes int attr) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(attr, typedValue, true);

        if (typedValue.type == TypedValue.TYPE_REFERENCE) {
            return typedValue.data;
        }

        return typedValue.resourceId;
    }

    /**
     * Retrieves the color specified with the <code>colorRes</code>.  This
     * is a helper method to deal with the API differences for retrieving colors.
     *
     * @param context The context to use when retrieving the color
     * @param colorRes The id for the color to retrieve
     * @return The color associated with <code>colorRes</code>
     */
    @ColorInt
    public static int getColor(Context context, @ColorRes int colorRes) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return context.getResources().getColor(colorRes, context.getTheme());
        }

        //noinspection deprecation
        return context.getResources().getColor(colorRes);
    }

    /**
     * Retrieves the ColorStateList specified with the <code>colorRes</code>.  This
     * is a helper method to deal with the API differences for retrieving colors.
     *
     * @param context The context to use when retrieving the color
     * @param colorRes The id for the ColorStateList to retrieve
     * @return The ColorStateList associated with <code>colorRes</code>
     */
    public static ColorStateList getColorStateList(Context context, @ColorRes int colorRes) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return context.getResources().getColorStateList(colorRes, context.getTheme());
        }

        //noinspection deprecation
        return context.getResources().getColorStateList(colorRes);
    }
}
