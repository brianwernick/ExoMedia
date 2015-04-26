package com.devbrackets.android.exomedia.event;

import com.devbrackets.android.exomedia.manager.EMPlaylistManager;

public class EMMediaAllowedTypeChangedEvent {
    public final EMPlaylistManager.MediaType allowedType;

    public EMMediaAllowedTypeChangedEvent(EMPlaylistManager.MediaType allowedType) {
        this.allowedType = allowedType;
    }
}
