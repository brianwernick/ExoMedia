package com.devbrackets.android.exomediademo.data;

import com.devbrackets.android.exomedia.manager.EMPlaylistManager;
import com.devbrackets.android.exomediademo.helper.AudioItems;

/**
 * A custom {@link com.devbrackets.android.exomedia.manager.EMPlaylistManager.PlaylistItem}
 * to hold the information pertaining to the audio items ("The Count of Monte Cristo" chapters)
 */
public class MediaItem implements EMPlaylistManager.PlaylistItem {
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
    public EMPlaylistManager.MediaType getMediaType() {
        return EMPlaylistManager.MediaType.AUDIO;
    }

    @Override
    public String getMediaUrl() {
        return audioItem.getMediaUrl();
    }

    @Override
    public String getDownloadedMediaUri() {
        return null;
    }

    @Override
    public String getThumbnailUrl() {
        return audioItem.getArtworkUrl();
    }

    @Override
    public String getArtworkUrl() {
        return audioItem.getArtworkUrl();
    }

    @Override
    public String getTitle() {
        return audioItem.getTitle();
    }

    @Override
    public String getAlbum() {
        return "ExoMedia Demo";
    }

    @Override
    public String getArtist() {
        return "Unknown Artist";
    }
}