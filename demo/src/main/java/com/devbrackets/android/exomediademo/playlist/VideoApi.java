package com.devbrackets.android.exomediademo.playlist;

import android.net.Uri;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;

import com.devbrackets.android.exomedia.ui.widget.EMVideoView;
import com.devbrackets.android.playlistcore.api.VideoPlayerApi;

public class VideoApi extends BaseMediaApi implements VideoPlayerApi {
    private EMVideoView videoView;

    public VideoApi(EMVideoView videoView) {
        this.videoView = videoView;

        videoView.setOnErrorListener(this);
        videoView.setOnPreparedListener(this);
        videoView.setOnCompletionListener(this);
        videoView.setOnBufferUpdateListener(this);
        videoView.setOnSeekCompletionListener(this);
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
        videoView.reset();
    }

    @Override
    public void release() {
        videoView.release();
    }

    @Override
    public void setVolume(@FloatRange(from = 0.0, to = 1.0) float left, @FloatRange(from = 0.0, to = 1.0) float right) {
        videoView.setVolume(left + right / 2);
    }

    @Override
    public void seekTo(@IntRange(from = 0L) long milliseconds) {
        videoView.seekTo((int)milliseconds);
    }

    @Override
    public void setDataSource(@NonNull Uri uri) {
        videoView.setVideoURI(uri);
    }

    @Override
    public long getCurrentPosition() {
        return videoView.getCurrentPosition();
    }

    @Override
    public long getDuration() {
        return videoView.getDuration();
    }

    @Override
    public int getBufferedPercent() {
        return videoView.getBufferPercentage();
    }

    public EMVideoView getVideoView() {
        return videoView;
    }
}
