package com.devbrackets.android.exomedia.type;

import android.net.Uri;
import android.support.annotation.Nullable;

/**
 * An enum for determining the type of media a particular
 * url is.
 */
public enum MediaSourceType {
    HLS(".*m3u8.*"),
    DASH(".*mpd.*"),
    SMOOTH_STREAM(".*ism.*"),
    DEFAULT(null);

    @Nullable
    private String regex;

    MediaSourceType(@Nullable String regex) {
        this.regex = regex;
    }

    @Nullable
    public String getRegex() {
        return regex;
    }

    public static MediaSourceType get(Uri uri) {
        for (int ordinal = 0; ordinal < values().length; ordinal++) {
            String regex = values()[ordinal].getRegex();
            if (regex != null && uri.toString().matches(regex)) {
                return values()[ordinal];
            }
        }

        return MediaSourceType.DEFAULT;
    }
}
