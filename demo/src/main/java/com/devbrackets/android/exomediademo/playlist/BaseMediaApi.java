package com.devbrackets.android.exomediademo.playlist;

import com.devbrackets.android.exomedia.listener.OnBufferUpdateListener;
import com.devbrackets.android.exomedia.listener.OnCompletionListener;
import com.devbrackets.android.exomedia.listener.OnErrorListener;
import com.devbrackets.android.exomedia.listener.OnPreparedListener;
import com.devbrackets.android.exomedia.listener.OnSeekCompletionListener;
import com.devbrackets.android.playlistcore.api.MediaPlayerApi;
import com.devbrackets.android.playlistcore.listener.OnMediaBufferUpdateListener;
import com.devbrackets.android.playlistcore.listener.OnMediaCompletionListener;
import com.devbrackets.android.playlistcore.listener.OnMediaErrorListener;
import com.devbrackets.android.playlistcore.listener.OnMediaPreparedListener;
import com.devbrackets.android.playlistcore.listener.OnMediaSeekCompletionListener;

public abstract class BaseMediaApi implements MediaPlayerApi, OnPreparedListener, OnCompletionListener, OnErrorListener,
        OnSeekCompletionListener, OnBufferUpdateListener {

    //The listeners that can be registered
    protected OnMediaPreparedListener preparedListener;
    protected OnMediaCompletionListener completionListener;
    protected OnMediaSeekCompletionListener seekCompletionListener;
    protected OnMediaErrorListener errorListener;
    protected OnMediaBufferUpdateListener bufferUpdateListener;

    protected boolean prepared;
    protected int bufferPercent;

    //The listener registrations
    @Override
    public void setOnMediaPreparedListener(OnMediaPreparedListener listener) {
        preparedListener = listener;
    }

    @Override
    public void setOnMediaBufferUpdateListener(OnMediaBufferUpdateListener listener) {
        bufferUpdateListener = listener;
    }

    @Override
    public void setOnMediaSeekCompletionListener(OnMediaSeekCompletionListener listener) {
        seekCompletionListener = listener;
    }

    @Override
    public void setOnMediaCompletionListener(OnMediaCompletionListener listener) {
        completionListener = listener;
    }

    @Override
    public void setOnMediaErrorListener(OnMediaErrorListener listener) {
        errorListener = listener;
    }

    //The listeners from the MediaPlayer (and VideoView)
    @Override
    public void onCompletion() {
        if (completionListener != null) {
            completionListener.onCompletion(this);
        }
    }

    @Override
    public void onPrepared() {
        prepared = true;

        if (preparedListener != null) {
            preparedListener.onPrepared(this);
        }
    }

    @Override
    public boolean onError() {
        return errorListener != null && errorListener.onError(this);
    }

    @Override
    public void onSeekComplete() {
        if (seekCompletionListener != null) {
            seekCompletionListener.onSeekComplete(this);
        }
    }

    @Override
    public void onBufferingUpdate(int percent) {
        bufferPercent = percent;

        if (bufferUpdateListener != null) {
            bufferUpdateListener.onBufferingUpdate(this, percent);
        }
    }
}
