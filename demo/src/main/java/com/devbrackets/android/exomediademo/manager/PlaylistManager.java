package com.devbrackets.android.exomediademo.manager;

import android.app.Application;
import android.support.annotation.NonNull;

import com.devbrackets.android.exomedia.listener.VideoControlsButtonListener;
import com.devbrackets.android.exomedia.ui.widget.VideoControls;
import com.devbrackets.android.exomediademo.data.MediaItem;
import com.devbrackets.android.exomediademo.playlist.VideoApi;
import com.devbrackets.android.exomediademo.service.MediaService;
import com.devbrackets.android.playlistcore.manager.ListPlaylistManager;

/**
 * A PlaylistManager that extends the {@link ListPlaylistManager} for use with the
 * {@link MediaService} which extends {@link com.devbrackets.android.playlistcore.service.BasePlaylistService}.
 */
public class PlaylistManager extends ListPlaylistManager<MediaItem> {

    public PlaylistManager(Application application) {
        super(application, MediaService.class);
    }

    /**
     * Note: You can call {@link #getMediaPlayers()} and add it manually in the activity,
     * however we have this helper method to allow registration of the media controls
     * listener provided by ExoMedia's {@link com.devbrackets.android.exomedia.ui.widget.VideoControls}
     */
    public void addVideoApi(@NonNull VideoApi videoApi) {
        getMediaPlayers().add(videoApi);
        updateVideoControls(videoApi);
        registerPlaylistListener(videoApi);
    }

    /**
     * Note: You can call {@link #getMediaPlayers()} and remove it manually in the activity,
     * however we have this helper method to remove the registered listener from {@link #addVideoApi(VideoApi)}
     */
    public void removeVideoApi(@NonNull VideoApi videoApi) {
        VideoControls controls = videoApi.videoView.getVideoControls();
        if (controls != null) {
            controls.setButtonListener(null);
        }

        unRegisterPlaylistListener(videoApi);
        getMediaPlayers().remove(videoApi);
    }

    /**
     * Updates the available controls on the VideoView and links the
     * button events to the playlist service and this.
     *
     * @param videoApi The VideoApi to link
     */
    private void updateVideoControls(@NonNull VideoApi videoApi) {
        VideoControls videoControls = videoApi.videoView.getVideoControls();
        if (videoControls != null) {
            videoControls.setPreviousButtonRemoved(false);
            videoControls.setNextButtonRemoved(false);
            videoControls.setButtonListener(new ControlsListener());
        }
    }

    /**
     * An implementation of the {@link VideoControlsButtonListener} that provides
     * integration with the playlist service.
     */
    private class ControlsListener implements VideoControlsButtonListener {
        @Override
        public boolean onPlayPauseClicked() {
            invokePausePlay();
            return true;
        }

        @Override
        public boolean onPreviousClicked() {
            invokePrevious();
            return false;
        }

        @Override
        public boolean onNextClicked() {
            invokeNext();
            return false;
        }

        @Override
        public boolean onRewindClicked() {
            return false;
        }

        @Override
        public boolean onFastForwardClicked() {
            return false;
        }
    }
}