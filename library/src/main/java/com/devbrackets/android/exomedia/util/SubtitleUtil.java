package com.devbrackets.android.exomedia.util;


import android.net.Uri;
import android.support.annotation.NonNull;
import android.webkit.MimeTypeMap;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.util.MimeTypes;

import java.net.URLEncoder;


/**
 * This utility class provides static utility methods for subtitles.
 */
public class SubtitleUtil {

    private void SubtitleView() {
    }

    /**
     * Get the subtitle MimeTypes value from the provided Uri. If the provided
     * Uri is not recognized as either SRT or VTT, then null is returned.
     *
     * @param uri The subtitle Uri.
     * @return If recognized, the associated MimeTypes value. Else, null.
     */
    public static String getSubtitleMimeType(Uri uri) {
        String extension = MediaUtil.getUriFileExtension(uri.toString());

        if (extension == null) {
            return null;
        }

        switch (extension.toLowerCase()) {
            case "srt":
                return MimeTypes.APPLICATION_SUBRIP;
            case "vtt":
                return MimeTypes.TEXT_VTT;
            default:
                return null;
        }
    }

    /**
     * Create a subtitle MediaSource instance.
     *
     * @param dataSourceFactory The data source factory.
     * @param uri               The Uri of the subtitles.
     * @return A new SingleSampleMediaSource instance for the provided
     * subtitle Uri.
     */
    public static MediaSource createSubtitleMediaSource(
            DataSource.Factory dataSourceFactory,
            Uri uri
    ) {
        return new SingleSampleMediaSource(
                uri,
                dataSourceFactory,
                Format.createTextSampleFormat(
                        null,
                        getSubtitleMimeType(uri),
                        null,
                        Format.NO_VALUE,
                        Format.NO_VALUE,
                        "en",
                        null,
                        0
                ),
                C.TIME_UNSET
        );
    }

    /**
     * Create a new MediaSource instance that combines the provided MediaSource
     * and MediaSource created from the provided caption Uri.
     *
     * @param dataSourceFactory The data source factory.
     * @param mediaSource       The media source.
     * @param captionUri        The related caption Uri.
     * @return The resultant MediaSource instance from combining the two sources.
     */
    public static MediaSource createMergingMediaSource(
            DataSource.Factory dataSourceFactory,
            MediaSource mediaSource,
            Uri captionUri
    ) {
        if (captionUri == null) {
            return mediaSource;
        }

        return new MergingMediaSource(
                mediaSource,
                createSubtitleMediaSource(
                        dataSourceFactory,
                        captionUri
                )
        );
    }


}
