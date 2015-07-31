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
        SMOOTH_STREAM(".ism", "application/vnd.ms-sstr+xml"),
        DASH(".mpd", "application/dash+xml"),
        HLS(".m3u8", "application/x-mpegurl"),
        MP4(".mp4", "video/mp4"),
        FMP4(".fmp4", "video/fmp4"),
        M4A(".m4a", "video/m4a"),
        MP3(".mp3", "audio/mp3"),
        TS(".ts", "video/mp2t"),
        AAC(".aac", "audio/aac"),
        WEBM(".webm", "video/webm"),
        MKV(".mkv", "video/mkv"),
        UNKNOWN("", "");

        private String extension;
        private String mimeType;

        MediaType(String extension, String mimeType) {
            this.extension = extension;
            this.mimeType = mimeType;
        }

        public String getExtension() {
            return extension;
        }

        public String getMimeType() {
            return mimeType;
        }
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

        //Finds the MediaType with the same extension
        for (MediaType type : MediaType.values()) {
            if (type.getExtension().equals(extension)) {
                return type;
            }
        }

        return MediaType.UNKNOWN;
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
