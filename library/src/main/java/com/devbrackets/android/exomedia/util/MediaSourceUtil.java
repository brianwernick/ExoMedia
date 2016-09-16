package com.devbrackets.android.exomedia.util;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.devbrackets.android.exomedia.type.MediaSourceType;

/**
 * A utility to handle the checks and comparisons when determining
 * the format for a media item.
 */
public class MediaSourceUtil {

    @NonNull
    public static MediaSourceType getType(@NonNull Uri uri) {
        String extension = getExtension(uri);
        if (extension != null) {
            return MediaSourceType.getByExtension(extension);
        }

        return MediaSourceType.getByLooseComparison(uri);
    }

    @Nullable
    public static String getExtension(@NonNull Uri uri) {
        String path = uri.getLastPathSegment();
        if (path == null) {
            return null;
        }

        int periodIndex = path.lastIndexOf('.');
        if (periodIndex == -1 && uri.getPathSegments().size() > 1) {
            //Checks the second to last segment to handle manifest urls (e.g. "TearsOfSteelTeaser.ism/manifest")
            path = uri.getPathSegments().get(uri.getPathSegments().size() -2);
            periodIndex = path.lastIndexOf('.');
        }

        //If there is no period, prepend one to the last segment in case it is the extension without a period
        if (periodIndex == -1) {
            periodIndex = 0;
            path = "." + uri.getLastPathSegment();
        }

        String rawExtension = path.substring(periodIndex);
        return rawExtension.toLowerCase();
    }
}
