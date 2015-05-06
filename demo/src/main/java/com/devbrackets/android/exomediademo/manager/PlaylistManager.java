package com.devbrackets.android.exomediademo.manager;

import android.app.Application;
import android.app.Service;

import com.devbrackets.android.exomedia.manager.EMPlaylistManager;
import com.devbrackets.android.exomedia.service.EMPlaylistService;
import com.devbrackets.android.exomediademo.App;
import com.devbrackets.android.exomediademo.data.MediaItem;
import com.devbrackets.android.exomediademo.service.AudioService;

/**
 * A PlaylistManager that extends the {@link EMPlaylistManager} for use with the
 * {@link AudioService} which extends {@link EMPlaylistService}.
 */
public class PlaylistManager extends EMPlaylistManager<MediaItem> {

    @Override
    protected Application getApplication() {
        return App.getApplication();
    }

    @Override
    protected Class<? extends Service> getMediaServiceClass() {
        return AudioService.class;
    }
}
