package com.devbrackets.android.exomediademo.data;

import com.devbrackets.android.exomediademo.manager.PlaylistManager;
import com.devbrackets.android.playlistcore.annotation.SupportedMediaType;
import com.devbrackets.android.playlistcore.api.PlaylistItem;

/**
 * A custom {@link PlaylistItem}
 * to hold the information pertaining to the audio and video items
 */
public class MediaItem implements PlaylistItem {
    private Samples.Sample sample;
    boolean isAudio;

    public MediaItem(Samples.Sample sample, boolean isAudio) {
        this.sample = sample;
        this.isAudio = isAudio;
    }

    @Override
    public long getId() {
        return 0;
    }

    @Override
    public boolean getDownloaded() {
        return false;
    }

    @Override
    @SupportedMediaType
    public int getMediaType() {
        return isAudio ? PlaylistManager.AUDIO : PlaylistManager.VIDEO;
    }

    @Override
    public String getMediaUrl() {
        return sample.getMediaUrl();
    }

    @Override
    public String getDownloadedMediaUri() {
        return null;
    }

    @Override
    public String getThumbnailUrl() {
        return sample.getArtworkUrl();
    }

    @Override
    public String getArtworkUrl() {
        return sample.getArtworkUrl();
    }

    @Override
    public String getTitle() {
        return sample.getTitle();
    }

    @Override
    public String getAlbum() {
        return "PlaylistCore Demo";
    }

    @Override
    public String getArtist() {
        return "Unknown Artist";
    }
}