package com.devbrackets.android.exomedia.event;

import com.devbrackets.android.exomedia.service.EMPlaylistService;

public class EMMediaStateEvent {
    public final EMPlaylistService.MediaState mediaState;

    public EMMediaStateEvent(EMPlaylistService.MediaState mediaState) {
        this.mediaState = mediaState;
    }
}
