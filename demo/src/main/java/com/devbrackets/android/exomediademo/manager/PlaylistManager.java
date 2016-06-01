package com.devbrackets.android.exomediademo.manager;

import android.app.Application;
import android.app.Service;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.devbrackets.android.exomedia.listener.VideoControlsButtonListener;
import com.devbrackets.android.exomedia.ui.widget.EMVideoView;
import com.devbrackets.android.exomedia.ui.widget.VideoControls;
import com.devbrackets.android.exomediademo.App;
import com.devbrackets.android.exomediademo.data.MediaItem;
import com.devbrackets.android.exomediademo.playlist.VideoApi;
import com.devbrackets.android.exomediademo.service.MediaService;
import com.devbrackets.android.playlistcore.api.VideoPlayerApi;
import com.devbrackets.android.playlistcore.manager.BasePlaylistManager;
import com.devbrackets.android.playlistcore.manager.ListPlaylistManager;

/**
 * A PlaylistManager that extends the {@link BasePlaylistManager} for use with the
 * {@link MediaService} which extends {@link com.devbrackets.android.playlistcore.service.BasePlaylistService}.
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
        return MediaService.class;
    }

    @Override
    public void setVideoPlayer(@Nullable VideoPlayerApi videoPlayer) {
        super.setVideoPlayer(videoPlayer);
        if (videoPlayer != null) {
            updateVideoControls(videoPlayer);
        }
    }

    /**
     * Updates the available controls on the VideoView and links the
     * button events to the playlist service and this.
     *
     * @param videoPlayer The videoPlayerApi to link
     */
    private void updateVideoControls(@NonNull VideoPlayerApi videoPlayer) {
        VideoApi api = (VideoApi)videoPlayer;
        EMVideoView videoView = api.getVideoView();
        if (videoView == null) {
            return;
        }

        VideoControls videoControls = videoView.getVideoControls();
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