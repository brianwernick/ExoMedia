/*
 * Copyright (C) 2016 Brian Wernick
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

/**
 * A Utility class to help with determining information about media
 */
public class MediaUtil {
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
}
