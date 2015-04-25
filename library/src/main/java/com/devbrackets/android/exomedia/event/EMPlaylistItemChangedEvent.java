package com.devbrackets.android.exomedia.event;

import com.devbrackets.android.exomedia.manager.EMPlaylistManager;

public class EMPlaylistItemChangedEvent<T extends EMPlaylistManager.PlaylistItem> {
    public final T currentItem;
    public final EMPlaylistManager.MediaType mediaType;
    public final boolean hasNext;
    public final boolean hasPrevious;

    public EMPlaylistItemChangedEvent(T currentItem, EMPlaylistManager.MediaType mediaType, boolean hasPrevious, boolean hasNext) {
        this.currentItem = currentItem;
        this.mediaType = mediaType;
        this.hasPrevious = hasPrevious;
        this.hasNext = hasNext;
    }
}
