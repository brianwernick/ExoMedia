/*
 * Copyright (C) 2015 Brian Wernick
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

package com.devbrackets.android.exomedia.util;

import android.net.Uri;
import android.support.annotation.Nullable;

/**
 * A Utility class to help with determining information about media
 */
public class MediaUtil {
    public enum MediaType {
        SMOOTH_STREAM,
        DASH,
        HLS,
        MP4,
        M4A,
        MP3,
        TS,
        AAC,
        WEBM,
        UNKNOWN
    }

    private interface Extensions {
        String AAC = ".aac";
        String M4A = ".m4a";
        String MP4 = ".mp4";
        String MP3 = ".mp3";
        String TS = ".ts";
        String WEBM = ".webm";
    }

    private MediaUtil() {
        //Purposefully left blank
    }

    public static boolean isLocalFile(String url) {
        return !(url == null || url.isEmpty()) && (url.startsWith("file") || url.startsWith("/"));
    }

    /**
     * If the passed uri contains a protocol (e.g. Http, file) then it
     * will be returned without any changes.  If the uri does not contain
     * a protocol then we will assume it is a file
     *
     * @param uri The uri to make sure contains a protocol
     * @return The updated uri
     */
    public static String getUriWithProtocol(String uri) {
        if (uri == null) {
            return "";
        }

        //Determines if we need to add the file protocol
        Uri tempUri = Uri.parse(uri);
        String protocol = tempUri.getScheme();
        if (protocol == null) {
            return "file://" + uri;
        }

        return uri;
    }

    /**
     * Determines the media type based on the mediaUri
     *
     * @param mediaUri The uri for the media to determine the MediaType for
     * @return The resulting MediaType
     */
    public static MediaType getMediaType(String mediaUri) {
        String extension = getExtension(mediaUri);
        if (extension == null) {
            return MediaType.UNKNOWN;
        }

        switch (extension) {
            case Extensions.AAC:
                return MediaType.AAC;

            case Extensions.M4A:
                return MediaType.M4A;

            case Extensions.MP3:
                return MediaType.MP3;

            case Extensions.MP4:
                return MediaType.MP4;

            case Extensions.TS:
                return MediaType.TS;

            case Extensions.WEBM:
                return MediaType.WEBM;

            default:
                return MediaType.UNKNOWN;
        }
    }

    @Nullable
    private static String getExtension(String mediaUri) {
        if (mediaUri == null || mediaUri.trim().isEmpty()) {
            return null;
        }

        int periodIndex = mediaUri.lastIndexOf('.');
        if (periodIndex == -1 || periodIndex >= mediaUri.length()) {
            return null;
        }

        String rawExtension = mediaUri.substring(periodIndex);
        return rawExtension.toLowerCase();
    }
}
