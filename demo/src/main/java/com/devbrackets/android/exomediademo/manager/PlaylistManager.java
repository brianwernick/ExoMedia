package com.devbrackets.android.exomediademo.manager;

import android.app.Application;
import android.app.Service;

import com.devbrackets.android.exomedia.manager.EMPlaylistManager;
import com.devbrackets.android.exomediademo.App;
import com.devbrackets.android.exomediademo.helper.AudioItems;
import com.devbrackets.android.exomediademo.service.AudioService;

public class PlaylistManager extends EMPlaylistManager<PlaylistManager.MediaItem> {

    @Override
    protected Application getApplication() {
        return App.getApplication();
    }

    @Override
    protected Class<? extends Service> getMediaServiceClass() {
        return AudioService.class;
    }




    public static class MediaItem implements EMPlaylistManager.PlaylistItem {
        private AudioItems.AudioItem audioItem;

        public MediaItem(AudioItems.AudioItem audioItem) {
            this.audioItem = audioItem;
        }

        @Override
        public long getId() {
            return 0;
        }

        @Override
        public long getPlaylistId() {
            return 0;
        }

        @Override
        public boolean isAudio() {
            return true;
        }

        @Override
        public boolean isVideo() {
            return false;
        }

        @Override
        public String getMediaUrl() {
            return audioItem.getMediaUrl();
        }

        @Override
        public String getTitle() {
            return audioItem.getTitle();
        }

        @Override
        public String getThumbnailUrl() {
            return null;
        }

        @Override
        public String getArtworkUrl() {
            return audioItem.getArtworkUrl();
        }
    }
}
