package com.devbrackets.android.exomediademo.manager;

import android.app.Application;
import android.app.Service;

import com.devbrackets.android.exomediademo.App;
import com.devbrackets.android.exomediademo.data.MediaItem;
import com.devbrackets.android.exomediademo.service.AudioService;
import com.devbrackets.android.playlistcore.manager.PlaylistManagerBase;

/**
 * A PlaylistManager that extends the {@link PlaylistManagerBase} for use with the
 * {@link AudioService} which extends {@link com.devbrackets.android.playlistcore.service.PlaylistBase}.
 */
public class PlaylistManager extends PlaylistManagerBase<MediaItem> {

    @Override
    protected Application getApplication() {
        return App.getApplication();
    }

    @Override
    protected Class<? extends Service> getMediaServiceClass() {
        return AudioService.class;
    }
}
