package com.devbrackets.android.exomedia.core.video.scale;

import android.support.annotation.NonNull;

/**
 * See {@link android.widget.ImageView.ScaleType} for a description
 * for each type
 */
public enum ScaleType {
    CENTER,
    CENTER_CROP,
    CENTER_INSIDE,
    FIT_CENTER,
    FIT_XY,
    NONE;

    /**
     * Retrieves the {@link ScaleType} with the specified <code>ordinal</code>. If
     * the ordinal is outside the allowed ordinals then {@link #NONE} will be returned
     *
     * @param ordinal The ordinal value for the {@link ScaleType} to retrieve
     * @return The {@link ScaleType} associated with the <code>ordinal</code>
     */
    @NonNull
    public static ScaleType fromOrdinal(int ordinal) {
        if (ordinal < 0 || ordinal > NONE.ordinal()) {
            return ScaleType.NONE;
        }

        return ScaleType.values()[ordinal];
    }
}