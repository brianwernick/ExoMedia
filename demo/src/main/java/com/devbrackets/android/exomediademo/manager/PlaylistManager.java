package com.devbrackets.android.exomediademo.manager;

import android.app.Application;
import android.app.Service;
import android.support.annotation.NonNull;

import com.devbrackets.android.exomediademo.App;
import com.devbrackets.android.exomediademo.data.MediaItem;
import com.devbrackets.android.exomediademo.service.AudioService;
import com.devbrackets.android.playlistcore.manager.BasePlaylistManager;
import com.devbrackets.android.playlistcore.manager.ListPlaylistManager;

/**
 * A PlaylistManager that extends the {@link BasePlaylistManager} for use with the
 * {@link AudioService} which extends {@link com.devbrackets.android.playlistcore.service.BasePlaylistService}.
 */
public class PlaylistManager extends ListPlaylistManager<MediaItem> {

    @NonNull
    @Override
    protected Application getApplication() {
        return App.getApplication();
    }

    @NonNull
    @Override
    protected Class<? extends Service> getMediaServiceClass() {
        return AudioService.class;
    }
}
