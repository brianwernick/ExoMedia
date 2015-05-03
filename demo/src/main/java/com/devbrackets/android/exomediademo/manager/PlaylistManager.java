package com.devbrackets.android.exomediademo.manager;

import android.app.Application;
import android.app.Service;
import android.support.annotation.Nullable;

import com.devbrackets.android.exomedia.event.EMMediaProgressEvent;
import com.devbrackets.android.exomedia.event.EMPlaylistItemChangedEvent;
import com.devbrackets.android.exomedia.listener.EMPlaylistServiceCallback;
import com.devbrackets.android.exomedia.manager.EMPlaylistManager;
import com.devbrackets.android.exomedia.service.EMPlaylistService;
import com.devbrackets.android.exomediademo.App;
import com.devbrackets.android.exomediademo.data.MediaItem;
import com.devbrackets.android.exomediademo.service.AudioService;

import java.util.ArrayList;
import java.util.List;

/**
 * A PlaylistManager that extends the {@link EMPlaylistManager} for use with the
 * {@link AudioService} which extends {@link EMPlaylistService}.
 */
public class PlaylistManager extends EMPlaylistManager<MediaItem> implements EMPlaylistServiceCallback {
    private AudioService service;
    private List<EMPlaylistServiceCallback> callbackList = new ArrayList<>();

    @Override
    protected Application getApplication() {
        return App.getApplication();
    }

    @Override
    protected Class<? extends Service> getMediaServiceClass() {
        return AudioService.class;
    }

    @Override
    public boolean onPlaylistItemChanged(PlaylistItem currentItem, MediaType mediaType, boolean hasNext, boolean hasPrevious) {
        for (EMPlaylistServiceCallback callback : callbackList) {
            if (callback.onPlaylistItemChanged(currentItem, mediaType, hasNext, hasPrevious)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean onMediaStateChanged(EMPlaylistService.MediaState mediaState) {
        for (EMPlaylistServiceCallback callback : callbackList) {
            if (callback.onMediaStateChanged(mediaState)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean onProgressUpdated(EMMediaProgressEvent event) {
        for (EMPlaylistServiceCallback callback : callbackList) {
            if (callback.onProgressUpdated(event)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Retrieves the most recent media playback state.
     *
     * @return The most recent MediaState
     */
    public EMPlaylistService.MediaState getCurrentMediaState() {
        if (service != null) {
            return service.getCurrentMediaState();
        }

        return EMPlaylistService.MediaState.STOPPED;
    }

    /**
     * Retrieves the current progress for the media playback
     *
     * @return The most recent progress event
     */
    @Nullable
    public EMMediaProgressEvent getCurrentProgress() {
        return service != null ? service.getCurrentMediaProgress() : null;
    }

    /**
     * Retrieves the most recent {@link EMPlaylistItemChangedEvent}
     *
     * @return The most recent Item Changed information
     */
    @Nullable
    public EMPlaylistItemChangedEvent getCurrentItemChangedEvent() {
        return service != null ? service.getCurrentItemChangedEvent() : null;
    }

    /**
     * Links the {@link AudioService} so that we can correctly manage the
     * {@link EMPlaylistServiceCallback} for the {@link com.devbrackets.android.exomediademo.AudioPlayerActivity}
     *
     * @param service The AudioService to link to this manager
     */
    public void registerService(AudioService service) {
        this.service = service;
        service.registerCallback(this);
    }

    /**
     * UnLinks the {@link AudioService} from this manager. (see {@link #registerService(AudioService)}
     */
    public void unRegisterService() {
        service.unRegisterCallback(this);
        service = null;
    }

    /**
     * Registers the callback to this service.  These callbacks will only be
     * called if {@link #registerService(AudioService)} has been called.
     *
     * @param callback The callback to register
     */
    public void registerServiceCallbacks(EMPlaylistServiceCallback callback) {
        if (callback != null) {
            callbackList.add(callback);
        }
    }

    /**
     * UnRegisters the specified callback.  This should be called when the callback
     * class losses focus, or should be destroyed.
     *
     * @param callback The callback to remove
     */
    public void unRegisterServiceCallbacks(EMPlaylistServiceCallback callback) {
        if (callback != null) {
            callbackList.remove(callback);
        }
    }
}
