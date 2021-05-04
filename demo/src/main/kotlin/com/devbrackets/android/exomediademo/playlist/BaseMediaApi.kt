package com.devbrackets.android.exomediademo.playlist

import com.devbrackets.android.exomedia.listener.*
import com.devbrackets.android.exomediademo.data.MediaItem
import com.devbrackets.android.playlistcore.api.MediaPlayerApi
import com.devbrackets.android.playlistcore.listener.MediaStatusListener

abstract class BaseMediaApi : MediaPlayerApi<MediaItem>, OnPreparedListener, OnCompletionListener, OnErrorListener, OnSeekCompletionListener, OnBufferUpdateListener {
    protected var prepared: Boolean = false
    protected var bufferPercent: Int = 0

    protected var statusListener: MediaStatusListener<MediaItem>? = null

    override fun setMediaStatusListener(listener: MediaStatusListener<MediaItem>) {
        statusListener = listener
    }

    override fun onCompletion() {
        statusListener?.onCompletion(this)
    }

    override fun onError(e: Exception?): Boolean {
        return statusListener?.onError(this) == true
    }

    override fun onPrepared() {
        prepared = true
        statusListener?.onPrepared(this)
    }

    override fun onSeekComplete() {
        statusListener?.onSeekComplete(this)
    }

    override fun onBufferingUpdate(percent: Int) {
        bufferPercent = percent
        statusListener?.onBufferingUpdate(this, percent)
    }
}
