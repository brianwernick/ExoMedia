package com.devbrackets.android.exomedia.core.video;

/**
 * Represents a protocol that the object can call clear.  This
 * is used to reference both the {@link com.devbrackets.android.exomedia.core.video.ResizingSurfaceView}
 * and {@link com.devbrackets.android.exomedia.core.video.ResizingTextureView} which can both
 * have their surfaces cleared.
 */
public interface ClearableSurface {
    void clearSurface();
}
