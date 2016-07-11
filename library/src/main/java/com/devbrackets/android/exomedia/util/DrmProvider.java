package com.devbrackets.android.exomedia.util;

import android.support.annotation.Nullable;

import com.google.android.exoplayer.drm.MediaDrmCallback;

/**
 * Provides the appropriate DRM callbacks based on incoming information
 */
public class DrmProvider {

    @Nullable
    public MediaDrmCallback getDefaultCallback() {
        return null;
    }

    @Nullable
    public MediaDrmCallback getDashCallback() {
        return getDefaultCallback();
    }

    @Nullable
    public MediaDrmCallback getSmoothStreamCallback() {
        return getDefaultCallback();
    }

    @Nullable
    public MediaDrmCallback getHlsCallback() {
        return getDefaultCallback();
    }
}
