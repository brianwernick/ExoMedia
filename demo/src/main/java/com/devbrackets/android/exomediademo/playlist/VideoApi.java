package com.devbrackets.android.exomediademo.playlist;

import android.media.MediaPlayer;
import android.net.Uri;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;

import com.devbrackets.android.exomedia.EMVideoView;
import com.devbrackets.android.playlistcore.api.VideoPlayerApi;

public class VideoApi implements VideoPlayerApi {
    private EMVideoView videoView;

    public VideoApi(EMVideoView videoView) {
        this.videoView = videoView;
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

    @Override
    public void setOnPreparedListener(MediaPlayer.OnPreparedListener onPreparedListener) {
        videoView.setOnPreparedListener(onPreparedListener);
    }

    @Override
    public void setOnBufferingUpdateListener(MediaPlayer.OnBufferingUpdateListener onBufferingUpdateListener) {
        //Purposefully left blank
    }

    @Override
    public void setOnSeekCompleteListener(MediaPlayer.OnSeekCompleteListener onSeekCompleteListener) {
        //Purposefully left blank
    }

    @Override
    public void setOnInfoListener(MediaPlayer.OnInfoListener onInfoListener) {
        videoView.setOnInfoListener(onInfoListener);
    }

    @Override
    public void setOnCompletionListener(MediaPlayer.OnCompletionListener onCompletionListener) {
        videoView.setOnCompletionListener(onCompletionListener);
    }

    @Override
    public void setOnErrorListener(MediaPlayer.OnErrorListener onErrorListener) {
        videoView.setOnErrorListener(onErrorListener);
    }
}
