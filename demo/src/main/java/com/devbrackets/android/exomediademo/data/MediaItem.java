package com.devbrackets.android.exomediademo.data;

import com.devbrackets.android.exomediademo.manager.PlaylistManager;
import com.devbrackets.android.playlistcore.manager.IPlaylistItem;

/**
 * A custom {@link IPlaylistItem}
 * to hold the information pertaining to the audio and video items
 */
public class MediaItem implements IPlaylistItem {

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
    public long getPlaylistId() {
        return 0;
    }

    @Override
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
        return "ExoMedia Demo";
    }

    @Override
    public String getArtist() {
        return "Unknown Artist";
    }
}