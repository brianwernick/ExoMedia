package com.devbrackets.android.exomedia.core.audio;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.devbrackets.android.exomedia.core.EMListenerMux;
import com.devbrackets.android.exomedia.core.builder.RenderBuilder;
import com.devbrackets.android.exomedia.core.type.MediaPlayerApi;
import com.devbrackets.android.exomedia.util.MediaType;

public class NativeMediaPlayer extends MediaPlayer implements MediaPlayerApi, MediaPlayer.OnBufferingUpdateListener {
    private static final String TAG = "NativeMediaPlayer";

    protected int currentBufferPercent = 0;
    protected EMListenerMux listenerMux;

    public NativeMediaPlayer(Context context) {
        super();
        setOnBufferingUpdateListener(this);
    }

    @Override
    public void setDataSource(Context context, Uri uri, MediaType defaultMediaType) {
        setDataSource(context, uri, (RenderBuilder)null);
    }

    @Override
    public void setDataSource(Context context, Uri uri, RenderBuilder renderBuilder) {
        try {
            setDataSource(context, uri);
        } catch (Exception e) {
            Log.d(TAG, "MediaPlayer: error setting data source", e);
        }
    }

    @Override
    public void prepare() {
        try {
            super.prepare();
        } catch (Exception e) {
            //Purposefully left blank
        }
    }

    @Override
    public void start() {
        super.start();
        listenerMux.setNotifiedCompleted(false);
    }

    @Override
    public void stopPlayback() {
        stop();
    }

    @Override
    public int getDuration() {
        if (!listenerMux.isPrepared()) {
            return 0;
        }

        return super.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        if (!listenerMux.isPrepared()) {
            return 0;
        }

        return super.getCurrentPosition();
    }

    @Override
    public int getBufferedPercent() {
        return currentBufferPercent;
    }

    @Override
    public void setListenerMux(EMListenerMux listenerMux) {
        this.listenerMux = listenerMux;

        setOnCompletionListener(listenerMux);
        setOnPreparedListener(listenerMux);
        setOnErrorListener(listenerMux);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            setOnInfoListener(listenerMux);
        }
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        currentBufferPercent = percent;
    }
}
