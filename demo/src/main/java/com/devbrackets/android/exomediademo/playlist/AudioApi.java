package com.devbrackets.android.exomediademo.playlist;

import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.os.PowerManager;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;

import com.devbrackets.android.exomedia.AudioPlayer;
import com.devbrackets.android.exomediademo.data.MediaItem;
import com.devbrackets.android.playlistcore.manager.BasePlaylistManager;

import org.jetbrains.annotations.NotNull;

public class AudioApi extends BaseMediaApi {
    @NonNull
    private AudioPlayer audioPlayer;

    public AudioApi(@NonNull Context context) {
        this.audioPlayer = new AudioPlayer(context.getApplicationContext());

        audioPlayer.setOnErrorListener(this);
        audioPlayer.setOnPreparedListener(this);
        audioPlayer.setOnCompletionListener(this);
        audioPlayer.setOnSeekCompletionListener(this);
        audioPlayer.setOnBufferUpdateListener(this);

        audioPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
        audioPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
    }

    @Override
    public boolean isPlaying() {
        return audioPlayer.isPlaying();
    }

    @Override
    public void play() {
        audioPlayer.start();
    }

    @Override
    public void pause() {
        audioPlayer.pause();
    }

    @Override
    public void stop() {
        audioPlayer.stopPlayback();
    }

    @Override
    public void reset() {
        audioPlayer.reset();
    }

    @Override
    public void release() {
        audioPlayer.release();
    }

    @Override
    public void setVolume(@FloatRange(from = 0.0, to = 1.0) float left, @FloatRange(from = 0.0, to = 1.0) float right) {
        audioPlayer.setVolume(left, right);
    }

    @Override
    public void seekTo(@IntRange(from = 0L) long milliseconds) {
        audioPlayer.seekTo((int)milliseconds);
    }

    @Override
    public boolean getHandlesOwnAudioFocus() {
        return false;
    }

    @Override
    public boolean handlesItem(@NotNull MediaItem item) {
        return item.getMediaType() == BasePlaylistManager.AUDIO;
    }

    @Override
    public void playItem(@NotNull MediaItem item) {
        try {
            prepared = false;
            bufferPercent = 0;
            audioPlayer.setDataSource(Uri.parse(item.getDownloaded() ? item.getDownloadedMediaUri() : item.getMediaUrl()));
            audioPlayer.prepareAsync();
        } catch (Exception e) {
            //Purposefully left blank
        }
    }

    @Override
    public long getCurrentPosition() {
        return prepared ? audioPlayer.getCurrentPosition() : 0;
    }

    @Override
    public long getDuration() {
        return prepared ? audioPlayer.getDuration() : 0;
    }

    @Override
    public int getBufferedPercent() {
        return bufferPercent;
    }
}
