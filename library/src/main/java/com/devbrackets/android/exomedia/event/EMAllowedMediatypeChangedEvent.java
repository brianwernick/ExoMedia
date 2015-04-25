package com.devbrackets.android.exomedia.event;

import com.devbrackets.android.exomedia.manager.EMPlaylistManager;

public class EMAllowedMediaTypeChangedEvent {
    public final EMPlaylistManager.MediaType allowedType;

    public EMAllowedMediaTypeChangedEvent(EMPlaylistManager.MediaType allowedType) {
        this.allowedType = allowedType;
    }
}
