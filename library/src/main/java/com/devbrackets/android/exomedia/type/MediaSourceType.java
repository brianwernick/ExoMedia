package com.devbrackets.android.exomedia.type;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public enum MediaSourceType {
    AAC(".aac", null),
    MP4(".mp4", null),
    MP3(".mp3", null),
    M4A(".m4a", null),
    FMP4(".fmp4", null),
    TS(".ts", null),
    WEBM(".webm", null),
    MKV(".mkv", null),
    _3GP(".3gp", null),
    HLS(".m3u8", ".*m3u8.*"),
    DASH(".mpd", ".*mpd.*"),
    SMOOTH_STREAM(".ism", ".*ism.*"),
    UNKNOWN(null, null);

    @Nullable
    private String extension;
    @Nullable
    private String looseComparisonRegex;

    MediaSourceType(@Nullable String extension, @Nullable String looseComparisonRegex) {
        this.extension = extension;
        this.looseComparisonRegex = looseComparisonRegex;
    }

    @Nullable
    public String getExtension() {
        return extension;
    }

    @Nullable
    public String getLooseComparisonRegex() {
        return looseComparisonRegex;
    }

    @NonNull
    public static MediaSourceType getByExtension(@NonNull String extension) {
        for (MediaSourceType type : values()) {
            if (type.getExtension() != null && type.getExtension().equalsIgnoreCase(extension)) {
                return type;
            }
        }

        return UNKNOWN;
    }

    @NonNull
    public static MediaSourceType getByLooseComparison(@NonNull Uri uri) {
        for (MediaSourceType type : values()) {
            if (type.getLooseComparisonRegex() != null && uri.toString().matches(type.getLooseComparisonRegex())) {
                return type;
            }
        }

        return UNKNOWN;
    }
}
