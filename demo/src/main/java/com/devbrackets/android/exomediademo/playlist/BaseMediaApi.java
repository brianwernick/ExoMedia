package com.devbrackets.android.exomediademo.playlist;

import com.devbrackets.android.exomedia.listener.OnBufferUpdateListener;
import com.devbrackets.android.exomedia.listener.OnCompletionListener;
import com.devbrackets.android.exomedia.listener.OnErrorListener;
import com.devbrackets.android.exomedia.listener.OnPreparedListener;
import com.devbrackets.android.exomedia.listener.OnSeekCompletionListener;
import com.devbrackets.android.exomediademo.data.MediaItem;
import com.devbrackets.android.playlistcore.api.MediaPlayerApi;
import com.devbrackets.android.playlistcore.listener.MediaStatusListener;

import org.jetbrains.annotations.NotNull;

public abstract class BaseMediaApi implements MediaPlayerApi<MediaItem>,
        OnPreparedListener,
        OnCompletionListener,
        OnErrorListener,
        OnSeekCompletionListener,
        OnBufferUpdateListener {

    protected boolean prepared;
    protected int bufferPercent;

    protected MediaStatusListener<MediaItem> mediaStatusListener;

    @Override
    public void setMediaStatusListener(@NotNull MediaStatusListener<MediaItem> listener) {
        mediaStatusListener = listener;
    }

    @Override
    public void onCompletion() {
        if (mediaStatusListener != null) {
            mediaStatusListener.onCompletion(this);
        }
    }

    @Override
    public boolean onError(Exception e) {
        return mediaStatusListener != null && mediaStatusListener.onError(this);
    }

    @Override
    public void onPrepared() {
        prepared = true;

        if (mediaStatusListener != null) {
            mediaStatusListener.onPrepared(this);
        }
    }

    @Override
    public void onSeekComplete() {
        if (mediaStatusListener != null) {
            mediaStatusListener.onSeekComplete(this);
        }
    }

    @Override
    public void onBufferingUpdate(int percent) {
        bufferPercent = percent;

        if (mediaStatusListener != null) {
            mediaStatusListener.onBufferingUpdate(this, percent);
        }
    }
}
