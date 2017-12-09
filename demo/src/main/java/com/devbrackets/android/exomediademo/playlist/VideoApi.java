package com.devbrackets.android.exomediademo.playlist;

import android.net.Uri;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;

import com.devbrackets.android.exomedia.ui.widget.VideoControls;
import com.devbrackets.android.exomedia.ui.widget.VideoView;
import com.devbrackets.android.exomediademo.data.MediaItem;
import com.devbrackets.android.playlistcore.data.PlaybackState;
import com.devbrackets.android.playlistcore.listener.PlaylistListener;
import com.devbrackets.android.playlistcore.manager.BasePlaylistManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VideoApi extends BaseMediaApi implements PlaylistListener<MediaItem> {
    public VideoView videoView;

    public VideoApi(VideoView videoView) {
        this.videoView = videoView;

        videoView.setOnErrorListener(this);
        videoView.setOnPreparedListener(this);
        videoView.setOnCompletionListener(this);
        videoView.setOnSeekCompletionListener(this);
        videoView.setOnBufferUpdateListener(this);
    }

    @Override
    public boolean isPlaying() {
        return videoView.isPlaying();
    }

    @Override
    public void play() {
        videoView.start();
    }

    @Override
    public void pause() {
        videoView.pause();
    }

    @Override
    public void stop() {
        videoView.stopPlayback();
    }

    @Override
    public void reset() {
        // Purposefully left blank
    }

    @Override
    public void release() {
        videoView.suspend();
    }

    @Override
    public void setVolume(@FloatRange(from = 0.0, to = 1.0) float left, @FloatRange(from = 0.0, to = 1.0) float right) {
        videoView.setVolume((left + right) / 2);
    }

    @Override
    public void seekTo(@IntRange(from = 0L) long milliseconds) {
        videoView.seekTo((int) milliseconds);
    }


    @Override
    public boolean getHandlesOwnAudioFocus() {
        return false;
    }

    @Override
    public boolean handlesItem(@NotNull MediaItem item) {
        return item.getMediaType() == BasePlaylistManager.VIDEO;
    }

    @Override
    public void playItem(@NotNull MediaItem item) {
        prepared = false;
        bufferPercent = 0;
        videoView.setVideoURI(Uri.parse(item.getDownloaded() ? item.getDownloadedMediaUri() : item.getMediaUrl()));
    }

    @Override
    public long getCurrentPosition() {
        return prepared ? videoView.getCurrentPosition() : 0;
    }

    @Override
    public long getDuration() {
        return prepared ? videoView.getDuration() : 0;
    }

    @Override
    public int getBufferedPercent() {
        return bufferPercent;
    }

    /*
     * PlaylistListener methods used for keeping the VideoControls provided
     * by the ExoMedia VideoView up-to-date with the current playback state
     */
    @Override
    public boolean onPlaylistItemChanged(@Nullable MediaItem currentItem, boolean hasNext, boolean hasPrevious) {
        VideoControls videoControls = videoView.getVideoControls();
        if (videoControls != null && currentItem != null) {
            // Updates the VideoControls display text
            videoControls.setTitle(currentItem.getTitle());
            videoControls.setSubTitle(currentItem.getAlbum());
            videoControls.setDescription(currentItem.getArtist());

            // Updates the VideoControls button visibilities
            videoControls.setPreviousButtonEnabled(hasPrevious);
            videoControls.setNextButtonEnabled(hasNext);
        }

        return false;
    }

    @Override
    public boolean onPlaybackStateChanged(@NotNull PlaybackState playbackState) {
        return false;
    }
}
